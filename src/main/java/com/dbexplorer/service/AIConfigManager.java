package com.dbexplorer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import com.dbexplorer.model.AIConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Service to manage AI Assistant configurations.
 * Loads and saves multiple AI settings with encrypted API keys.
 */
public class AIConfigManager {
    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"),
            ".dbexplorer", "ai-configs.json");
    private static final Path LAST_USED_FILE = Path.of(System.getProperty("user.home"),
            ".dbexplorer", "ai-last-used.txt");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private List<AIConfig> configs = new ArrayList<>();
    private String lastUsedId;

    public AIConfigManager() {
        loadConfigs();
        loadLastUsed();
    }

    /**
     * Loads all AI configurations from disk.
     */
    public void loadConfigs() {
        if (Files.exists(CONFIG_FILE)) {
            try {
                String json = Files.readString(CONFIG_FILE);
                configs = GSON.fromJson(json, new TypeToken<List<AIConfig>>(){}.getType());
                if (configs != null) {
                    for (AIConfig config : configs) {
                        if (config.getApiKey() != null) {
                            config.setApiKey(CryptoUtils.decrypt(config.getApiKey()));
                        }
                    }
                } else {
                    configs = new ArrayList<>();
                }
            } catch (IOException e) {
                System.err.println("Failed to load AI configs: " + e.getMessage());
                configs = new ArrayList<>();
            }
        }
    }

    private void loadLastUsed() {
        if (Files.exists(LAST_USED_FILE)) {
            try {
                lastUsedId = Files.readString(LAST_USED_FILE).trim();
            } catch (IOException e) {
                System.err.println("Failed to load last used AI config ID: " + e.getMessage());
            }
        }
    }

    /**
     * Saves all AI configurations to disk.
     */
    public void saveConfigs() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            
            List<AIConfig> copies = configs.stream().map(config -> {
                AIConfig copy = new AIConfig();
                copy.setId(config.getId());
                copy.setName(config.getName());
                copy.setApiProvider(config.getApiProvider());
                copy.setModel(config.getModel());
                copy.setApiKey(CryptoUtils.encrypt(config.getApiKey()));
                copy.setBaseUrl(config.getBaseUrl());
                copy.setMaxTokens(config.getMaxTokens());
                copy.setTemperature(config.getTemperature());
                copy.setEnabled(config.isEnabled());
                return copy;
            }).collect(Collectors.toList());
            
            String json = GSON.toJson(copies);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            System.err.println("Failed to save AI configs: " + e.getMessage());
        }
    }

    public void addConfig(AIConfig config) {
        if (config.getId() == null) {
            config.setId(UUID.randomUUID().toString());
        }
        configs.add(config);
        saveConfigs();
    }

    public void updateConfig(AIConfig config) {
        for (int i = 0; i < configs.size(); i++) {
            if (configs.get(i).getId().equals(config.getId())) {
                configs.set(i, config);
                break;
            }
        }
        saveConfigs();
    }

    public void deleteConfig(String id) {
        configs.removeIf(c -> c.getId().equals(id));
        if (id.equals(lastUsedId)) {
            lastUsedId = null;
            saveLastUsed();
        }
        saveConfigs();
    }

    public List<AIConfig> getConfigs() {
        return configs;
    }

    public AIConfig getConfig(String id) {
        return configs.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public AIConfig getLastUsedConfig() {
        if (lastUsedId != null) {
            AIConfig config = getConfig(lastUsedId);
            if (config != null) return config;
        }
        return configs.isEmpty() ? null : configs.get(0);
    }

    public void setLastUsedConfig(String id) {
        this.lastUsedId = id;
        saveLastUsed();
    }

    private void saveLastUsed() {
        try {
            Files.createDirectories(LAST_USED_FILE.getParent());
            Files.writeString(LAST_USED_FILE, lastUsedId != null ? lastUsedId : "");
        } catch (IOException e) {
            System.err.println("Failed to save last used AI config ID: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return !configs.isEmpty();
    }
}
