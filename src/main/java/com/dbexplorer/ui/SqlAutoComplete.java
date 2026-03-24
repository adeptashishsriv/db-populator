package com.dbexplorer.ui;

import com.dbexplorer.model.ConnectionInfo;
import com.dbexplorer.service.SchemaExplorerService;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

// Disambiguate: we want the Swing timer, not java.util.Timer

/**
 * Non-blocking SQL autocomplete for a JTextPane.
 *
 * Design goals:
 *  - Zero performance impact: schema metadata is fetched ONCE on a background
 *    thread and cached per connection. Subsequent keystrokes only filter an
 *    in-memory list — no DB calls.
 *  - Debounced: popup only appears 250 ms after the user stops typing.
 *  - Ctrl+Space forces the popup open immediately.
 *  - Tab / Enter accepts the selected suggestion.
 *  - Escape / focus-loss hides the popup.
 */
public class SqlAutoComplete {

    // ── SQL keywords included in suggestions ─────────────────────────────────
    private static final List<String> SQL_KEYWORDS = List.of(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE",
            "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "FULL", "CROSS", "ON",
            "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL",
            "AS", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", "UNION",
            "ALL", "DISTINCT", "CASE", "WHEN", "THEN", "ELSE", "END", "ASC", "DESC",
            "COUNT", "SUM", "AVG", "MIN", "MAX", "CAST", "COALESCE", "WITH",
            "RETURNING", "OVER", "PARTITION", "WINDOW", "RANK", "ROW_NUMBER",
            "DENSE_RANK", "LAG", "LEAD", "BEGIN", "COMMIT", "ROLLBACK",
            "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "DEFAULT",
            "UNIQUE", "AUTO_INCREMENT", "IDENTITY", "SEQUENCE", "NEXTVAL",
            "VARCHAR", "INT", "INTEGER", "BIGINT", "SMALLINT", "DECIMAL", "NUMERIC",
            "FLOAT", "DOUBLE", "BOOLEAN", "DATE", "TIMESTAMP", "TEXT", "CHAR",
            "EXPLAIN", "ANALYZE", "EXEC", "EXECUTE", "PROCEDURE", "FUNCTION",
            "TRUE", "FALSE", "SCHEMA", "SHOW", "DESCRIBE", "USE", "MERGE"
    );

