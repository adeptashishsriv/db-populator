package com.dbexplorer.ui;

import com.dbexplorer.model.ConnectionInfo;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * Non-modal dialog showing the schema ER diagram for a specific schema.
 */
public class SchemaDiagramDialog extends JDialog {

    public SchemaDiagramDialog(Frame owner, ConnectionInfo info,
                                Connection conn, String schema) {
        super(owner, "⬡ Schema Diagram — " + info.getName()
                + (schema != null ? " / " + schema : ""), false);

        SchemaDiagramPanel diagram = new SchemaDiagramPanel();

        // Toolbar
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        JButton fitBtn = new JButton("⊡ Fit");
        fitBtn.setToolTipText("Fit all tables into view");
        fitBtn.addActionListener(e -> diagram.fitToView());

        JLabel hint = new JLabel("  Left-drag: move table  •  Right-drag: pan  •  Scroll: zoom  •  Corner-drag: resize");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(Color.GRAY);

        tb.add(fitBtn);
        tb.add(Box.createHorizontalStrut(12));
        tb.add(hint);

        // Wrap diagram in a scroll pane — diagram implements Scrollable so
        // the scroll pane knows the preferred viewport size and unit increments.
        JScrollPane scroll = new JScrollPane(diagram,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getHorizontalScrollBar().setUnitIncrement(20);
        // Transparent corner so it blends with the dark canvas
        scroll.setBackground(new Color(18, 18, 28));
        scroll.getViewport().setBackground(new Color(18, 18, 28));

        setLayout(new BorderLayout());
        add(tb, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        setSize(1200, 800);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Load after dialog is visible so getWidth/getHeight are valid for fitToView
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                diagram.loadSchema(info, conn, schema);
            }
        });
    }
}
