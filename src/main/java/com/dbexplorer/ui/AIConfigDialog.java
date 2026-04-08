package com.dbexplorer.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.dbexplorer.model.AIConfig;
import com.dbexplorer.service.AIAssistantService;
import com.dbexplorer.service.AIConfigManager;

/**
 * Dialog for configuring AI Assistant settings.
 */
public class AIConfigDialog extends JDialog {
    private final AIConfigManager configManager;
    private AIConfig editingConfig;
    private boolean saved = false;

    private JList<AIConfig> configList;
    private DefaultListModel<AIConfig> listModel;

    private JTextField nameField;
    private JComboBox<String> providerCombo;
    private JComboBox<String> modelCombo;
    private JPasswordField apiKeyField;
    private JTextField baseUrlField;
    private JSpinner maxTokensSpinner;
    private JSlider temperatureSlider;
    private JLabel temperatureLabel;
    private JCheckBox enabledCheckBox;
    private JButton testButton;
    private JButton saveButton;
    private JButton deleteButton;

    public AIConfigDialog(Frame owner, AIConfigManager configManager) {
        super(owner, "AI Assistant Configuration", true);
        this.configManager = configManager;
        initializeUI();
        loadConfigs();
        
        // If no configs exist, start with default fields populated
        if (configManager.getConfigs().isEmpty()) {
            clearFields();
        }
        
        pack();
        setLocationRelativeTo(owner);
    }

