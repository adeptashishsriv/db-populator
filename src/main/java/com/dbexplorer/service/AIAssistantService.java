package com.dbexplorer.service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import com.dbexplorer.model.AIConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Service to interact with AI APIs for SQL generation from natural language descriptions.
 * Supports OpenAI (GPT-4, GPT-3.5-turbo), Claude, and other configured models.
 */
public class AIAssistantService {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    
    private final HttpClient httpClient;
    private final AIConfigManager configManager;

    public AIAssistantService(AIConfigManager configManager) {
        this.httpClient = HttpClient.newHttpClient();
        this.configManager = configManager;
    }

    /**
     * Generates SQL from natural language description using the AI model.
     */
    public String generateSQL(String naturalLanguageQuery, String schemaInfo, String databaseType) {
        AIConfig config = configManager.getLastUsedConfig();
        return generateSQLWithSpecificConfig(config, naturalLanguageQuery, schemaInfo, databaseType);
    }

    /**
     * Generates SQL using a specific AI profile.
     */
    public String generateSQLWithSpecificConfig(AIConfig config, String naturalLanguageQuery, String schemaInfo, String databaseType) {
        if (config == null || !config.isEnabled()) {
            return "Error: AI Assistant is not configured. Please configure it in Edit → AI Configuration.";
        }
        
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: API Key not configured. Please set your API key in Edit → AI Configuration.";
        }

        try {
            String systemPrompt = buildSystemPrompt(schemaInfo, databaseType);
            String response;
            
            if ("Claude".equals(config.getApiProvider())) {
                response = callClaudeAPI(config, systemPrompt, naturalLanguageQuery);
                return extractContentFromClaude(response);
            } else {
                response = callOpenAIAPI(config, systemPrompt, naturalLanguageQuery);
                return extractSQLFromResponse(response);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Request was interrupted: " + e.getMessage();
        } catch (IOException e) {
            return "Error: Failed to connect to AI service: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String buildSystemPrompt(String schemaInfo, String databaseType) {
        return """
            You are an expert SQL query generator. Your task is to convert natural language descriptions into valid SQL queries.
            
            DATABASE TYPE: %s
            
            DATABASE SCHEMA:
            %s
            
            INSTRUCTIONS:
            1. Generate only the SQL query, no explanations.
            2. Use appropriate SQL syntax for %s.
            3. Include proper table and column names from the schema.
            4. Use proper JOIN clauses when multiple tables are involved.
            5. Always include necessary WHERE, ORDER BY, GROUP BY clauses if applicable.
            6. Return ONLY the SQL query without markdown backticks or additional text.
            7. If the request cannot be fulfilled with the given schema, explain why clearly.""".formatted(databaseType, schemaInfo, databaseType);
    }

    /**
     * Calls OpenAI or compatible API.
     */
    private String callOpenAIAPI(AIConfig config, String systemPrompt, String userQuery) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        String model = config.getModel() != null ? config.getModel() : "gpt-3.5-turbo";
        requestBody.addProperty("model", model);
        
        // Parameter handling based on model type
        boolean isReasoningModel = model.startsWith("o1") || model.equals("gpt-5-nano");
        
        if (isReasoningModel) {
            requestBody.addProperty("max_completion_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : 1000);
            // Temperature is usually restricted to 1.0 or omitted for reasoning models
        } else {
            requestBody.addProperty("temperature", config.getTemperature() > 0 ? config.getTemperature() : 0.7);
            requestBody.addProperty("max_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : 1000);
        }

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userQuery);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        String url = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty() 
                ? config.getBaseUrl() : OPENAI_API_URL;
                
        if (url.contains("api.openai.com") && !url.endsWith("/chat/completions")) {
            url = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
        }

        return executeRequest(url, requestBody.toString(), Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer " + config.getApiKey()
        ));
    }

    /**
     * Calls Claude (Anthropic) API.
     */
    private String callClaudeAPI(AIConfig config, String systemPrompt, String userQuery) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", config.getModel() != null ? config.getModel() : "claude-3-sonnet-20240229");
        requestBody.addProperty("max_tokens", config.getMaxTokens() > 0 ? config.getMaxTokens() : 1000);
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.addProperty("system", systemPrompt);
        }

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userQuery);
        messages.add(userMsg);
        requestBody.add("messages", messages);

        String url = config.getBaseUrl() != null && !config.getBaseUrl().isEmpty() 
                ? config.getBaseUrl() : CLAUDE_API_URL;

        return executeRequest(url, requestBody.toString(), Map.of(
            "content-type", "application/json",
            "x-api-key", config.getApiKey(),
            "anthropic-version", "2023-06-01"
        ));
    }

    private String executeRequest(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(java.time.Duration.ofSeconds(30));

        headers.forEach(builder::header);
        
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("API error (code " + response.statusCode() + "): " + response.body());
        }
    }

    /**
     * Robust test method that selects the correct "curl-like" execution path based on the provider.
     */
    public String testConnection(AIConfig config) throws Exception {
        String testQuery = "Hello, world";
        
        String response;
        String result;

        if ("Claude".equals(config.getApiProvider())) {
            response = callClaudeAPI(config, null, testQuery);
            result = extractContentFromClaude(response);
        } else {
            // OpenAI / GPT-5-nano path
            response = callOpenAIAPI(config, "You are a connectivity test agent.", testQuery);
            result = extractSQLFromResponse(response);
        }
        
        if (result != null && !result.toLowerCase().contains("error")) {
            return "Connection Verified! Model: " + config.getModel() + " replied: " + result;
        } else {
            throw new Exception("Malformed response: " + response);
        }
    }

    private String extractSQLFromResponse(String apiResponse) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(apiResponse).getAsJsonObject();
            if (jsonResponse.has("choices")) {
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (!choices.isEmpty()) {
                    String content = choices.get(0).getAsJsonObject().get("message").getAsJsonObject().get("content").getAsString();
                    return content.replace("```sql", "").replace("```", "").trim();
                }
            }
            return "Error: Invalid response format";
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }

    private String extractContentFromClaude(String apiResponse) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(apiResponse).getAsJsonObject();
            if (jsonResponse.has("content")) {
                JsonArray contentArray = jsonResponse.getAsJsonArray("content");
                if (!contentArray.isEmpty()) {
                    return contentArray.get(0).getAsJsonObject().get("text").getAsString().trim();
                }
            }
            return "Error: Invalid Claude response format";
        } catch (Exception e) {
            return "Error parsing Claude response: " + e.getMessage();
        }
    }

    public boolean isConfigured() {
        return configManager.isConfigured();
    }

    public String getConfigurationStatus() {
        AIConfig lastUsed = configManager.getLastUsedConfig();
        if (lastUsed != null) {
            return "✓ AI Assistant profile: " + lastUsed.getName();
        } else {
            return "⚠ AI Assistant not configured. Configure in Edit → AI Configuration.";
        }
    }
}
