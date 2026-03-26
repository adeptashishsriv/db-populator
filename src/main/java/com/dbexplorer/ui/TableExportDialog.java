package com.dbexplorer.ui;

import com.dbexplorer.model.ConnectionInfo;
import com.dbexplorer.service.TableDataExportService;
import com.dbexplorer.service.TableDataExportService.StreamResult;
import com.dbexplorer.ui.DbIcons;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.List;

/**
 * Non-modal dialog for table-level exports.
 *
 * Tabs: DDL | INSERT Statements | Update SQL | CSV
 *
 * Scalability design:
 *  - INSERT, UPDATE, CSV stream directly to a temp file on a background thread.
 *    The text area shows only the first PREVIEW_ROWS lines; the full file is
 *    never held in heap.
 *  - A JProgressBar shows live row counts during generation.
 *  - Generation starts immediately when a tab becomes active (either via
 *    selectTab() called from the context menu, or by clicking the tab).
 *  - Save copies the already-written temp file — no re-generation needed.
 *  - DDL is small by nature and is kept in memory as before.
 */
public class TableExportDialog extends JDialog {

    private static final int TAB_DDL    = 0;
    private static final int TAB_INSERT = 1;
    private static final int TAB_UPDATE = 2;
    private static final int TAB_CSV    = 3;

    private static final String[] TAB_TITLES = {
        "DDL", "INSERT Statements", "Update SQL", "CSV"
    };
    private static final javax.swing.Icon[] TAB_ICONS = {
        DbIcons.MENU_DDL, DbIcons.MENU_INSERT, DbIcons.MENU_UPDATE, DbIcons.MENU_CSV
    };
    private static final String[] FILE_EXT   = {"sql", "sql", "sql", "csv"};
    private static final String[] FILE_DESC  = {
        "SQL files (*.sql)", "SQL files (*.sql)", "SQL files (*.sql)", "CSV files (*.csv)"
    };
    private static final String[] FILE_SUFFIX = {"_ddl", "_inserts", "_updates", ""};

    private final ConnectionInfo info;
    private final Connection     conn;
    private final String         schema;
    private final String         table;

    // Per-tab state
    private final JTextArea[]    textAreas    = new JTextArea[4];
    private final JProgressBar[] progressBars = new JProgressBar[4];
    private final JLabel[]       statusLabels = new JLabel[4];
    private final JButton[]      saveBtns     = new JButton[4];
    private final boolean[]      generated    = new boolean[4];
    // Temp files for streaming exports (INSERT/UPDATE/CSV); null for DDL
    private final File[]         tempFiles    = new File[4];

    private final JTabbedPane tabs;

