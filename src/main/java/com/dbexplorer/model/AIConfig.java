package com.dbexplorer.model;

/**
 * Configuration model for AI Assistant settings.
 * Stores API provider, model, and authentication details.
 */
public class AIConfig {
    private String id;
    private String name;                  // User-defined name for this configuration
    private String apiProvider;           // "OpenAI" or "Claude"
    private String model;                 // "gpt-3.5-turbo", "gpt-4", "claude-3-sonnet", etc.
    private String apiKey;                // Encrypted API key
    private String baseUrl;               // Optional: custom API endpoint
    private int maxTokens;                // Max tokens for response
    private double temperature;           // Temperature for model (0.0-2.0)
    private boolean enabled;              // Enable/disable AI Assistant

    public AIConfig() {
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getApiProvider() { return apiProvider; }
    public void setApiProvider(String apiProvider) { this.apiProvider = apiProvider; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return (name != null ? name : apiProvider + " - " + model) + " [" + (enabled ? "Enabled" : "Disabled") + "]";
    }
}
