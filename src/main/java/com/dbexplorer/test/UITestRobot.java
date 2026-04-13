package com.dbexplorer.test;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.dbexplorer.model.DatabaseType;
import com.dbexplorer.ui.MainFrame;
import com.dbexplorer.ui.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Standalone UI test robot for DB Explorer.
 * Drives the Swing application through end-to-end test cases using java.awt.Robot.
 *
 * Usage: java -cp db-explorer.jar com.dbexplorer.test.UITestRobot test-config.json
 */
public class UITestRobot {

    // =========================================================================
    // Instance Fields
    // =========================================================================

    private java.awt.Robot robot;
    private JFrame mainFrame;
    private TestConfig config;
    private TestReporter reporter;
    private boolean aiConfigSkipped = false;

    // =========================================================================
    // Data Models
    // =========================================================================

    /**
     * Test configuration parsed from JSON.
     */
    public static class TestConfig {
        // Required fields
        private String connectionName;
        private String databaseType;
        private String host;
        private int port;
        private String databaseName;
        private String username;
        private String password;

        // Optional fields
        private String query;
        private String ddlScriptPath;
        private String aiProvider;
        private String aiModel;
        private String aiBaseUrl;
        private String aiApiKey;
        private String aiPrompt;

        public TestConfig() {}

        public TestConfig(String connectionName, String databaseType, String host, int port,
                          String databaseName, String username, String password,
                          String query, String ddlScriptPath,
                          String aiProvider, String aiModel, String aiBaseUrl,
                          String aiApiKey, String aiPrompt) {
            this.connectionName = connectionName;
            this.databaseType = databaseType;
            this.host = host;
            this.port = port;
            this.databaseName = databaseName;
            this.username = username;
            this.password = password;
            this.query = query;
            this.ddlScriptPath = ddlScriptPath;
            this.aiProvider = aiProvider;
            this.aiModel = aiModel;
            this.aiBaseUrl = aiBaseUrl;
            this.aiApiKey = aiApiKey;
            this.aiPrompt = aiPrompt;
        }

        // Getters
        public String getConnectionName() { return connectionName; }
        public String getDatabaseType() { return databaseType; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getDatabaseName() { return databaseName; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getQuery() { return query; }
        public String getDdlScriptPath() { return ddlScriptPath; }
        public String getAiProvider() { return aiProvider; }
        public String getAiModel() { return aiModel; }
        public String getAiBaseUrl() { return aiBaseUrl; }
        public String getAiApiKey() { return aiApiKey; }
        public String getAiPrompt() { return aiPrompt; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestConfig that = (TestConfig) o;
            return port == that.port
                    && Objects.equals(connectionName, that.connectionName)
                    && Objects.equals(databaseType, that.databaseType)
                    && Objects.equals(host, that.host)
                    && Objects.equals(databaseName, that.databaseName)
                    && Objects.equals(username, that.username)
                    && Objects.equals(password, that.password)
                    && Objects.equals(query, that.query)
                    && Objects.equals(ddlScriptPath, that.ddlScriptPath)
                    && Objects.equals(aiProvider, that.aiProvider)
                    && Objects.equals(aiModel, that.aiModel)
                    && Objects.equals(aiBaseUrl, that.aiBaseUrl)
                    && Objects.equals(aiApiKey, that.aiApiKey)
                    && Objects.equals(aiPrompt, that.aiPrompt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(connectionName, databaseType, host, port, databaseName,
                    username, password, query, ddlScriptPath,
                    aiProvider, aiModel, aiBaseUrl, aiApiKey, aiPrompt);
        }

        @Override
        public String toString() {
            return "TestConfig{" +
                    "connectionName='" + connectionName + '\'' +
                    ", databaseType='" + databaseType + '\'' +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    ", databaseName='" + databaseName + '\'' +
                    ", username='" + username + '\'' +
                    ", password='***'" +
                    ", query='" + query + '\'' +
                    ", ddlScriptPath='" + ddlScriptPath + '\'' +
                    ", aiProvider='" + aiProvider + '\'' +
                    ", aiModel='" + aiModel + '\'' +
                    ", aiBaseUrl='" + aiBaseUrl + '\'' +
                    ", aiApiKey='***'" +
                    ", aiPrompt='" + aiPrompt + '\'' +
                    '}';
        }
    }

    /**
     * Result of a single test case execution.
     */
    public static class TestResult {
        public enum Status { PASS, FAIL, SKIP }

        private final String name;
        private final Status status;
        private final long durationMs;
        private final String failureReason;

        public TestResult(String name, Status status, long durationMs, String failureReason) {
            this.name = name;
            this.status = status;
            this.durationMs = durationMs;
            this.failureReason = failureReason;
        }

        public String getName() { return name; }
        public Status getStatus() { return status; }
        public long getDurationMs() { return durationMs; }
        public String getFailureReason() { return failureReason; }
    }

    // =========================================================================
    // Test Reporter
    // =========================================================================

    /**
     * Simple test reporting utility that collects results and prints formatted output.
     */
    public static class TestReporter {
        private final List<TestResult> results = new ArrayList<>();

        public void reportPass(String testName, long durationMs) {
            results.add(new TestResult(testName, TestResult.Status.PASS, durationMs, null));
            System.out.println(formatPass(testName, durationMs));
        }

        public void reportFail(String testName, long durationMs, String reason) {
            results.add(new TestResult(testName, TestResult.Status.FAIL, durationMs, reason));
            System.out.println(formatFail(testName, durationMs, reason));
        }

        public void reportSkip(String testName, String reason) {
            results.add(new TestResult(testName, TestResult.Status.SKIP, 0, reason));
            System.out.println(formatSkip(testName, reason));
        }

        public void printSummary() {
            long passed = results.stream().filter(r -> r.getStatus() == TestResult.Status.PASS).count();
            long failed = results.stream().filter(r -> r.getStatus() == TestResult.Status.FAIL).count();
            long skipped = results.stream().filter(r -> r.getStatus() == TestResult.Status.SKIP).count();
            System.out.println("=== Test Summary ===");
            System.out.println("Total: " + results.size() + " | Passed: " + passed
                    + " | Failed: " + failed + " | Skipped: " + skipped);
        }

        public int getExitCode() {
            boolean anyFailed = results.stream().anyMatch(r -> r.getStatus() == TestResult.Status.FAIL);
            return anyFailed ? 1 : 0;
        }

        public List<TestResult> getResults() {
            return List.copyOf(results);
        }

        // Static package-private format methods for testability

        static String formatPass(String testName, long durationMs) {
            return "[PASS] " + testName + " (" + durationMs + "ms)";
        }

        static String formatFail(String testName, long durationMs, String reason) {
            return "[FAIL] " + testName + " (" + durationMs + "ms): " + reason;
        }

        static String formatSkip(String testName, String reason) {
            return "[SKIP] " + testName + ": " + reason;
        }
    }

    // =========================================================================
    // Configuration Parsing
    // =========================================================================

    private static final Gson GSON = new Gson();

    /**
     * Parse a TestConfig from a JSON file.
     *
     * @param filePath path to the JSON configuration file
     * @return parsed TestConfig
     * @throws IllegalArgumentException if the file is missing, JSON is invalid, or required fields are absent
     */
    public static TestConfig parseConfig(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Config file not found: " + filePath);
        }

        String json;
        try {
            json = Files.readString(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read config file: " + filePath + " - " + e.getMessage(), e);
        }

        return parseConfigFromJson(json);
    }

    /**
     * Parse a TestConfig from a JSON string.
     * Visible for testing.
     *
     * @param json JSON string
     * @return parsed TestConfig
     * @throws IllegalArgumentException if JSON is invalid or required fields are missing
     */
    public static TestConfig parseConfigFromJson(String json) {
        TestConfig config;
        try {
            config = GSON.fromJson(json, TestConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON in config: " + e.getMessage(), e);
        }

        if (config == null) {
            throw new IllegalArgumentException("Invalid JSON in config: parsed to null");
        }

        validateRequiredFields(config);
        return config;
    }

    private static void validateRequiredFields(TestConfig config) {
        List<String> missing = new ArrayList<>();

        if (isNullOrEmpty(config.connectionName)) missing.add("connectionName");
        if (isNullOrEmpty(config.databaseType)) missing.add("databaseType");
        if (isNullOrEmpty(config.host)) missing.add("host");
        if (config.port <= 0) missing.add("port (must be > 0)");
        if (isNullOrEmpty(config.databaseName)) missing.add("databaseName");
        if (isNullOrEmpty(config.username)) missing.add("username");
        if (isNullOrEmpty(config.password)) missing.add("password");

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing or invalid required config fields: " + String.join(", ", missing));
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Returns true when AI test cases should be skipped (aiProvider or aiApiKey is null/empty).
     */
    public static boolean shouldSkipAI(TestConfig config) {
        return isNullOrEmpty(config.aiProvider) || isNullOrEmpty(config.aiApiKey);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    // =========================================================================
    // RobotHelper Methods
    // =========================================================================

    /**
     * Initialize the Robot instance lazily.
     */
    private void ensureRobot() {
        if (robot == null) {
            try {
                robot = new java.awt.Robot();
                robot.setAutoDelay(50);
                robot.setAutoWaitForIdle(true);
            } catch (AWTException e) {
                throw new RuntimeException("Failed to create java.awt.Robot: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Thread.sleep wrapper with default 500ms.
     *
     * @param ms milliseconds to sleep; if &lt;= 0, defaults to 500ms
     */
    public void delay(long ms) {
        try {
            Thread.sleep(ms <= 0 ? 500 : ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Convenience overload: delay with default 500ms.
     */
    public void delay() {
        delay(500);
    }

    /**
     * EDT-safe recursive search for a component matching the given type and predicate.
     *
     * @param container the root container to search
     * @param type      the component class to match
     * @param predicate additional filter predicate
     * @param <T>       component type
     * @return the first matching component, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T findComponent(Container container, Class<T> type, Predicate<T> predicate) {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(findComponentRecursive(container, type, predicate)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            // ignore — return null
        }
        return result.get();
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentRecursive(Container container, Class<T> type, Predicate<T> predicate) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                T candidate = (T) child;
                if (predicate.test(candidate)) {
                    return candidate;
                }
            }
            if (child instanceof Container childContainer) {
                T found = findComponentRecursive(childContainer, type, predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Find a component by its name property (Component.getName()).
     *
     * @param container the root container to search
     * @param name      the component name to match
     * @return the first matching component, or null if not found
     */
    public Component findComponentByName(Container container, String name) {
        return findComponent(container, Component.class, c -> name.equals(c.getName()));
    }

    /**
     * Poll for a JDialog whose title contains the given substring.
     *
     * @param titleSubstring substring to match in dialog title
     * @param timeoutMs      maximum time to wait in milliseconds
     * @return the matching JDialog, or null if not found within timeout
     */
    public JDialog findDialog(String titleSubstring, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog
                        && dialog.isVisible()
                        && dialog.getTitle() != null
                        && dialog.getTitle().contains(titleSubstring)) {
                    return dialog;
                }
            }
            delay(100);
        }
        return null;
    }

    /**
     * Find a JButton by its text or tooltip text.
     *
     * @param container     the root container to search
     * @param textOrTooltip the button text or tooltip text to match
     * @return the first matching JButton, or null if not found
     */
    public JButton findButton(Container container, String textOrTooltip) {
        return findComponent(container, JButton.class, btn -> {
            String btnText = btn.getText();
            String btnTip = btn.getToolTipText();
            // Strip HTML tags from tooltip for matching
            String tipPlain = btnTip != null ? btnTip.replaceAll("<[^>]*>", "").trim() : "";
            return textOrTooltip.equals(btnText)
                    || textOrTooltip.equals(btnTip)
                    || (btnText != null && btnText.contains(textOrTooltip))
                    || (btnTip != null && btnTip.contains(textOrTooltip))
                    || tipPlain.contains(textOrTooltip);
        });
    }

    /**
     * Move mouse to the center of the component on screen and click (left button).
     *
     * @param component the component to click
     */
    public void clickComponent(Component component) {
        ensureRobot();
        try {
            // Wait for the component to be showing on screen (up to 3 seconds)
            for (int i = 0; i < 30; i++) {
                AtomicReference<Boolean> showing = new AtomicReference<>(false);
                SwingUtilities.invokeAndWait(() -> showing.set(component.isShowing()));
                if (showing.get()) break;
                Thread.sleep(100);
            }

            AtomicReference<Point> locRef = new AtomicReference<>();
            AtomicReference<java.awt.Dimension> sizeRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                if (component.isShowing()) {
                    locRef.set(component.getLocationOnScreen());
                    sizeRef.set(component.getSize());
                }
            });
            Point loc = locRef.get();
            java.awt.Dimension size = sizeRef.get();
            if (loc == null || size == null) {
                throw new RuntimeException("Component not visible on screen: " + component.getClass().getSimpleName());
            }
            int centerX = loc.x + size.width / 2;
            int centerY = loc.y + size.height / 2;
            robot.mouseMove(centerX, centerY);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            showClickSpark(centerX, centerY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw new RuntimeException("Failed to get component location: " +
                    (cause != null ? cause.getMessage() : e.getMessage()), e);
        }
    }

    /**
     * Shows a brief visual spark at the given screen coordinates AFTER a click.
     * Uses a non-focusable JWindow so it never steals input from the target.
     */
    private void showClickSpark(int screenX, int screenY) {
        SwingUtilities.invokeLater(() -> {
            try {
                int sparkSize = 40;
                JWindow spark = new JWindow();
                spark.setAlwaysOnTop(true);
                spark.setFocusableWindowState(false); // critical: don't steal focus
                spark.setBackground(new Color(0, 0, 0, 0));
                spark.setBounds(screenX - sparkSize / 2, screenY - sparkSize / 2, sparkSize, sparkSize);

                final float[] alpha = {1.0f};
                final int[] radius = {6};

                JPanel sparkPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        int r = radius[0];
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha[0] * 0.3f)));
                        g2.setColor(new Color(255, 200, 50));
                        g2.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alpha[0])));
                        g2.setColor(new Color(255, 255, 100));
                        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                        g2.setColor(Color.WHITE);
                        g2.fillOval(cx - 3, cy - 3, 6, 6);
                        g2.dispose();
                    }
                };
                sparkPanel.setOpaque(false);
                spark.setContentPane(sparkPanel);
                spark.setVisible(true);

                javax.swing.Timer timer = new javax.swing.Timer(30, null);
                timer.addActionListener(e -> {
                    alpha[0] -= 0.1f;
                    radius[0] += 2;
                    if (alpha[0] <= 0) {
                        timer.stop();
                        spark.dispose();
                    } else {
                        sparkPanel.repaint();
                    }
                });
                timer.start();
            } catch (Exception ignored) {}
        });
    }