    // ── Schema cache: connectionId → sorted list of identifiers ──────────────
    private static final Map<String, List<String>> schemaCache = new ConcurrentHashMap<>();
    private static final Set<String> loadingConnections = ConcurrentHashMap.newKeySet();
    private static final ExecutorService bgLoader =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "autocomplete-loader");
                t.setDaemon(true);
                return t;
            });

    // ── Instance state ────────────────────────────────────────────────────────
    private final JTextComponent editor;
    private final Supplier<ConnectionInfo> connectionSupplier;
    private final Supplier<Connection> jdbcSupplier;

    private final JPopupMenu popup;
    private final JList<String> list;
    private final DefaultListModel<String> listModel;
    private final javax.swing.Timer debounce;

    // Suppresses focusLost-triggered hide during popup.show() on some LAFs
    private boolean suppressFocusHide = false;

    public SqlAutoComplete(JTextComponent editor,
                           Supplier<ConnectionInfo> connectionSupplier,
                           Supplier<Connection> jdbcSupplier) {
        this.editor = editor;
        this.connectionSupplier = connectionSupplier;
        this.jdbcSupplier = jdbcSupplier;

        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusable(false);
        list.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        list.setCellRenderer(new SuggestionRenderer());

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setFocusable(false);
        scroll.getViewport().setFocusable(false);

        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(scroll, BorderLayout.CENTER);
        popup.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));
        // CRITICAL: keep popup non-focusable so the editor never loses focus
        popup.setFocusable(false);

        // Debounce timer — fires 250 ms after last keystroke
        debounce = new javax.swing.Timer(250, e -> triggerSuggest(false));
        debounce.setRepeats(false);

        installListeners();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Pre-warm the schema cache for a connection. Call this after connecting
     * so the first keystroke is instant.
     */
    public static void warmCache(ConnectionInfo info, Connection conn) {
        if (info == null || conn == null) return;
        loadSchemaAsync(info, conn);
    }

    /** Invalidate cached schema for a connection (e.g. after disconnect). */
    public static void invalidateCache(String connectionId) {
        schemaCache.remove(connectionId);
        loadingConnections.remove(connectionId);
    }

    // ── Listener setup ────────────────────────────────────────────────────────

    private void installListeners() {
        // Keystroke → debounce / navigation
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popup.isVisible()) {
                    handlePopupNavigation(e);
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown()) {
                    e.consume();
                    triggerSuggest(true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                // Ignore keys that don't produce text or are navigation/control
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER
                        || code == KeyEvent.VK_TAB  || code == KeyEvent.VK_UP
                        || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_SHIFT
                        || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT
                        || code == KeyEvent.VK_META || e.isControlDown()
                        || (code == KeyEvent.VK_SPACE && e.isControlDown())) return;
                debounce.restart();
            }
        });

        // mousePressed fires before focus changes — accept on press so popup
        // doesn't vanish before the click is processed
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    list.setSelectedIndex(idx);
                    acceptSelected();
                }
            }
        });

        // Hide popup when editor loses focus — but only if focus went somewhere
        // other than the popup or its children (popup is non-focusable so the
        // editor should never actually lose focus to it, but guard anyway)
        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Component opposite = e.getOppositeComponent();
                // If focus moved into the popup hierarchy, keep it open
                if (opposite != null && SwingUtilities.isDescendingFrom(opposite, popup)) return;
                SwingUtilities.invokeLater(() -> hidePopup());
            }
        });
    }

    private void handlePopupNavigation(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE -> { e.consume(); hidePopup(); }
            case KeyEvent.VK_DOWN -> {
                e.consume();
                int next = Math.min(list.getSelectedIndex() + 1, listModel.size() - 1);
                list.setSelectedIndex(next);
                list.ensureIndexIsVisible(next);
            }
            case KeyEvent.VK_UP -> {
                e.consume();
                int prev = Math.max(list.getSelectedIndex() - 1, 0);
                list.setSelectedIndex(prev);
                list.ensureIndexIsVisible(prev);
            }
            case KeyEvent.VK_ENTER, KeyEvent.VK_TAB -> { e.consume(); acceptSelected(); }
        }
    }

    // ── Core suggest logic ────────────────────────────────────────────────────

    private void triggerSuggest(boolean forceOpen) {
        String prefix = getCurrentWordPrefix();
        if (!forceOpen && prefix.length() < 2) { hidePopup(); return; }

        List<String> candidates = buildCandidates(prefix);
        if (candidates.isEmpty()) { hidePopup(); return; }

        listModel.clear();
        int limit = Math.min(candidates.size(), 12);
        for (int i = 0; i < limit; i++) listModel.addElement(candidates.get(i));
        list.setSelectedIndex(0);

        showPopup();
    }

    private List<String> buildCandidates(String prefix) {
        String upper = prefix.toUpperCase();
        List<String> result = new ArrayList<>();

        // 1. Schema identifiers (tables, columns, views…) — from cache
        ConnectionInfo info = connectionSupplier.get();
        if (info != null) {
            List<String> cached = schemaCache.get(info.getId());
            if (cached != null) {
                for (String s : cached) {
                    if (s.toUpperCase().startsWith(upper)) result.add(s);
                    if (result.size() >= 50) break;
                }
            } else {
                // Trigger background load if not already loading
                Connection conn = jdbcSupplier.get();
                if (conn != null) loadSchemaAsync(info, conn);
            }
        }

        // 2. SQL keywords
        for (String kw : SQL_KEYWORDS) {
            if (kw.startsWith(upper) && !result.contains(kw)) {
                result.add(kw);
                if (result.size() >= 60) break;
            }
        }

        return result;
    }

    // ── Schema background loader ──────────────────────────────────────────────

    private static void loadSchemaAsync(ConnectionInfo info, Connection conn) {
        if (loadingConnections.contains(info.getId())) return;
        loadingConnections.add(info.getId());

        bgLoader.submit(() -> {
            try {
                SchemaExplorerService svc = new SchemaExplorerService();
                List<String> identifiers = new ArrayList<>();

                List<String> schemas = svc.getSchemas(conn, info.getDbType());
                for (String schema : schemas) {
                    try {
                        svc.getTables(conn, info.getDbType(), schema)
                                .forEach(t -> { identifiers.add(t); identifiers.add(schema + "." + t); });
                        svc.getViews(conn, info.getDbType(), schema)
                                .forEach(v -> { identifiers.add(v); identifiers.add(schema + "." + v); });
                    } catch (Exception ignored) {}
                }

                Collections.sort(identifiers);
                schemaCache.put(info.getId(), identifiers);
            } catch (Exception ignored) {
                // Connection may not be ready yet — will retry on next keystroke
            } finally {
                loadingConnections.remove(info.getId());
            }
        });
    }

    // ── Popup positioning & acceptance ───────────────────────────────────────

    private void showPopup() {
        try {
            int caret = editor.getCaretPosition();
            Rectangle r = editor.modelToView2D(caret).getBounds();

            int rowHeight = list.getFixedCellHeight();
            if (rowHeight <= 0 && !listModel.isEmpty()) {
                Component proto = list.getCellRenderer()
                        .getListCellRendererComponent(list, listModel.getElementAt(0), 0, false, false);
                rowHeight = proto.getPreferredSize().height;
            }
            if (rowHeight <= 0) rowHeight = 24;

            int visibleRows = Math.min(listModel.size(), 10);
            int popupH = visibleRows * rowHeight + 8;

            popup.setPreferredSize(new Dimension(320, popupH));

            // Suppress focusLost that popup.show() can fire on some LAFs
            suppressFocusHide = true;
            popup.show(editor, r.x, r.y + r.height);
            suppressFocusHide = false;

            editor.requestFocusInWindow();
        } catch (BadLocationException ignored) {}
    }

    private void hidePopup() {
        // Do NOT stop the debounce — user may keep typing after dismissal
        popup.setVisible(false);
    }

    private void acceptSelected() {
        String selected = list.getSelectedValue();
        hidePopup();
        if (selected == null) return;

        String prefix = getCurrentWordPrefix();
        int caretPos = editor.getCaretPosition();
        int start = caretPos - prefix.length();
        try {
            editor.getDocument().remove(start, prefix.length());
            editor.getDocument().insertString(start, selected, null);
        } catch (BadLocationException ignored) {}
    }

    /** Extract the word being typed immediately before the caret. */
    private String getCurrentWordPrefix() {
        try {
            int caret = editor.getCaretPosition();
            String text = editor.getDocument().getText(0, caret);
            int i = text.length() - 1;
            while (i >= 0 && (Character.isLetterOrDigit(text.charAt(i))
                    || text.charAt(i) == '_' || text.charAt(i) == '.')) {
                i--;
            }
            return text.substring(i + 1);
        } catch (BadLocationException e) {
            return "";
        }
    }

    // ── Custom cell renderer ──────────────────────────────────────────────────

    private static class SuggestionRenderer extends DefaultListCellRenderer {
        private static final Color KW_COLOR  = new Color(100, 149, 237);
        private static final Color TBL_COLOR = new Color(80, 200, 120);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String s = (String) value;
            boolean isKeyword = SQL_KEYWORDS.contains(s.toUpperCase());
            if (!isSelected) {
                setForeground(isKeyword ? KW_COLOR : TBL_COLOR);
            }
            setText((isKeyword ? "⌨  " : "⊞  ") + s);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            return this;
        }
    }
}