    public TableExportDialog(Frame owner, ConnectionInfo info,
                              Connection conn, String schema, String table) {
        super(owner, "Export — " + (schema != null ? schema + "." : "") + table, false);
        this.info   = info;
        this.conn   = conn;
        this.schema = schema;
        this.table  = table;

        tabs = new JTabbedPane();
        for (int i = 0; i < 4; i++) tabs.addTab(TAB_TITLES[i], TAB_ICONS[i], buildTab(i));

        // Lazy-generate on tab switch
        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            if (!generated[idx]) generateTab(idx);
        });

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        setSize(960, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Clean up temp files on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                for (File f : tempFiles) { if (f != null) f.delete(); }
            }
        });
    }

    /**
     * Pre-select a tab and start generation immediately.
     * Called before setVisible(true) from the context menu handler.
     */
    public void selectTab(int idx) {
        if (idx >= 0 && idx < tabs.getTabCount()) {
            tabs.setSelectedIndex(idx);
            if (!generated[idx]) generateTab(idx);
        }
    }

    // ── Tab construction ──────────────────────────────────────────────────────

    private JPanel buildTab(int idx) {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        ta.setLineWrap(false);
        ta.setText("Select this tab to generate…");
        textAreas[idx] = ta;

        JScrollPane scroll = new JScrollPane(ta,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Progress bar — visible only during generation
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(false);
        pb.setStringPainted(true);
        pb.setString("Ready");
        pb.setVisible(false);
        progressBars[idx] = pb;

        JLabel status = new JLabel("  Ready");
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 11f));
        status.setForeground(Color.GRAY);
        statusLabels[idx] = status;

        JButton copyBtn = new JButton("Copy", DbIcons.MENU_COPY);
        copyBtn.setToolTipText("Copy preview to clipboard");
        copyBtn.addActionListener(e -> copyToClipboard(idx));

        JButton saveBtn = new JButton("Save", DbIcons.MENU_SAVE);
        saveBtn.setToolTipText("Save complete export to file");
        saveBtn.setEnabled(false);   // enabled once generation completes
        saveBtn.addActionListener(e -> saveToFile(idx));
        saveBtns[idx] = saveBtn;

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(copyBtn);
        tb.add(Box.createHorizontalStrut(6));
        tb.add(saveBtn);
        tb.add(Box.createHorizontalStrut(12));
        tb.add(pb);
        tb.add(Box.createHorizontalStrut(8));
        tb.add(status);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tb,     BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ── Generation ────────────────────────────────────────────────────────────

    private void generateTab(int idx) {
        generated[idx] = true;

        JTextArea    ta     = textAreas[idx];
        JProgressBar pb     = progressBars[idx];
        JLabel       status = statusLabels[idx];
        JButton      save   = saveBtns[idx];

        ta.setText("Generating…");
        pb.setValue(0);
        pb.setString("0 rows");
        pb.setVisible(true);
        status.setText("  Generating…");
        status.setForeground(Color.GRAY);
        save.setEnabled(false);

        TableDataExportService svc = new TableDataExportService();

        new SwingWorker<Object, Integer>() {

            @Override
            protected Object doInBackground() throws Exception {
                TableDataExportService.ProgressCallback cb = rows -> publish(rows);

                if (idx == TAB_DDL) {
                    // DDL is small — keep in memory
                    return svc.exportTableDdl(conn, info.getDbType(), schema, table);
                }

                // Streaming exports — write to a temp file
                String suffix = FILE_SUFFIX[idx].isEmpty() ? table : table + FILE_SUFFIX[idx];
                File tmp = File.createTempFile(suffix + "_", "." + FILE_EXT[idx]);
                tmp.deleteOnExit();
                tempFiles[idx] = tmp;

                return switch (idx) {
                    case TAB_INSERT -> svc.exportInserts(conn, info.getDbType(), schema, table, tmp, cb);
                    case TAB_UPDATE -> svc.exportUpdates(conn, info.getDbType(), schema, table, tmp, cb);
                    case TAB_CSV    -> svc.exportCsv(conn, info.getDbType(), schema, table, tmp, cb);
                    default         -> "";
                };
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                pb.setString(latest + " rows…");
                status.setText("  " + latest + " rows processed…");
            }

            @Override
            protected void done() {
                pb.setVisible(false);
                try {
                    Object result = get();
                    if (result instanceof String ddl) {
                        // DDL tab
                        ta.setText(ddl);
                        ta.setCaretPosition(0);
                        status.setText("  " + ddl.split("\n", -1).length + " lines");
                        save.setEnabled(true);
                    } else if (result instanceof StreamResult sr) {
                        ta.setText(sr.preview());
                        ta.setCaretPosition(0);
                        long fileSizeKb = sr.file().length() / 1024;
                        status.setText("  " + sr.rowCount() + " rows exported"
                                + (sr.rowCount() > TableDataExportService.PREVIEW_ROWS
                                   ? " (preview shown — " + fileSizeKb + " KB file ready to save)"
                                   : ""));
                        status.setForeground(UIManager.getColor("Label.foreground"));
                        save.setEnabled(true);
                    }
                } catch (Exception ex) {
                    ta.setText("-- Error: " + ex.getMessage());
                    status.setText("  Error: " + ex.getMessage());
                    status.setForeground(new Color(200, 50, 50));
                }
            }
        }.execute();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void copyToClipboard(int idx) {
        String text = textAreas[idx].getText();
        if (text == null || text.isBlank()) return;
        Toolkit.getDefaultToolkit()
               .getSystemClipboard()
               .setContents(new StringSelection(text), null);
        statusLabels[idx].setText("  Preview copied to clipboard");
    }

    private void saveToFile(int idx) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Export");
        String baseName = (schema != null ? schema + "_" : "") + table + FILE_SUFFIX[idx];
        fc.setSelectedFile(new File(baseName + "." + FILE_EXT[idx]));
        fc.setFileFilter(new FileNameExtensionFilter(FILE_DESC[idx], FILE_EXT[idx]));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File dest = fc.getSelectedFile();
        String ext = "." + FILE_EXT[idx];
        if (!dest.getName().toLowerCase().endsWith(ext))
            dest = new File(dest.getAbsolutePath() + ext);

        try {
            if (idx == TAB_DDL) {
                // DDL is in the text area
                Files.writeString(dest.toPath(), textAreas[idx].getText());
            } else {
                // Copy the already-written temp file
                File src = tempFiles[idx];
                if (src == null || !src.exists()) {
                    JOptionPane.showMessageDialog(this,
                            "Export file not found. Please regenerate.",
                            "Save Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Files.copy(src.toPath(), dest.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            long kb = dest.length() / 1024;
            statusLabels[idx].setText("  Saved: " + dest.getName() + " (" + kb + " KB)");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save file:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