    /**
     * Find a JButton by text or tooltip and click it.
     *
     * @param container     the root container to search
     * @param textOrTooltip the button text or tooltip text to match
     * @throws AssertionError if the button is not found
     */
    public void clickButton(Container container, String textOrTooltip) {
        JButton button = findButton(container, textOrTooltip);
        if (button == null) {
            throw new AssertionError("Button not found: " + textOrTooltip);
        }
        clickComponent(button);
    }

    /**
     * Type a string character by character via Robot key events.
     * Handles uppercase letters by pressing/releasing Shift.
     *
     * @param text the text to type
     */
    public void typeText(String text) {
        ensureRobot();
        for (char c : text.toCharArray()) {
            typeChar(c);
        }
    }

    private void typeChar(char c) {
        if (Character.isUpperCase(c)) {
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(Character.toLowerCase(c)));
            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(Character.toLowerCase(c)));
            robot.keyRelease(KeyEvent.VK_SHIFT);
        } else {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                // For special characters, use shift + base key mapping
                typeSpecialChar(c);
            } else {
                // Some characters need shift (e.g., !, @, #, etc.)
                if (isShiftRequired(c)) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                } else {
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                }
            }
        }
    }

    private boolean isShiftRequired(char c) {
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    private void typeSpecialChar(char c) {
        // Fallback: use clipboard to paste the character
        java.awt.datatransfer.StringSelection selection =
                new java.awt.datatransfer.StringSelection(String.valueOf(c));
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    /**
     * Press and release a single key.
     *
     * @param keyCode the KeyEvent key code
     */
    public void pressKey(int keyCode) {
        ensureRobot();
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    /**
     * Press Ctrl + a key.
     *
     * @param keyCode the KeyEvent key code to combine with Ctrl
     */
    public void pressCtrlKey(int keyCode) {
        ensureRobot();
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    /**
     * Poll at 100ms intervals until the predicate returns true, or throw AssertionError on timeout.
     *
     * @param predicate   the condition to wait for
     * @param timeoutMs   maximum time to wait in milliseconds
     * @param description description of what we're waiting for (used in error message)
     * @throws AssertionError if the condition is not met within the timeout
     */
    public void waitForCondition(java.util.function.BooleanSupplier predicate, long timeoutMs, String description) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (predicate.getAsBoolean()) {
                return;
            }
            delay(100);
        }
        throw new AssertionError("Timed out waiting for: " + description + " (timeout: " + timeoutMs + "ms)");
    }

    // =========================================================================
    // Tree and Menu Helper Methods
    // =========================================================================

    /**
     * Find a JTree node by matching user object toString() against the given text.
     * Performs a recursive search through the tree model starting from the root.
     *
     * @param tree the JTree to search
     * @param text the text to match against node user object toString()
     * @return the matching DefaultMutableTreeNode, or null if not found
     */
    public DefaultMutableTreeNode getTreeNodeByText(JTree tree, String text) {
        AtomicReference<DefaultMutableTreeNode> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                TreeModel model = tree.getModel();
                Object root = model.getRoot();
                if (root instanceof DefaultMutableTreeNode rootNode) {
                    result.set(searchTreeNode(rootNode, text));
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            // ignore — return null
        }
        return result.get();
    }

    private DefaultMutableTreeNode searchTreeNode(DefaultMutableTreeNode node, String text) {
        Object userObject = node.getUserObject();
        if (userObject != null) {
            String nodeText = userObject.toString();
            // Match by exact equals or contains (tree may show "Name (type)" format)
            if (nodeText.equals(text) || nodeText.contains(text)) {
                return node;
            }
        }
        Enumeration<?> children = node.children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (child instanceof DefaultMutableTreeNode childNode) {
                DefaultMutableTreeNode found = searchTreeNode(childNode, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Expand a tree path on the EDT.
     *
     * @param tree the JTree
     * @param path the TreePath to expand
     */
    public void expandTreeNode(JTree tree, TreePath path) {
        try {
            SwingUtilities.invokeAndWait(() -> tree.expandPath(path));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to expand tree node: " + e.getMessage(), e);
        }
    }

    /**
     * Double-click a tree node by computing the row bounds and clicking the center.
     *
     * @param tree the JTree
     * @param node the node to double-click
     */
    public void doubleClickTreeNode(JTree tree, DefaultMutableTreeNode node) {
        ensureRobot();
        try {
            AtomicReference<Point> locRef = new AtomicReference<>();
            AtomicReference<Rectangle> boundsRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                TreePath path = new TreePath(node.getPath());
                int row = tree.getRowForPath(path);
                Rectangle bounds = tree.getRowBounds(row);
                boundsRef.set(bounds);
                locRef.set(tree.getLocationOnScreen());
            });
            Point treeLoc = locRef.get();
            Rectangle bounds = boundsRef.get();
            if (bounds == null) {
                throw new AssertionError("Could not get row bounds for tree node: " + node);
            }
            int x = treeLoc.x + bounds.x + bounds.width / 2;
            int y = treeLoc.y + bounds.y + bounds.height / 2;
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to double-click tree node: " + e.getMessage(), e);
        }
    }

    /**
     * Right-click a tree node by computing the row bounds and clicking the center with BUTTON3.
     *
     * @param tree the JTree
     * @param node the node to right-click
     */
    public void rightClickTreeNode(JTree tree, DefaultMutableTreeNode node) {
        ensureRobot();
        try {
            AtomicReference<Point> locRef = new AtomicReference<>();
            AtomicReference<Rectangle> boundsRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                TreePath path = new TreePath(node.getPath());
                int row = tree.getRowForPath(path);
                Rectangle bounds = tree.getRowBounds(row);
                boundsRef.set(bounds);
                locRef.set(tree.getLocationOnScreen());
            });
            Point treeLoc = locRef.get();
            Rectangle bounds = boundsRef.get();
            if (bounds == null) {
                throw new AssertionError("Could not get row bounds for tree node: " + node);
            }
            int x = treeLoc.x + bounds.x + bounds.width / 2;
            int y = treeLoc.y + bounds.y + bounds.height / 2;
            robot.mouseMove(x, y);
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to right-click tree node: " + e.getMessage(), e);
        }
    }

    /**
     * Find a JMenuItem by text within a Container (popup menu or menu bar) and click it.
     *
     * @param container the container to search (e.g., JPopupMenu)
     * @param text      the menu item text to match
     * @throws AssertionError if the menu item is not found
     */
    public void clickMenuItem(Container container, String text) {
        // First try finding in the given container
        JMenuItem item = findComponent(container, JMenuItem.class,
                mi -> text.equals(mi.getText()) || (mi.getText() != null && mi.getText().contains(text)));
        
        // If not found, search all visible popup menus (they live in separate heavyweight windows)
        if (item == null) {
            for (Window w : Window.getWindows()) {
                if (w.isVisible()) {
                    item = findComponent(w, JMenuItem.class,
                            mi -> text.equals(mi.getText()) || (mi.getText() != null && mi.getText().contains(text)));
                    if (item != null) break;
                }
            }
        }
        
        if (item == null) {
            throw new AssertionError("Menu item not found: " + text);
        }
        clickComponent(item);
    }

    /**
     * Navigate a menu bar: click the menu to open it, then find and click the menu item.
     *
     * @param menuBar  the JMenuBar
     * @param menuText the text of the JMenu to open
     * @param itemText the text of the JMenuItem to click
     * @throws AssertionError if the menu or menu item is not found
     */
    public void clickMenuBarItem(JMenuBar menuBar, String menuText, String itemText) {
        // Find the menu by text
        JMenu targetMenu = null;
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null && menuText.equals(menu.getText())) {
                targetMenu = menu;
                break;
            }
        }
        if (targetMenu == null) {
            throw new AssertionError("Menu not found: " + menuText);
        }

        // Click the menu to open it
        clickComponent(targetMenu);
        delay(300);

        // Find and click the menu item
        JMenuItem targetItem = null;
        for (int i = 0; i < targetMenu.getItemCount(); i++) {
            JMenuItem item = targetMenu.getItem(i);
            if (item != null && itemText.equals(item.getText())) {
                targetItem = item;
                break;
            }
        }
        if (targetItem == null) {
            throw new AssertionError("Menu item not found: " + itemText + " in menu: " + menuText);
        }
        clickComponent(targetItem);
    }

    // =========================================================================
    // Test Case Implementations
    // =========================================================================

    private void testLaunch() {
        // Apply initial theme
        ThemeManager.applyInitialTheme();

        // Create and display MainFrame on EDT
        try {
            SwingUtilities.invokeAndWait(() -> {
                mainFrame = new MainFrame();
                mainFrame.setVisible(true);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while launching MainFrame");
        } catch (InvocationTargetException e) {
            throw new AssertionError("Failed to launch MainFrame: " + e.getCause().getMessage());
        }

        // Wait up to 10 seconds for frame to be visible
        waitForCondition(() -> {
            AtomicReference<Boolean> visible = new AtomicReference<>(false);
            try {
                SwingUtilities.invokeAndWait(() -> visible.set(mainFrame.isVisible() && mainFrame.isShowing()));
            } catch (Exception ignored) {}
            return visible.get();
        }, 10000, "MainFrame to become visible");

        // Maximize the window
        try {
            SwingUtilities.invokeAndWait(() -> mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH));
        } catch (Exception ignored) {}
        delay(500);
    }

    private void testAddConnection() {
        ensureRobot();
        // Click "Add Connection" toolbar button
        clickButton(mainFrame, "Add Connection");
        delay();

        // Wait for ConnectionDialog
        JDialog dialog = findDialog("Connection", 5000);
        if (dialog == null) throw new AssertionError("ConnectionDialog did not appear within 5 seconds");
        delay(300);

        // Find the database type combo and select matching DatabaseType
        JComboBox<?> dbTypeCombo = findComponent(dialog, JComboBox.class, c -> true);
        if (dbTypeCombo == null) throw new AssertionError("Database type combo not found");
        DatabaseType targetType = DatabaseType.valueOf(config.getDatabaseType());
        try {
            SwingUtilities.invokeAndWait(() -> dbTypeCombo.setSelectedItem(targetType));
        } catch (Exception e) {
            throw new AssertionError("Failed to set database type: " + e.getMessage());
        }
        delay(300);

        // Find all text fields in the dialog and fill them
        // Fields order in ConnectionDialog for JDBC types: name, host, port, database, username, password, driverPath
        List<JTextField> textFields = new ArrayList<>();
        List<JPasswordField> passwordFields = new ArrayList<>();
        findAllComponents(dialog, JTextField.class, tf -> !(tf instanceof JPasswordField) && tf.isVisible(), textFields);
        findAllComponents(dialog, JPasswordField.class, pf -> pf.isVisible(), passwordFields);

        // Fill connection name (first text field)
        if (textFields.size() >= 1) { clearAndType(textFields.get(0), config.getConnectionName()); }
        // Fill host (second text field, after name)
        if (textFields.size() >= 2) { clearAndType(textFields.get(1), config.getHost()); }
        // Fill port (third text field)
        if (textFields.size() >= 3) { clearAndType(textFields.get(2), String.valueOf(config.getPort())); }
        // Fill database (fourth text field)
        if (textFields.size() >= 4) { clearAndType(textFields.get(3), config.getDatabaseName()); }
        // Fill username (fifth text field)
        if (textFields.size() >= 5) { clearAndType(textFields.get(4), config.getUsername()); }
        // Fill password (first password field)
        if (passwordFields.size() >= 1) { clearAndType(passwordFields.get(0), config.getPassword()); }

        delay(300);

        // Click "Save" button
        clickButton(dialog, "Save");
        delay();

        // Wait for dialog to close
        waitForCondition(() -> !dialog.isVisible(), 5000, "ConnectionDialog to close after Save");

        // Verify connection appears in the JTree (poll for up to 5 seconds)
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found in MainFrame");
        
        final JTree finalTree = tree;
        waitForCondition(() -> getTreeNodeByText(finalTree, config.getConnectionName()) != null,
                5000, "Connection '" + config.getConnectionName() + "' to appear in tree");
        
        DefaultMutableTreeNode node = getTreeNodeByText(tree, config.getConnectionName());
        if (node == null) throw new AssertionError("Connection '" + config.getConnectionName() + "' not found in tree");
    }

    private void testTestConnection() {
        ensureRobot();
        // Click "Add Connection" toolbar button
        clickButton(mainFrame, "Add Connection");
        delay();

        // Wait for ConnectionDialog
        JDialog dialog = findDialog("Connection", 5000);
        if (dialog == null) throw new AssertionError("ConnectionDialog did not appear");
        delay(300);

        // Select database type
        JComboBox<?> dbTypeCombo = findComponent(dialog, JComboBox.class, c -> true);
        if (dbTypeCombo != null) {
            DatabaseType targetType = DatabaseType.valueOf(config.getDatabaseType());
            try { SwingUtilities.invokeAndWait(() -> dbTypeCombo.setSelectedItem(targetType)); } catch (Exception ignored) {}
        }
        delay(300);

        // Fill fields from config
        List<JTextField> textFields = new ArrayList<>();
        List<JPasswordField> passwordFields = new ArrayList<>();
        findAllComponents(dialog, JTextField.class, tf -> !(tf instanceof JPasswordField) && tf.isVisible(), textFields);
        findAllComponents(dialog, JPasswordField.class, pf -> pf.isVisible(), passwordFields);

        if (textFields.size() >= 1) clearAndType(textFields.get(0), config.getConnectionName());
        if (textFields.size() >= 2) clearAndType(textFields.get(1), config.getHost());
        if (textFields.size() >= 3) clearAndType(textFields.get(2), String.valueOf(config.getPort()));
        if (textFields.size() >= 4) clearAndType(textFields.get(3), config.getDatabaseName());
        if (textFields.size() >= 5) clearAndType(textFields.get(4), config.getUsername());
        if (passwordFields.size() >= 1) clearAndType(passwordFields.get(0), config.getPassword());
        delay(300);

        // Click "Test Connection" button
        clickButton(dialog, "Test Connection");

        // Wait for success/error dialog
        JDialog resultDialog = findDialog("Test", 15000);
        if (resultDialog == null) {
            resultDialog = findDialog("Connection", 5000);
        }
        if (resultDialog == null) throw new AssertionError("Test Connection result dialog did not appear");

        // Click OK to dismiss the result dialog
        delay(300);
        clickButton(resultDialog, "OK");
        delay(300);

        // Now click "Save" to save the connection (instead of Cancel)
        if (dialog.isVisible()) {
            clickButton(dialog, "Save");
            delay();
        }

        // Wait for dialog to close
        waitForCondition(() -> !dialog.isVisible(), 5000, "ConnectionDialog to close after Save");

        // Verify connection appears in the JTree (poll for up to 5 seconds)
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found in MainFrame");

        final JTree finalTree = tree;
        waitForCondition(() -> getTreeNodeByText(finalTree, config.getConnectionName()) != null,
                5000, "Connection '" + config.getConnectionName() + "' to appear in tree");

        DefaultMutableTreeNode node = getTreeNodeByText(tree, config.getConnectionName());
        if (node == null) throw new AssertionError("Connection '" + config.getConnectionName() + "' not found in tree after save");
    }

    private void testConnect() {
        ensureRobot();
        System.out.println("  [CONNECT] Looking for connection in tree...");
        
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found in MainFrame");

        final JTree finalTree = tree;
        waitForCondition(() -> getTreeNodeByText(finalTree, config.getConnectionName()) != null,
                5000, "Connection node to appear in tree");

        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node '" + config.getConnectionName() + "' not found");

        System.out.println("  [CONNECT] Found connection node, right-clicking to connect...");

        // Right-click the connection node and select "Connect"
        rightClickTreeNode(tree, connNode);
        delay(1000);
        clickMenuItem(mainFrame, "Connect");
        delay(2000);

        // Dismiss any error dialogs
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isVisible()) {
                String title = d.getTitle() != null ? d.getTitle().toLowerCase() : "";
                if (title.contains("error") || title.contains("fail")) {
                    JButton okBtn = findButton(d, "OK");
                    if (okBtn != null) {
                        clickComponent(okBtn);
                        delay(500);
                    }
                    throw new AssertionError("Connection failed: " + title);
                }
            }
        }

        System.out.println("  [CONNECT] Waiting for schema nodes to load (may take a few seconds for remote databases)...");

        // Wait up to 15 seconds for child nodes to appear (schema nodes)
        waitForCondition(() -> {
            AtomicReference<Integer> childCount = new AtomicReference<>(0);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    DefaultMutableTreeNode node = getTreeNodeByTextDirect(tree, config.getConnectionName());
                    if (node != null) {
                        childCount.set(node.getChildCount());
                        if (childCount.get() == 1) {
                            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getFirstChild();
                            String childText = firstChild.getUserObject() != null ? firstChild.getUserObject().toString() : "";
                            if (childText.startsWith("Loading")) {
                                childCount.set(0);
                            }
                        }
                    }
                });
            } catch (Exception ignored) {}
            return childCount.get() > 0;
        }, 5000, "Schema child nodes to appear under connection");

        System.out.println("  [CONNECT] Database connected successfully.");
        delay(1000);
    }

    private void testBrowseSchema() {
        ensureRobot();
        System.out.println("  [BROWSE] Looking for connection and suppchain schema...");
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Find the connection node, expand it
        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node not found");
        expandTreeNode(tree, new TreePath(connNode.getPath()));
        delay(2000);

        // Look for "suppchain" schema; fall back to first schema
        DefaultMutableTreeNode schemaNode = getTreeNodeByText(tree, "suppchain");
        if (schemaNode == null) {
            System.out.println("  [BROWSE] 'suppchain' not found, trying first schema child");
            AtomicReference<DefaultMutableTreeNode> ref = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    DefaultMutableTreeNode cn = getTreeNodeByTextDirect(tree, config.getConnectionName());
                    if (cn != null && cn.getChildCount() > 0) {
                        ref.set((DefaultMutableTreeNode) cn.getFirstChild());
                    }
                });
            } catch (Exception ignored) {}
            schemaNode = ref.get();
        }
        if (schemaNode == null) throw new AssertionError("No schema node found under connection");

        System.out.println("  [BROWSE] Expanding schema: " + schemaNode.getUserObject());
        expandTreeNode(tree, new TreePath(schemaNode.getPath()));
        delay(2000);

        // Find and expand the "Tables" category node
        waitForCondition(() -> getTreeNodeByText(tree, "Tables") != null,
                10000, "Tables category node to appear");

        DefaultMutableTreeNode tablesNode = getTreeNodeByText(tree, "Tables");
        if (tablesNode == null) throw new AssertionError("Tables category node not found");
        expandTreeNode(tree, new TreePath(tablesNode.getPath()));
        delay(2000);

        // Verify at least one child under Tables
        waitForCondition(() -> {
            AtomicReference<Integer> count = new AtomicReference<>(0);
            try {
                SwingUtilities.invokeAndWait(() -> {
                    DefaultMutableTreeNode tn = getTreeNodeByText(tree, "Tables");
                    if (tn != null) {
                        int childCount = tn.getChildCount();
                        if (childCount == 1) {
                            DefaultMutableTreeNode fc = (DefaultMutableTreeNode) tn.getFirstChild();
                            String text = fc.getUserObject() != null ? fc.getUserObject().toString() : "";
                            if (text.startsWith("Loading") || text.startsWith("(empty)")) {
                                count.set(0);
                                return;
                            }
                        }
                        count.set(childCount);
                    }
                });
            } catch (Exception ignored) {}
            return count.get() > 0;
        }, 10000, "At least one table to appear under Tables");

        System.out.println("  [BROWSE] Schema browsing complete.");
    }

    private void testViewTableData() {
        ensureRobot();
        System.out.println("  [VIEW-DATA] Looking for Customer table in suppchain schema...");
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Find the Customer table node (should be visible after browse-schema expanded Tables)
        DefaultMutableTreeNode customerNode = getTreeNodeByText(tree, "Customer");
        if (customerNode == null) {
            // Try Customers (plural)
            customerNode = getTreeNodeByText(tree, "Customers");
        }
        if (customerNode == null) throw new AssertionError("Customer/Customers table node not found in tree");

        System.out.println("  [VIEW-DATA] Found table: " + customerNode.getUserObject());

        // Right-click the Customer table node
        rightClickTreeNode(tree, customerNode);
        delay(1000);

        // Click "View Data" from context menu
        clickMenuItem(mainFrame, "View Data");
        delay(2000);

        // Wait for results to appear (a new tab should open with SELECT * FROM ... and execute)
        waitForCondition(() -> {
            JTable table = findComponent(mainFrame, JTable.class, t -> t.isVisible() && t.getRowCount() > 0);
            return table != null;
        }, 15000, "Customer table data to load in results");

        System.out.println("  [VIEW-DATA] Customer table data loaded successfully.");
        delay(1000);
    }

    private void testOpenQueryTab() {
        ensureRobot();
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Right-click the connection node
        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node not found");
        rightClickTreeNode(tree, connNode);
        delay(500);

        // Wait for popup menu, click "Open Query Tab"
        JPopupMenu popup = findComponent(mainFrame, JPopupMenu.class, JPopupMenu::isVisible);
        if (popup == null) {
            // Try finding from all windows
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog || w instanceof JFrame) {
                    popup = findComponent((Container) w, JPopupMenu.class, JPopupMenu::isVisible);
                    if (popup != null) break;
                }
            }
        }
        // Click "Open Query Tab" menu item
        delay(300);
        clickMenuItem(popup != null ? popup : mainFrame, "Open Query Tab");
        delay(500);

        // Verify a tab appears in the SqlEditorPanel (find JTabbedPane)
        JTabbedPane tabbedPane = findComponent(mainFrame, JTabbedPane.class, tp -> tp.getTabCount() > 0);
        if (tabbedPane == null) throw new AssertionError("No JTabbedPane with tabs found after opening query tab");
    }

    private void testRunQuery() {
        ensureRobot();
        // Find the active editor (JTextComponent in the active tab)
        JTextComponent editor = findComponent(mainFrame, JTextComponent.class,
                tc -> tc.isVisible() && tc.isEditable() && !(tc instanceof JTextField) && !(tc instanceof JTextArea));
        if (editor == null) {
            // Fallback: try JTextArea
            editor = findComponent(mainFrame, JTextArea.class, ta -> ta.isVisible() && ta.isEditable());
        }
        if (editor == null) throw new AssertionError("No editable text editor found in active tab");

        // Click it to focus
        clickComponent(editor);
        delay(300);

        // Select all (Ctrl+A) then type the query
        pressCtrlKey(KeyEvent.VK_A);
        delay(100);

        String query = config.getQuery() != null && !config.getQuery().isEmpty() ? config.getQuery() : "SELECT 1";
        // Use clipboard to paste the query for reliability
        setClipboardText(query);
        pressCtrlKey(KeyEvent.VK_V);
        delay(300);

        // Press Ctrl+Enter to execute
        pressCtrlKey(KeyEvent.VK_ENTER);

        // Wait up to 30 seconds for results (find JTable in the result panel with row count > 0)
        waitForCondition(() -> {
            JTable table = findComponent(mainFrame, JTable.class, t -> t.isVisible() && t.getRowCount() > 0);
            return table != null;
        }, 30000, "Query results to appear with at least one row");
    }

    private void testVerifyResults() {
        // Find the JTable in the active tab's result panel
        JTable table = findComponent(mainFrame, JTable.class, t -> t.isVisible() && t.getRowCount() > 0);
        if (table == null) throw new AssertionError("No visible JTable with rows found");

        AtomicReference<Integer> rowCount = new AtomicReference<>(0);
        AtomicReference<Integer> colCount = new AtomicReference<>(0);
        try {
            SwingUtilities.invokeAndWait(() -> {
                rowCount.set(table.getRowCount());
                colCount.set(table.getColumnCount());
            });
        } catch (Exception ignored) {}

        if (rowCount.get() <= 0) throw new AssertionError("Result table has no rows");
        if (colCount.get() <= 0) throw new AssertionError("Result table has no columns");
    }

    private void testCreateSampleData() {
        ensureRobot();
        System.out.println("  [SAMPLE-DATA] Loading DDL script...");

        // Get DDL script - either from file or use embedded supply chain DDL
        String ddlScript;
        if (config.getDdlScriptPath() != null && !config.getDdlScriptPath().isEmpty()) {
            System.out.println("  [SAMPLE-DATA] Using DDL from file: " + config.getDdlScriptPath());
            try {
                ddlScript = Files.readString(Path.of(config.getDdlScriptPath()));
                System.out.println("  [SAMPLE-DATA] DDL file loaded, length: " + ddlScript.length() + " chars");
            } catch (IOException e) {
                throw new AssertionError("Failed to read DDL script file: " + config.getDdlScriptPath() + " - " + e.getMessage());
            }
        } else {
            System.out.println("  [SAMPLE-DATA] Using embedded supply chain DDL");
            ddlScript = SUPPLY_CHAIN_DDL;
        }

        // Find the tree and connection node
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Debug: print tree nodes to help diagnose matching issues
        try {
            SwingUtilities.invokeAndWait(() -> {
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                System.out.println("  [SAMPLE-DATA] Tree has " + root.getChildCount() + " connection(s):");
                for (int i = 0; i < root.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
                    System.out.println("  [SAMPLE-DATA]   '" + child.getUserObject() + "'");
                }
            });
        } catch (Exception ignored) {}

        // Wait for connection node (may need time after earlier tests)
        final JTree ft = tree;
        waitForCondition(() -> getTreeNodeByText(ft, config.getConnectionName()) != null,
                5000, "Connection node '" + config.getConnectionName() + "' to appear");

        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node not found: '" + config.getConnectionName() + "'");
        rightClickTreeNode(tree, connNode);
        delay(1000);
        clickMenuItem(mainFrame, "Open Query Tab");
        delay(1500);

        // Split DDL into individual statements and execute each one
        String[] statements = ddlScript.split(";");
        int stmtCount = 0;
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;
            stmtCount++;

            System.out.println("  [SAMPLE-DATA] Executing statement " + stmtCount + ": " + trimmed.substring(0, Math.min(60, trimmed.length())) + "...");

            // Find the active editor
            JTextComponent editor = findComponent(mainFrame, JTextComponent.class,
                    tc -> tc.isVisible() && tc.isEditable() && !(tc instanceof JTextField) && !(tc instanceof JTextArea));
            if (editor == null) {
                editor = findComponent(mainFrame, JTextArea.class, ta -> ta.isVisible() && ta.isEditable());
            }
            if (editor == null) throw new AssertionError("No editable text editor found for statement " + stmtCount);

            // Clear editor and paste this statement
            clickComponent(editor);
            delay(300);
            pressCtrlKey(KeyEvent.VK_A);
            delay(200);
            setClipboardText(trimmed);
            pressCtrlKey(KeyEvent.VK_V);
            delay(500);

            // Execute with Ctrl+Enter
            pressCtrlKey(KeyEvent.VK_ENTER);
            delay(3000); // Wait for each statement to complete

            // Dismiss any error dialogs
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isVisible()) {
                    String title = d.getTitle() != null ? d.getTitle().toLowerCase() : "";
                    if (title.contains("error")) {
                        JButton okBtn = findButton(d, "OK");
                        if (okBtn != null) clickComponent(okBtn);
                        delay(500);
                        System.out.println("  [SAMPLE-DATA] Warning: statement " + stmtCount + " may have failed");
                        break;
                    }
                }
            }
        }

        System.out.println("  [SAMPLE-DATA] Executed " + stmtCount + " statements. Refreshing schema tree...");

        // Refresh the schema tree to pick up new objects
        connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode != null) {
            rightClickTreeNode(tree, connNode);
            delay(1000);
            clickMenuItem(mainFrame, "Refresh Schemas");
            delay(3000);
        }

        System.out.println("  [SAMPLE-DATA] DDL execution complete.");
    }

    private void testExportDdl() {
        ensureRobot();
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Find the connection node and expand it
        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node not found");
        expandTreeNode(tree, new TreePath(connNode.getPath()));
        delay(2000);

        // Look for the "shipping" schema node; fall back to first schema
        DefaultMutableTreeNode schemaNode = getTreeNodeByText(tree, "shipping");
        if (schemaNode == null) {
            System.out.println("  [EXPORT-DDL] 'shipping' schema not found, using first schema");
            AtomicReference<DefaultMutableTreeNode> ref = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    DefaultMutableTreeNode cn = getTreeNodeByTextDirect(tree, config.getConnectionName());
                    if (cn != null && cn.getChildCount() > 0) {
                        ref.set((DefaultMutableTreeNode) cn.getFirstChild());
                    }
                });
            } catch (Exception ignored) {}
            schemaNode = ref.get();
        }
        if (schemaNode == null) throw new AssertionError("No schema node found");

        System.out.println("  [EXPORT-DDL] Exporting DDL for schema: " + schemaNode.getUserObject());

        // Right-click it, click "Export DDL"
        rightClickTreeNode(tree, schemaNode);
        delay(1000);
        clickMenuItem(mainFrame, "Export DDL");

        // Wait for DdlExportDialog
        JDialog ddlDialog = findDialog("DDL", 10000);
        if (ddlDialog == null) throw new AssertionError("DDL Export dialog did not appear");

        // Wait for DDL to generate
        delay(3000);

        waitForCondition(() -> {
            JTextArea textArea = findComponent(ddlDialog, JTextArea.class, ta -> true);
            if (textArea == null) return false;
            AtomicReference<String> text = new AtomicReference<>("");
            try { SwingUtilities.invokeAndWait(() -> text.set(textArea.getText())); } catch (Exception ignored) {}
            return text.get().contains("CREATE");
        }, 10000, "DDL text to contain CREATE");

        // Click "Copy" button
        JButton copyBtn = findButton(ddlDialog, "Copy");
        if (copyBtn == null) copyBtn = findButton(ddlDialog, "Copy DDL to clipboard");
        if (copyBtn != null) {
            clickComponent(copyBtn);
            delay(500);
        }

        // Close the dialog
        try { SwingUtilities.invokeAndWait(() -> ddlDialog.dispose()); } catch (Exception ignored) {}
        delay(500);
    }

    private void testViewSchemaDiagram() {
        ensureRobot();
        System.out.println("  [SCHEMA-DIAGRAM] Looking for suppchain schema...");
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Find the suppchain schema node (should already be expanded from browse-schema)
        DefaultMutableTreeNode schemaNode = getTreeNodeByText(tree, "suppchain");
        if (schemaNode == null) {
            // Fall back: expand connection and try again
            DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
            if (connNode != null) {
                expandTreeNode(tree, new TreePath(connNode.getPath()));
                delay(2000);
                schemaNode = getTreeNodeByText(tree, "suppchain");
            }
        }
        if (schemaNode == null) throw new AssertionError("'suppchain' schema node not found");

        System.out.println("  [SCHEMA-DIAGRAM] Opening diagram for: " + schemaNode.getUserObject());

        // Right-click suppchain schema, click "Schema Diagram"
        rightClickTreeNode(tree, schemaNode);
        delay(1000);
        clickMenuItem(mainFrame, "Schema Diagram");

        // Wait for SchemaDiagramDialog
        JDialog diagramDialog = findDialog("Schema Diagram", 10000);
        if (diagramDialog == null) throw new AssertionError("Schema Diagram dialog did not appear");

        // Click "Fit" button
        delay(2000);
        JButton fitBtn = findButton(diagramDialog, "Fit");
        if (fitBtn == null) {
            fitBtn = findComponent(diagramDialog, JButton.class,
                    b -> b.getText() != null && b.getText().contains("Fit"));
        }
        if (fitBtn != null) {
            clickComponent(fitBtn);
        }

        delay(3000);

        // Close the dialog
        try { SwingUtilities.invokeAndWait(() -> diagramDialog.dispose()); } catch (Exception ignored) {}
        delay(500);
    }

    private void testConfigureAI() {
        if (shouldSkipAI(config)) {
            aiConfigSkipped = true;
            throw new SkipException("AI config not provided (aiProvider or aiApiKey missing)");
        }

        ensureRobot();

        // Step 1: Open Settings menu -> "AI Configuration..."
        JMenuBar menuBar = mainFrame.getJMenuBar();
        if (menuBar == null) throw new AssertionError("Menu bar not found");
        clickMenuBarItem(menuBar, "Settings", "AI Configuration...");
        delay(1000);

        // Step 2: Wait for AIConfigDialog
        JDialog aiDialog = findDialog("AI", 8000);
        if (aiDialog == null) throw new AssertionError("AI Configuration dialog did not appear");
        delay(1000);

        // Step 3: Click "Add New" button
        clickButton(aiDialog, "Add New");
        delay(1000);

        // Step 4: Fill config name
        List<JTextField> textFields = new ArrayList<>();
        List<JPasswordField> passwordFields = new ArrayList<>();
        findAllComponents(aiDialog, JTextField.class, tf -> !(tf instanceof JPasswordField) && tf.isVisible(), textFields);
        findAllComponents(aiDialog, JPasswordField.class, pf -> pf.isVisible(), passwordFields);

        if (textFields.size() >= 1) {
            clearAndType(textFields.get(0), config.getAiProvider() + " Config");
        }
        delay(500);

        // Step 5: Select provider from combo
        JComboBox<?> providerCombo = findComponent(aiDialog, JComboBox.class,
                cb -> cb.isVisible() && cb.getItemCount() > 1);
        if (providerCombo != null && config.getAiProvider() != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (int i = 0; i < providerCombo.getItemCount(); i++) {
                        if (providerCombo.getItemAt(i).toString().equalsIgnoreCase(config.getAiProvider())) {
                            providerCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                });
            } catch (Exception ignored) {}
            delay(1000); // Wait for provider change to update fields
        }

        // Step 6: Re-find fields after provider change
        textFields.clear();
        passwordFields.clear();
        findAllComponents(aiDialog, JTextField.class, tf -> !(tf instanceof JPasswordField) && tf.isVisible(), textFields);
        findAllComponents(aiDialog, JPasswordField.class, pf -> pf.isVisible(), passwordFields);
        delay(500);

        // Step 7: Fill model field
        if (textFields.size() >= 2 && config.getAiModel() != null) {
            clearAndType(textFields.get(1), config.getAiModel());
        }
        delay(500);

        // Step 8: Fill base URL field
        if (textFields.size() >= 3 && config.getAiBaseUrl() != null) {
            clearAndType(textFields.get(2), config.getAiBaseUrl());
        }
        delay(500);

        // Step 9: Fill API key
        if (passwordFields.size() >= 1 && config.getAiApiKey() != null) {
            clearAndType(passwordFields.get(0), config.getAiApiKey());
        }
        delay(1000);

        // Step 10: Test the AI connection before saving
        JButton testBtn = findButton(aiDialog, "Test Connection");
        if (testBtn == null) testBtn = findButton(aiDialog, "Test");
        if (testBtn != null) {
            // Count dialogs before clicking test
            final int dialogCountBefore = countVisibleDialogs();
            clickComponent(testBtn);
            delay(1000);

            // Wait for a NEW dialog to appear (not the AI config dialog itself)
            waitForCondition(() -> countVisibleDialogs() > dialogCountBefore, 20000,
                    "AI test connection result dialog to appear");
            delay(500);

            // Find and dismiss the new dialog
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isVisible() && d != aiDialog) {
                    JButton okBtn = findButton(d, "OK");
                    if (okBtn != null) {
                        clickComponent(okBtn);
                        delay(500);
                        break;
                    }
                }
            }
        }
        delay(1000);

        // Step 11: Click "Save"
        clickButton(aiDialog, "Save");
        delay(1000);

        // Step 12: Dismiss any confirmation popup
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isVisible() && d != aiDialog) {
                JButton okBtn = findButton(d, "OK");
                if (okBtn != null) {
                    clickComponent(okBtn);
                    delay(500);
                    break;
                }
            }
        }
        delay(500);

        // Step 13: Close the AI config dialog
        if (aiDialog.isVisible()) {
            JButton closeBtn = findButton(aiDialog, "Close");
            if (closeBtn != null) {
                clickComponent(closeBtn);
            } else {
                try { SwingUtilities.invokeAndWait(() -> aiDialog.dispose()); } catch (Exception ignored) {}
            }
        }
        delay(1000);
    }

    /** Counts currently visible JDialog instances. */
    private int countVisibleDialogs() {
        int count = 0;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog && w.isVisible()) count++;
        }
        return count;
    }

    private void testAIAssistant() {
        if (aiConfigSkipped) {
            throw new SkipException("AI configuration was skipped");
        }

        ensureRobot();

        // Step 1: Find the connection in the tree
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found in MainFrame");

        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection '" + config.getConnectionName() + "' not found in tree");

        // Step 2: Select the connection node (single click to highlight it)
        TreePath connPath = new TreePath(connNode.getPath());
        try {
            SwingUtilities.invokeAndWait(() -> {
                tree.setSelectionPath(connPath);
                tree.scrollPathToVisible(connPath);
            });
        } catch (Exception ignored) {}
        delay(1000);

        // Step 3: Check if connected (EDT-safe child count check)
        final AtomicReference<Integer> childCount = new AtomicReference<>(0);
        try {
            SwingUtilities.invokeAndWait(() -> childCount.set(connNode.getChildCount()));
        } catch (Exception ignored) {}

        if (childCount.get() == 0) {
            // Step 4: Not connected — right-click and select Connect
            System.out.println("  [AI-ASSISTANT] Database not connected, connecting...");
            rightClickTreeNode(tree, connNode);
            delay(1000);
            clickMenuItem(mainFrame, "Connect");
            delay(2000);

            // Dismiss any error/confirmation dialogs
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isVisible()) {
                    String title = d.getTitle() != null ? d.getTitle() : "";
                    JButton okBtn = findButton(d, "OK");
                    if (okBtn != null && !title.contains("DB Explorer")) {
                        if (title.toLowerCase().contains("error") || title.toLowerCase().contains("fail")) {
                            clickComponent(okBtn);
                            delay(500);
                            throw new AssertionError("Database connection failed for AI Assistant test");
                        }
                        // Dismiss any other popup (e.g. confirmation)
                        clickComponent(okBtn);
                        delay(500);
                    }
                }
            }

            // Wait for schema nodes to load
            waitForCondition(() -> {
                AtomicReference<Integer> cc = new AtomicReference<>(0);
                try { SwingUtilities.invokeAndWait(() -> cc.set(connNode.getChildCount())); } catch (Exception ignored2) {}
                return cc.get() > 0;
            }, 15000, "Database to connect for AI Assistant");
            delay(1000);

            // Re-select the connection after connecting
            try {
                SwingUtilities.invokeAndWait(() -> {
                    tree.setSelectionPath(connPath);
                    tree.scrollPathToVisible(connPath);
                });
            } catch (Exception ignored) {}
            delay(1000);
        }

        System.out.println("  [AI-ASSISTANT] Database connected, clicking AI button...");

        // Step 5: Click the AI toolbar button
        clickButton(mainFrame, "AI SQL Assistant");
        delay(1500);

        // Step 6: Check for and dismiss any warning popup
        boolean hadWarning = dismissNonAIDialogs();
        
        if (hadWarning) {
            delay(1000);
            // Re-select connection and try again
            try {
                SwingUtilities.invokeAndWait(() -> {
                    tree.setSelectionPath(connPath);
                    tree.scrollPathToVisible(connPath);
                });
            } catch (Exception ignored) {}
            delay(1000);

            clickButton(mainFrame, "AI SQL Assistant");
            delay(1500);

            // Dismiss any further warnings
            dismissNonAIDialogs();
            delay(500);
        }

        // Step 7: Wait for AI Assistant dialog
        JDialog aiDialog = findDialog("AI", 8000);
        if (aiDialog == null) aiDialog = findDialog("SQL Generator", 5000);
        if (aiDialog == null) throw new AssertionError("AI Assistant dialog did not appear");
        delay(1000);

        // Step 8: Type the prompt
        JTextArea inputArea = findComponent(aiDialog, JTextArea.class, ta -> ta.isVisible() && ta.isEditable());
        if (inputArea == null) throw new AssertionError("AI input text area not found");

        clickComponent(inputArea);
        delay(500);
        pressCtrlKey(KeyEvent.VK_A);
        delay(200);
        String prompt = config.getAiPrompt() != null && !config.getAiPrompt().isEmpty()
                ? config.getAiPrompt() : "Show all tables in the database";
        setClipboardText(prompt);
        pressCtrlKey(KeyEvent.VK_V);
        delay(500);

        // Step 9: Click "Generate SQL"
        clickButton(aiDialog, "Generate SQL");
        delay(1000);

        // Step 10: Wait for output
        final JDialog finalAiDialog = aiDialog;
        waitForCondition(() -> {
            List<JTextArea> textAreas = new ArrayList<>();
            findAllComponents(finalAiDialog, JTextArea.class, ta -> ta.isVisible(), textAreas);
            if (textAreas.size() >= 2) {
                AtomicReference<String> text = new AtomicReference<>("");
                try { SwingUtilities.invokeAndWait(() -> text.set(textAreas.get(1).getText())); } catch (Exception ignored) {}
                return !text.get().isEmpty() && !text.get().startsWith("Error");
            }
            return false;
        }, 30000, "AI-generated SQL to appear in output area");
        delay(500);

        // Step 11: Close the dialog
        try { SwingUtilities.invokeAndWait(() -> finalAiDialog.dispose()); } catch (Exception ignored) {}
        delay(1000);
    }

    /** Dismisses any visible JDialog that is NOT the AI assistant dialog. Returns true if a dialog was dismissed. */
    private boolean dismissNonAIDialogs() {
        boolean dismissed = false;
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isVisible()) {
                String title = d.getTitle() != null ? d.getTitle().toLowerCase() : "";
                // Skip the AI assistant dialog itself
                if (title.contains("ai sql") || title.contains("sql generator")) continue;
                // Skip the main frame
                if (title.equals("db explorer")) continue;
                
                JButton okBtn = findButton(d, "OK");
                if (okBtn != null) {
                    clickComponent(okBtn);
                    delay(500);
                    dismissed = true;
                    break;
                }
            }
        }
        return dismissed;
    }

    private void testDisconnect() {
        ensureRobot();
        // Click "Disconnect DB" toolbar button
        clickButton(mainFrame, "Disconnect DB");
        delay(500);

        // Wait for confirmation dialog, click "Yes"
        JDialog confirmDialog = findDialog("Confirm", 5000);
        if (confirmDialog == null) {
            confirmDialog = findDialog("Disconnect", 3000);
        }
        if (confirmDialog != null) {
            clickButton(confirmDialog, "Yes");
            delay(500);
            // Dismiss the "Disconnected" info dialog if it appears
            JDialog infoDialog = findDialog("Disconnected", 3000);
            if (infoDialog != null) {
                JButton okBtn = findButton(infoDialog, "OK");
                if (okBtn != null) clickComponent(okBtn);
                delay(300);
            }
        }

        // Verify status bar text changed (just check no exception)
        delay(500);
    }

    private void testAboutDialog() {
        ensureRobot();
        // Click "About" toolbar button
        clickButton(mainFrame, "About");
        delay(500);

        // Wait for AboutDialog (title "About")
        JDialog aboutDialog = findDialog("About", 5000);
        if (aboutDialog == null) throw new AssertionError("About dialog did not appear");

        // Verify "Version" text exists (find JLabel containing "Version")
        JLabel versionLabel = findComponent(aboutDialog, JLabel.class,
                lbl -> lbl.getText() != null && lbl.getText().contains("Version"));
        if (versionLabel == null) throw new AssertionError("Version label not found in About dialog");

        // Verify "OK" button exists
        JButton okBtn = findButton(aboutDialog, "OK");
        if (okBtn == null) throw new AssertionError("OK button not found in About dialog");

        // Verify "Check for Updates" button exists
        JButton updatesBtn = findComponent(aboutDialog, JButton.class,
                b -> b.getText() != null && b.getText().contains("Check for Updates"));
        if (updatesBtn == null) throw new AssertionError("Check for Updates button not found in About dialog");

        // Click "OK"
        clickComponent(okBtn);
        delay(300);
    }

    private void testHelpDialog() {
        ensureRobot();
        // Open Help menu -> "User Guide"
        JMenuBar menuBar = mainFrame.getJMenuBar();
        if (menuBar == null) throw new AssertionError("Menu bar not found");
        clickMenuBarItem(menuBar, "Help", "User Guide");
        delay(500);

        // Wait for HelpDialog (title "Help")
        JDialog helpDialog = findDialog("Help", 5000);
        if (helpDialog == null) throw new AssertionError("Help dialog did not appear");

        // Find JTabbedPane, verify 6 tabs
        JTabbedPane tabs = findComponent(helpDialog, JTabbedPane.class, tp -> true);
        if (tabs == null) throw new AssertionError("JTabbedPane not found in Help dialog");

        AtomicReference<Integer> tabCount = new AtomicReference<>(0);
        try { SwingUtilities.invokeAndWait(() -> tabCount.set(tabs.getTabCount())); } catch (Exception ignored) {}
        if (tabCount.get() != 6) throw new AssertionError("Expected 6 tabs in Help dialog, found " + tabCount.get());

        // Click each tab
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            try { SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(idx)); } catch (Exception ignored) {}
            delay(300);
        }

        // Click "Close"
        clickButton(helpDialog, "Close");
        delay(300);
    }

    private void testSwitchTheme() {
        ensureRobot();
        // Find JComboBox in toolbar (the theme combo)
        JComboBox<?> themeCombo = findComponent(mainFrame, JComboBox.class,
                cb -> cb.isVisible() && cb.getToolTipText() != null && cb.getToolTipText().contains("Theme"));
        if (themeCombo == null) {
            // Fallback: find any combo with theme names
            themeCombo = findComponent(mainFrame, JComboBox.class, cb -> {
                if (!cb.isVisible() || cb.getItemCount() < 3) return false;
                for (int i = 0; i < cb.getItemCount(); i++) {
                    if ("Flat Light".equals(cb.getItemAt(i).toString())) return true;
                }
                return false;
            });
        }
        if (themeCombo == null) throw new AssertionError("Theme combo not found in toolbar");

        // Record current selection and LookAndFeel
        final JComboBox<?> combo = themeCombo;
        AtomicReference<String> originalTheme = new AtomicReference<>();
        AtomicReference<String> originalLaf = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                originalTheme.set(combo.getSelectedItem() != null ? combo.getSelectedItem().toString() : "");
                originalLaf.set(UIManager.getLookAndFeel().getClass().getName());
            });
        } catch (Exception ignored) {}

        // Select "Flat Light" (or a different theme)
        String targetTheme = "Flat Light".equals(originalTheme.get()) ? "Flat Dark" : "Flat Light";
        try {
            SwingUtilities.invokeAndWait(() -> combo.setSelectedItem(targetTheme));
        } catch (Exception ignored) {}
        delay(2000);

        // Verify UIManager.getLookAndFeel() changed
        AtomicReference<String> newLaf = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> newLaf.set(UIManager.getLookAndFeel().getClass().getName()));
        } catch (Exception ignored) {}
        if (originalLaf.get().equals(newLaf.get())) {
            throw new AssertionError("LookAndFeel did not change after theme switch");
        }

        // Restore original theme
        try {
            SwingUtilities.invokeAndWait(() -> combo.setSelectedItem(originalTheme.get()));
        } catch (Exception ignored) {}
        delay(1000);
    }

    private void testCheckUpdates() {
        ensureRobot();
        // Open Help menu -> "Check for Updates\u2026" (note the Unicode ellipsis)
        JMenuBar menuBar = mainFrame.getJMenuBar();
        if (menuBar == null) throw new AssertionError("Menu bar not found");
        clickMenuBarItem(menuBar, "Help", "Check for Updates\u2026");
        delay(500);

        // Wait for UpdateDialog (title contains "Update")
        JDialog updateDialog = findDialog("Update", 5000);
        if (updateDialog == null) throw new AssertionError("Update dialog did not appear");

        delay(2000);

        // Click "Close"
        clickButton(updateDialog, "Close");
        delay(300);
    }

    private void testClearConsole() {
        ensureRobot();
        // Click "Clear Console" toolbar button
        clickButton(mainFrame, "Clear Console");
        delay(1000);

        // Find the LogPanel's JTextArea, verify it's empty
        JTextArea logArea = findComponent(mainFrame, JTextArea.class,
                ta -> !ta.isEditable());
        if (logArea != null) {
            waitForCondition(() -> {
                AtomicReference<String> text = new AtomicReference<>("");
                try { SwingUtilities.invokeAndWait(() -> text.set(logArea.getText())); } catch (Exception ignored) {}
                return text.get().isEmpty();
            }, 2000, "Console text area to be empty");
        }
    }

    private void testDeleteConnection() {
        ensureRobot();
        JTree tree = findComponent(mainFrame, JTree.class, t -> true);
        if (tree == null) throw new AssertionError("JTree not found");

        // Find the connection node in the tree
        DefaultMutableTreeNode connNode = getTreeNodeByText(tree, config.getConnectionName());
        if (connNode == null) throw new AssertionError("Connection node '" + config.getConnectionName() + "' not found for deletion");

        // Right-click it, click "Delete"
        rightClickTreeNode(tree, connNode);
        delay(500);
        clickMenuItem(mainFrame, "Delete");
        delay(500);

        // Wait for confirmation dialog, click "Yes"
        JDialog confirmDialog = findDialog("Confirm", 5000);
        if (confirmDialog == null) {
            confirmDialog = findDialog("Delete", 3000);
        }
        if (confirmDialog != null) {
            clickButton(confirmDialog, "Yes");
            delay(500);
        }

        // Verify the node is gone from the tree
        delay(500);
        DefaultMutableTreeNode deletedNode = getTreeNodeByText(tree, config.getConnectionName());
        if (deletedNode != null) throw new AssertionError("Connection '" + config.getConnectionName() + "' still exists after deletion");
    }

    // =========================================================================
    // SkipException for AI test skip handling
    // =========================================================================

    private static class SkipException extends RuntimeException {
        private final String reason;
        SkipException(String reason) {
            super(reason);
            this.reason = reason;
        }
        String getReason() { return reason; }
    }

    // =========================================================================
    // Supply Chain DDL
    // =========================================================================

    private static final String SUPPLY_CHAIN_DDL =
        "DROP TABLE IF EXISTS shipments;\n" +
        "DROP TABLE IF EXISTS order_items;\n" +
        "DROP TABLE IF EXISTS purchase_orders;\n" +
        "DROP TABLE IF EXISTS inventory;\n" +
        "DROP TABLE IF EXISTS products;\n" +
        "DROP TABLE IF EXISTS warehouses;\n" +
        "DROP TABLE IF EXISTS suppliers;\n" +
        "\n" +
        "CREATE TABLE suppliers (\n" +
        "    supplier_id SERIAL PRIMARY KEY,\n" +
        "    name VARCHAR(100) NOT NULL,\n" +
        "    contact_email VARCHAR(150),\n" +
        "    country VARCHAR(60),\n" +
        "    rating DECIMAL(3,2)\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE warehouses (\n" +
        "    warehouse_id SERIAL PRIMARY KEY,\n" +
        "    name VARCHAR(100) NOT NULL,\n" +
        "    location VARCHAR(200),\n" +
        "    capacity INT\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE products (\n" +
        "    product_id SERIAL PRIMARY KEY,\n" +
        "    name VARCHAR(100) NOT NULL,\n" +
        "    sku VARCHAR(50) UNIQUE,\n" +
        "    category VARCHAR(60),\n" +
        "    unit_price DECIMAL(10,2),\n" +
        "    supplier_id INT REFERENCES suppliers(supplier_id)\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE inventory (\n" +
        "    inventory_id SERIAL PRIMARY KEY,\n" +
        "    product_id INT REFERENCES products(product_id),\n" +
        "    warehouse_id INT REFERENCES warehouses(warehouse_id),\n" +
        "    quantity INT,\n" +
        "    last_restocked DATE\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE purchase_orders (\n" +
        "    order_id SERIAL PRIMARY KEY,\n" +
        "    supplier_id INT REFERENCES suppliers(supplier_id),\n" +
        "    order_date DATE,\n" +
        "    status VARCHAR(30),\n" +
        "    total_amount DECIMAL(12,2)\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE order_items (\n" +
        "    item_id SERIAL PRIMARY KEY,\n" +
        "    order_id INT REFERENCES purchase_orders(order_id),\n" +
        "    product_id INT REFERENCES products(product_id),\n" +
        "    quantity INT,\n" +
        "    unit_price DECIMAL(10,2)\n" +
        ");\n" +
        "\n" +
        "CREATE TABLE shipments (\n" +
        "    shipment_id SERIAL PRIMARY KEY,\n" +
        "    order_id INT REFERENCES purchase_orders(order_id),\n" +
        "    warehouse_id INT REFERENCES warehouses(warehouse_id),\n" +
        "    ship_date DATE,\n" +
        "    arrival_date DATE,\n" +
        "    tracking_number VARCHAR(100),\n" +
        "    status VARCHAR(30)\n" +
        ");\n" +
        "\n" +
        "INSERT INTO suppliers (name, contact_email, country, rating) VALUES\n" +
        "    ('Acme Corp', 'acme@example.com', 'USA', 4.50),\n" +
        "    ('Global Parts', 'info@globalparts.com', 'Germany', 4.20),\n" +
        "    ('Eastern Supply', 'east@supply.com', 'Japan', 4.80);\n" +
        "\n" +
        "INSERT INTO warehouses (name, location, capacity) VALUES\n" +
        "    ('Main Warehouse', 'New York, NY', 10000),\n" +
        "    ('West Coast Hub', 'Los Angeles, CA', 8000),\n" +
        "    ('Central Depot', 'Chicago, IL', 6000);\n" +
        "\n" +
        "INSERT INTO products (name, sku, category, unit_price, supplier_id) VALUES\n" +
        "    ('Widget A', 'WGT-001', 'Widgets', 9.99, 1),\n" +
        "    ('Gadget B', 'GDG-002', 'Gadgets', 24.50, 2),\n" +
        "    ('Component C', 'CMP-003', 'Components', 5.75, 3);\n" +
        "\n" +
        "INSERT INTO inventory (product_id, warehouse_id, quantity, last_restocked) VALUES\n" +
        "    (1, 1, 500, '2024-01-15'),\n" +
        "    (2, 2, 300, '2024-02-10'),\n" +
        "    (3, 3, 1000, '2024-03-01');\n" +
        "\n" +
        "INSERT INTO purchase_orders (supplier_id, order_date, status, total_amount) VALUES\n" +
        "    (1, '2024-03-01', 'DELIVERED', 999.00),\n" +
        "    (2, '2024-03-15', 'SHIPPED', 2450.00),\n" +
        "    (3, '2024-04-01', 'PENDING', 575.00);\n" +
        "\n" +
        "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES\n" +
        "    (1, 1, 100, 9.99),\n" +
        "    (2, 2, 100, 24.50),\n" +
        "    (3, 3, 100, 5.75);\n" +
        "\n" +
        "INSERT INTO shipments (order_id, warehouse_id, ship_date, arrival_date, tracking_number, status) VALUES\n" +
        "    (1, 1, '2024-03-02', '2024-03-05', 'TRK-10001', 'DELIVERED'),\n" +
        "    (2, 2, '2024-03-16', NULL, 'TRK-10002', 'IN_TRANSIT'),\n" +
        "    (3, 3, NULL, NULL, NULL, 'PENDING');\n";

    // =========================================================================
    // Additional Helper Methods
    // =========================================================================

    /**
     * Find a tree node by text directly on the EDT (for use inside invokeAndWait blocks).
     */
    private DefaultMutableTreeNode getTreeNodeByTextDirect(JTree tree, String text) {
        TreeModel model = tree.getModel();
        Object root = model.getRoot();
        if (root instanceof DefaultMutableTreeNode rootNode) {
            return searchTreeNode(rootNode, text);
        }
        return null;
    }

    /**
     * Find all components of a given type matching a predicate, collecting into a list.
     */
    @SuppressWarnings("unchecked")
    private <T extends Component> void findAllComponents(Container container, Class<T> type,
                                                          Predicate<T> predicate, List<T> results) {
        try {
            SwingUtilities.invokeAndWait(() -> findAllComponentsRecursive(container, type, predicate, results));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            // ignore
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> void findAllComponentsRecursive(Container container, Class<T> type,
                                                                    Predicate<T> predicate, List<T> results) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) {
                T candidate = (T) child;
                if (predicate.test(candidate)) {
                    results.add(candidate);
                }
            }
            if (child instanceof Container childContainer) {
                findAllComponentsRecursive(childContainer, type, predicate, results);
            }
        }
    }

    /**
     * Clear a text field and type new text into it.
     */
    private void clearAndType(JTextComponent field, String text) {
        clickComponent(field);
        delay(100);
        pressCtrlKey(KeyEvent.VK_A);
        delay(50);
        pressKey(KeyEvent.VK_DELETE);
        delay(50);
        // Use clipboard for reliability
        setClipboardText(text);
        pressCtrlKey(KeyEvent.VK_V);
        delay(100);
    }

    /**
     * Set the system clipboard content.
     */
    private void setClipboardText(String text) {
        java.awt.datatransfer.StringSelection selection =
                new java.awt.datatransfer.StringSelection(text);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    // =========================================================================
    // Test Runner
    // =========================================================================

    /**
     * A named test case entry pairing a name with a Runnable.
     */
    private record TestCaseEntry(String name, Runnable action) {}

    /**
     * Build the ordered list of 20 test cases.
     */
    private List<TestCaseEntry> buildTestCases() {
        return List.of(
            new TestCaseEntry("launch", this::testLaunch),
            new TestCaseEntry("test-and-save-connection", this::testTestConnection),
            new TestCaseEntry("connect", this::testConnect),
            new TestCaseEntry("browse-schema", this::testBrowseSchema),
            new TestCaseEntry("view-table-data", this::testViewTableData),
            new TestCaseEntry("view-schema-diagram", this::testViewSchemaDiagram),
            new TestCaseEntry("configure-ai", this::testConfigureAI),
            new TestCaseEntry("ai-assistant", this::testAIAssistant),
            new TestCaseEntry("disconnect", this::testDisconnect),
            new TestCaseEntry("about-dialog", this::testAboutDialog),
            new TestCaseEntry("help-dialog", this::testHelpDialog),
            new TestCaseEntry("switch-theme", this::testSwitchTheme),
            new TestCaseEntry("check-updates", this::testCheckUpdates),
            new TestCaseEntry("clear-console", this::testClearConsole),
            new TestCaseEntry("delete-connection", this::testDeleteConnection)
        );
    }

    /**
     * Run all test cases sequentially with error isolation.
     */
    private void runAllTests() {
        List<TestCaseEntry> testCases = buildTestCases();
        for (TestCaseEntry testCase : testCases) {
            long start = System.currentTimeMillis();
            try {
                testCase.action().run();
                reporter.reportPass(testCase.name(), System.currentTimeMillis() - start);
            } catch (SkipException e) {
                reporter.reportSkip(testCase.name(), e.getReason());
            } catch (Throwable e) {
                reporter.reportFail(testCase.name(), System.currentTimeMillis() - start,
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // Main Entry Point
    // =========================================================================

    public static void main(String[] args) {
        // Parse command-line arg for config file path
        if (args.length < 1) {
            System.err.println("Usage: UITestRobot <config-file-path>");
            System.exit(1);
        }

        String configPath = args[0];
        if (!Files.exists(Path.of(configPath))) {
            System.err.println("ERROR: Config file not found: " + configPath);
            System.exit(1);
        }

        // Parse TestConfig from JSON
        TestConfig config;
        try {
            config = parseConfig(configPath);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return; // unreachable, but satisfies compiler
        }

        System.out.println("Loaded test configuration:");
        System.out.println(config);

        // Create UITestRobot instance and set up
        UITestRobot robot = new UITestRobot();
        robot.config = config;
        robot.reporter = new TestReporter();

        // Run all test cases
        robot.runAllTests();

        // Print summary
        robot.reporter.printSummary();

        // Dispose MainFrame if it was created
        if (robot.mainFrame != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    robot.mainFrame.dispose();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                System.err.println("Warning: Failed to dispose MainFrame: " + e.getMessage());
            }
        }

        // Exit with appropriate code
        System.exit(robot.reporter.getExitCode());
    }
}