    private void initializeUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(800, 600));

        // Left side: Config list
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(250, 0));

        listModel = new DefaultListModel<>();
        configList = new JList<>(listModel);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                AIConfig selected = configList.getSelectedValue();
                if (selected != null) {
                    populateFields(selected);
                } else {
                    clearFields();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(configList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JPanel listButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton addButton = new JButton("Add New");
        addButton.addActionListener(e -> {
            configList.clearSelection();
            clearFields();
            editingConfig = new AIConfig();
            nameField.requestFocus();
        });
        listButtons.add(addButton);
        
        deleteButton = new JButton("Delete");
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteConfig());
        listButtons.add(deleteButton);
        leftPanel.add(listButtons, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // Right side: Config details
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Name section
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.setBorder(BorderFactory.createTitledBorder("Config Name"));
        nameField = new JTextField();
        namePanel.add(nameField, BorderLayout.CENTER);
        mainPanel.add(namePanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Provider and Model section
        JPanel providerPanel = createProviderSection();
        mainPanel.add(providerPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // API Key section
        JPanel apiKeyPanel = createApiKeySection();
        mainPanel.add(apiKeyPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Advanced settings section
        JPanel advancedPanel = createAdvancedSection();
        mainPanel.add(advancedPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Enabled checkbox
        enabledCheckBox = new JCheckBox("Enable AI Assistant");
        enabledCheckBox.setFont(enabledCheckBox.getFont().deriveFont(Font.BOLD));
        mainPanel.add(enabledCheckBox);
        mainPanel.add(Box.createVerticalStrut(10));

        // Buttons panel
        JPanel buttonsPanel = createButtonsPanel();
        mainPanel.add(buttonsPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createProviderSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("API Provider & Model"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Provider:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        providerCombo = new JComboBox<>(new String[]{"OpenAI", "Claude", "Custom"});
        providerCombo.addActionListener(e -> updateModelCombo());
        panel.add(providerCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Model:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        modelCombo = new JComboBox<>();
        modelCombo.setEditable(false);
        panel.add(modelCombo, gbc);

        return panel;
    }

    private JPanel createApiKeySection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Authentication"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("API Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        apiKeyField = new JPasswordField(30);
        panel.add(apiKeyField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JLabel helpLabel = new JLabel("<html>This key will be encrypted and saved securely.</html>");
        helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN, 10f));
        helpLabel.setForeground(Color.GRAY);
        panel.add(helpLabel, gbc);

        return panel;
    }

    private JPanel createAdvancedSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Advanced Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Base URL:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        baseUrlField = new JTextField(30);
        panel.add(baseUrlField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Max Tokens:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 4000, 100));
        panel.add(maxTokensSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Temperature:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel tempPanel = new JPanel(new BorderLayout());
        temperatureSlider = new JSlider(0, 200, 70);
        temperatureSlider.setMajorTickSpacing(50);
        temperatureSlider.setMinorTickSpacing(10);
        temperatureSlider.setPaintTicks(true);
        temperatureSlider.setPaintLabels(true);
        temperatureSlider.addChangeListener(e -> updateTemperatureLabel());
        temperatureLabel = new JLabel("0.70");
        temperatureLabel.setPreferredSize(new Dimension(40, 20));
        
        tempPanel.add(temperatureSlider, BorderLayout.CENTER);
        tempPanel.add(temperatureLabel, BorderLayout.EAST);
        panel.add(tempPanel, gbc);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection());
        panel.add(testButton);

        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveConfig());
        panel.add(saveButton);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        return panel;
    }

    private void updateModelCombo() {
        String provider = (String) providerCombo.getSelectedItem();
        Object currentModel = modelCombo.getSelectedItem();
        modelCombo.removeAllItems();

        if ("OpenAI".equals(provider)) {
            modelCombo.addItem("gpt-5-nano");
            modelCombo.addItem("gpt-4-turbo");
            modelCombo.addItem("gpt-4");
            modelCombo.addItem("gpt-3.5-turbo");
            baseUrlField.setText("https://api.openai.com/v1");
        } else if ("Claude".equals(provider)) {
            modelCombo.addItem("claude-sonnet-4-20250514");
            modelCombo.addItem("claude-3-5-sonnet-20240620");
            modelCombo.addItem("claude-3-opus-20240229");
            modelCombo.addItem("claude-3-sonnet-20240229");
            modelCombo.addItem("claude-3-haiku-20240307");
            baseUrlField.setText("https://api.anthropic.com/v1");
        } else {
            modelCombo.addItem("Custom Model");
            baseUrlField.setText("");
        }
        
        if (currentModel != null) {
            modelCombo.setSelectedItem(currentModel);
        } else {
            modelCombo.setSelectedIndex(0);
        }
    }

    private void updateTemperatureLabel() {
        double temp = temperatureSlider.getValue() / 100.0;
        temperatureLabel.setText(String.format("%.2f", temp));
    }

    private void populateFields(AIConfig config) {
        editingConfig = config;
        nameField.setText(config.getName());
        providerCombo.setSelectedItem(config.getApiProvider());
        updateModelCombo();
        modelCombo.setSelectedItem(config.getModel());
        apiKeyField.setText(config.getApiKey());
        baseUrlField.setText(config.getBaseUrl());
        maxTokensSpinner.setValue(config.getMaxTokens());
        temperatureSlider.setValue((int)(config.getTemperature() * 100));
        updateTemperatureLabel();
        enabledCheckBox.setSelected(config.isEnabled());
        deleteButton.setEnabled(true);
    }

    private void clearFields() {
        editingConfig = null;
        nameField.setText("");
        providerCombo.setSelectedIndex(0);
        updateModelCombo();
        // Since updateModelCombo sets index 0, model and base URL are now populated
        apiKeyField.setText("");
        maxTokensSpinner.setValue(1000);
        temperatureSlider.setValue(70);
        updateTemperatureLabel();
        enabledCheckBox.setSelected(true);
        deleteButton.setEnabled(false);
    }

    private void loadConfigs() {
        listModel.clear();
        for (AIConfig config : configManager.getConfigs()) {
            listModel.addElement(config);
        }
    }

    private void saveConfig() {
        if (nameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a name for this config.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (editingConfig == null) editingConfig = new AIConfig();
        
        editingConfig.setName(nameField.getText());
        editingConfig.setApiProvider((String) providerCombo.getSelectedItem());
        editingConfig.setModel((String) modelCombo.getSelectedItem());
        editingConfig.setApiKey(new String(apiKeyField.getPassword()));
        editingConfig.setBaseUrl(baseUrlField.getText());
        editingConfig.setMaxTokens((Integer) maxTokensSpinner.getValue());
        editingConfig.setTemperature(temperatureSlider.getValue() / 100.0);
        editingConfig.setEnabled(enabledCheckBox.isSelected());

        if (editingConfig.getId() == null) {
            configManager.addConfig(editingConfig);
        } else {
            configManager.updateConfig(editingConfig);
        }
        
        loadConfigs();
        saved = true;
        JOptionPane.showMessageDialog(this, "Configuration saved!");
    }

    private void deleteConfig() {
        AIConfig selected = configList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + selected.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                configManager.deleteConfig(selected.getId());
                loadConfigs();
                clearFields();
            }
        }
    }

    private void testConnection() {
        AIConfig testConfig = new AIConfig();
        testConfig.setApiProvider((String) providerCombo.getSelectedItem());
        testConfig.setModel((String) modelCombo.getSelectedItem());
        testConfig.setApiKey(new String(apiKeyField.getPassword()));
        testConfig.setBaseUrl(baseUrlField.getText());
        testConfig.setMaxTokens((Integer) maxTokensSpinner.getValue());
        testConfig.setTemperature(temperatureSlider.getValue() / 100.0);
        
        testButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new Thread(() -> {
            try {
                AIAssistantService service = new AIAssistantService(configManager);
                String result = service.testConnection(testConfig);
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    testButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, result);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    testButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public boolean isSaved() { return saved; }

    private AIConfig createDefaultConfig() {
        AIConfig config = new AIConfig();
        config.setName("Default OpenAI");
        config.setApiProvider("OpenAI");
        config.setModel("gpt-3.5-turbo");
        config.setBaseUrl("https://api.openai.com/v1");
        config.setMaxTokens(1000);
        config.setTemperature(0.7);
        config.setEnabled(true);
        return config;
    }
}
