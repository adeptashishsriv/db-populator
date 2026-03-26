package com.dbexplorer.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Programmatically rendered vector icons for the database tree.
 * All icons are drawn with Java2D so they look crisp at any DPI
 * and automatically adapt to light/dark themes.
 */
public final class DbIcons {

    public static final int SIZE = 16;

    // Cached icons
    public static final Icon DATABASE_CONNECTED   = make(DbIcons::drawDatabaseConnected);
    public static final Icon DATABASE_DISCONNECTED = make(DbIcons::drawDatabaseDisconnected);
    public static final Icon DATABASE_DYNAMO       = make(DbIcons::drawDynamo);
    public static final Icon DATABASE_SQLITE       = make(DbIcons::drawSQLite);
    public static final Icon DATABASE_GENERIC      = make(DbIcons::drawGeneric);
    public static final Icon SCHEMA                = make(DbIcons::drawSchema);
    public static final Icon TABLE                 = make(DbIcons::drawTable);
    public static final Icon VIEW                  = make(DbIcons::drawView);
    public static final Icon FUNCTION              = make(DbIcons::drawFunction);
    public static final Icon PROCEDURE             = make(DbIcons::drawProcedure);
    public static final Icon INDEX                 = make(DbIcons::drawIndex);
    public static final Icon SEQUENCE              = make(DbIcons::drawSequence);
    public static final Icon COLUMN                = make(DbIcons::drawColumn);
    public static final Icon FOLDER_OPEN           = make(DbIcons::drawFolderOpen);
    public static final Icon FOLDER_CLOSED         = make(DbIcons::drawFolderClosed);
    public static final Icon LOADING               = make(DbIcons::drawLoading);

    // ── Toolbar icons (20 px) ─────────────────────────────────────────────────
    public static final int TB = 20;
    public static final Icon TB_ADD        = makeTb(DbIcons::tbAdd);
    public static final Icon TB_DISCONNECT = makeTb(DbIcons::tbDisconnect);
    public static final Icon TB_RUN        = makeTb(DbIcons::tbRun);
    public static final Icon TB_NEW_TAB    = makeTb(DbIcons::tbNewTab);
    public static final Icon TB_EXPLAIN    = makeTb(DbIcons::tbExplain);
    public static final Icon TB_CLEAR      = makeTb(DbIcons::tbClear);
    public static final Icon TB_ABOUT      = makeTb(DbIcons::tbAbout);

    // ── Menu / dialog icons (16 px) ───────────────────────────────────────────
    public static final Icon MENU_DDL      = make(DbIcons::menuDdl);
    public static final Icon MENU_INSERT   = make(DbIcons::menuInsert);
    public static final Icon MENU_UPDATE   = make(DbIcons::menuUpdate);
    public static final Icon MENU_CSV      = make(DbIcons::menuCsv);
    public static final Icon MENU_DIAGRAM  = make(DbIcons::menuDiagram);
    public static final Icon MENU_VIEW_DATA= make(DbIcons::menuViewData);
    public static final Icon MENU_COPY     = make(DbIcons::menuCopy);
    public static final Icon MENU_SAVE     = make(DbIcons::menuSave);
    public static final Icon MENU_EXPORT   = make(DbIcons::menuExport);

    private DbIcons() {}

    @FunctionalInterface
    private interface Painter { void paint(Graphics2D g); }

    private static Icon make(Painter p) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        p.paint(g);
        g.dispose();
        return new ImageIcon(img);
    }

    private static Icon makeTb(Painter p) {
        BufferedImage img = new BufferedImage(TB, TB, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        p.paint(g);
        g.dispose();
        return new ImageIcon(img);
    }

    // ── Database cylinder (connected = green accent) ──────────────────────────
    private static void drawDatabaseConnected(Graphics2D g) {
        drawCylinder(g, new Color(41, 182, 246), new Color(2, 136, 209), new Color(100, 220, 100));
    }

    private static void drawDatabaseDisconnected(Graphics2D g) {
        drawCylinder(g, new Color(120, 120, 130), new Color(80, 80, 90), new Color(180, 60, 60));
    }

    private static void drawCylinder(Graphics2D g, Color body, Color shadow, Color dot) {
        int x = 1, y = 2, w = 14, h = 12;
        int ry = 3; // ellipse y-radius for top/bottom caps

        // Body gradient
        GradientPaint gp = new GradientPaint(x, 0, body.brighter(), x + w, 0, shadow);
        g.setPaint(gp);
        g.fillRoundRect(x, y + ry, w, h - ry, 3, 3);

        // Bottom cap ellipse
        g.setColor(shadow);
        g.fillOval(x, y + h - ry, w, ry * 2);

        // Top cap ellipse
        g.setColor(body.brighter());
        g.fillOval(x, y, w, ry * 2);

        // Horizontal lines on body (data rows feel)
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(0.8f));
        for (int ly = y + ry + 2; ly < y + h - 1; ly += 3) {
            g.drawLine(x + 1, ly, x + w - 1, ly);
        }

        // Status dot (bottom-right)
        g.setColor(dot);
        g.fillOval(x + w - 4, y + h - 1, 5, 5);
        g.setColor(dot.darker());
        g.setStroke(new BasicStroke(0.7f));
        g.drawOval(x + w - 4, y + h - 1, 5, 5);
    }

    // ── SQLite (teal cylinder with file icon) ────────────────────────────────
    private static void drawSQLite(Graphics2D g) {
        drawCylinder(g, new Color(0, 188, 212), new Color(0, 131, 143), new Color(100, 220, 100));
        // Small "S" overlay
        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(new Font("SansSerif", Font.BOLD, 8));
        g.drawString("S", 5, 10);
    }

    // ── Generic JDBC (grey cylinder with "?" overlay) ────────────────────────
    private static void drawGeneric(Graphics2D g) {
        drawCylinder(g, new Color(120, 144, 156), new Color(84, 110, 122), new Color(100, 220, 100));
        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString("?", 5, 10);
    }

    // ── DynamoDB (orange lightning bolt) ─────────────────────────────────────
    private static void drawDynamo(Graphics2D g) {        // Cylinder in orange
        drawCylinder(g, new Color(255, 153, 0), new Color(200, 100, 0), new Color(100, 220, 100));
        // Lightning bolt overlay
        g.setColor(new Color(255, 255, 255, 200));
        Path2D bolt = new Path2D.Float();
        bolt.moveTo(9, 2);
        bolt.lineTo(6, 8);
        bolt.lineTo(8.5, 8);
        bolt.lineTo(6, 14);
        bolt.lineTo(10, 7.5);
        bolt.lineTo(7.5, 7.5);
        bolt.closePath();
        g.fill(bolt);
    }

    // ── Schema (blueprint/layers) ─────────────────────────────────────────────
    private static void drawSchema(Graphics2D g) {
        Color c1 = new Color(100, 181, 246);
        Color c2 = new Color(66, 165, 245);
        Color c3 = new Color(30, 136, 229);

        // Three stacked rounded rectangles (layers)
        g.setColor(c3);
        g.fillRoundRect(2, 10, 12, 4, 3, 3);
        g.setColor(c2);
        g.fillRoundRect(2, 6,  12, 4, 3, 3);
        g.setColor(c1);
        g.fillRoundRect(2, 2,  12, 4, 3, 3);

        // Subtle borders
        g.setStroke(new BasicStroke(0.6f));
        g.setColor(new Color(0, 0, 0, 50));
        g.drawRoundRect(2, 10, 12, 4, 3, 3);
        g.drawRoundRect(2, 6,  12, 4, 3, 3);
        g.drawRoundRect(2, 2,  12, 4, 3, 3);
    }

    // ── Table (grid) ──────────────────────────────────────────────────────────
    private static void drawTable(Graphics2D g) {
        Color header = new Color(66, 165, 245);
        Color row1   = new Color(227, 242, 253);
        Color row2   = new Color(187, 222, 251);
        Color border = new Color(100, 160, 220);

        // Header row
        g.setColor(header);
        g.fillRoundRect(1, 1, 14, 4, 2, 2);

        // Data rows
        g.setColor(row1);
        g.fillRect(1, 5, 14, 3);
        g.setColor(row2);
        g.fillRect(1, 8, 14, 3);
        g.setColor(row1);
        g.fillRect(1, 11, 14, 3);

        // Outer border
        g.setColor(border);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(1, 1, 14, 13, 2, 2);

        // Column divider
        g.setColor(new Color(100, 160, 220, 120));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(7, 1, 7, 14);

        // Row dividers
        g.drawLine(1, 5,  15, 5);
        g.drawLine(1, 8,  15, 8);
        g.drawLine(1, 11, 15, 11);
    }

    // ── View (eye over grid) ──────────────────────────────────────────────────
    private static void drawView(Graphics2D g) {
        // Faded table background
        g.setColor(new Color(179, 229, 252, 160));
        g.fillRoundRect(1, 5, 14, 10, 2, 2);
        g.setColor(new Color(100, 181, 246, 100));
        g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(1, 5, 14, 10, 2, 2);
        g.drawLine(1, 9, 15, 9);
        g.drawLine(7, 5, 7, 15);

        // Eye shape
        g.setColor(new Color(255, 255, 255, 220));
        g.fillOval(2, 1, 12, 8);

        Path2D eye = new Path2D.Float();
        eye.moveTo(2, 5);
        eye.quadTo(8, 0, 14, 5);
        eye.quadTo(8, 10, 2, 5);
        eye.closePath();
        g.setColor(new Color(30, 136, 229));
        g.fill(eye);

        // Pupil
        g.setColor(new Color(13, 71, 161));
        g.fillOval(5, 3, 6, 5);
        g.setColor(new Color(255, 255, 255, 180));
        g.fillOval(6, 4, 2, 2);
    }

    // ── Function (ƒ symbol) ───────────────────────────────────────────────────
    private static void drawFunction(Graphics2D g) {
        g.setColor(new Color(171, 71, 188));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 11));
        FontMetrics fm = g.getFontMetrics();
        String txt = "f";
        int tx = (SIZE - fm.stringWidth(txt)) / 2;
        int ty = (SIZE + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(txt, tx, ty);
    }

    // ── Procedure (gear/cog) ──────────────────────────────────────────────────
    private static void drawProcedure(Graphics2D g) {
        g.setColor(new Color(255, 167, 38));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);

        // Gear teeth
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        int cx = 8, cy = 8, r = 4;
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            int x1 = (int)(cx + Math.cos(a) * (r - 1));
            int y1 = (int)(cy + Math.sin(a) * (r - 1));
            int x2 = (int)(cx + Math.cos(a) * (r + 2));
            int y2 = (int)(cy + Math.sin(a) * (r + 2));
            g.drawLine(x1, y1, x2, y2);
        }
        g.fillOval(cx - 2, cy - 2, 4, 4);
    }

    // ── Index (lightning bolt) ────────────────────────────────────────────────
    private static void drawIndex(Graphics2D g) {
        g.setColor(new Color(255, 213, 79));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);

        g.setColor(new Color(100, 70, 0));
        Path2D bolt = new Path2D.Float();
        bolt.moveTo(10, 2);
        bolt.lineTo(5,  8);
        bolt.lineTo(8,  8);
        bolt.lineTo(5, 14);
        bolt.lineTo(11, 7);
        bolt.lineTo(8,  7);
        bolt.closePath();
        g.fill(bolt);
    }

    // ── Sequence (123 counter) ────────────────────────────────────────────────
    private static void drawSequence(Graphics2D g) {
        g.setColor(new Color(38, 198, 218));
        g.fillRoundRect(1, 1, 14, 14, 4, 4);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 7));
        g.drawString("1", 3, 7);
        g.drawString("2", 3, 13);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(8, 4, 13, 4);
        g.drawLine(8, 10, 13, 10);
    }

    // ── Column (single row indicator) ────────────────────────────────────────
    private static void drawColumn(Graphics2D g) {
        g.setColor(new Color(144, 202, 249));
        g.fillRect(1, 5, 14, 6);
        g.setColor(new Color(66, 165, 245));
        g.setStroke(new BasicStroke(0.8f));
        g.drawRect(1, 5, 14, 6);
        g.drawLine(6, 5, 6, 11);

        // Small key icon for primary key feel
        g.setColor(new Color(255, 193, 7));
        g.fillOval(2, 6, 3, 3);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(5, 8, 8, 8);
        g.drawLine(7, 8, 7, 10);
    }

    // ── Folder closed ─────────────────────────────────────────────────────────
    private static void drawFolderClosed(Graphics2D g) {
        Color body = new Color(255, 202, 40);
        Color tab  = new Color(255, 179, 0);

        // Tab
        g.setColor(tab);
        g.fillRoundRect(1, 4, 6, 3, 2, 2);

        // Body
        g.setColor(body);
        g.fillRoundRect(1, 6, 14, 9, 3, 3);

        // Shine
        g.setColor(new Color(255, 255, 255, 60));
        g.fillRoundRect(2, 7, 12, 3, 2, 2);

        // Border
        g.setColor(new Color(180, 130, 0, 120));
        g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(1, 6, 14, 9, 3, 3);
    }

    // ── Folder open ───────────────────────────────────────────────────────────
    private static void drawFolderOpen(Graphics2D g) {
        Color body = new Color(255, 213, 79);
        Color tab  = new Color(255, 193, 7);

        g.setColor(tab);
        g.fillRoundRect(1, 4, 6, 3, 2, 2);

        // Open folder body (trapezoid feel)
        Path2D folder = new Path2D.Float();
        folder.moveTo(1,  7);
        folder.lineTo(15, 7);
        folder.lineTo(13, 15);
        folder.lineTo(1,  15);
        folder.closePath();
        g.setColor(body);
        g.fill(folder);

        g.setColor(new Color(255, 255, 255, 60));
        g.fillRect(2, 8, 12, 3);

        g.setColor(new Color(180, 130, 0, 120));
        g.setStroke(new BasicStroke(0.8f));
        g.draw(folder);
    }

    // ── Loading spinner (arc) ─────────────────────────────────────────────────
    private static void drawLoading(Graphics2D g) {
        g.setColor(new Color(150, 150, 150, 80));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(2, 2, 12, 12);
        g.setColor(new Color(66, 165, 245));
        g.drawArc(2, 2, 12, 12, 90, -270);
    }

    // ── Toolbar painters (20 px canvas) ──────────────────────────────────────

    /** Green "+" circle — Add Connection */
    private static void tbAdd(Graphics2D g) {
        g.setColor(new Color(56, 142, 60));
        g.fillOval(1, 1, 18, 18);
        g.setColor(new Color(200, 230, 200, 60));
        g.fillOval(3, 3, 8, 6);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(10, 5, 10, 15);
        g.drawLine(5, 10, 15, 10);
    }

    /** Red circle with horizontal bar — Disconnect */
    private static void tbDisconnect(Graphics2D g) {
        g.setColor(new Color(198, 40, 40));
        g.fillOval(1, 1, 18, 18);
        g.setColor(new Color(255, 200, 200, 60));
        g.fillOval(3, 3, 8, 6);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        // Plug/unplug symbol: two lines with gap
        g.drawLine(6, 7, 6, 13);
        g.drawLine(14, 7, 14, 13);
        g.drawLine(6, 7, 14, 7);
        // Strike-through diagonal
        g.setColor(new Color(255, 100, 100));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(5, 5, 15, 15);
    }

    /** Green play triangle — Run Query */
    private static void tbRun(Graphics2D g) {
        // Circle background
        g.setColor(new Color(46, 125, 50));
        g.fillOval(1, 1, 18, 18);
        g.setColor(new Color(200, 255, 200, 50));
        g.fillOval(3, 3, 8, 6);
        // Triangle
        g.setColor(Color.WHITE);
        Path2D tri = new Path2D.Float();
        tri.moveTo(7, 5);
        tri.lineTo(7, 15);
        tri.lineTo(16, 10);
        tri.closePath();
        g.fill(tri);
    }

    /** Blue document with "+" — New Tab */
    private static void tbNewTab(Graphics2D g) {
        // Page body
        g.setColor(new Color(66, 165, 245));
        g.fillRoundRect(3, 1, 11, 14, 2, 2);
        // Folded corner
        g.setColor(new Color(30, 100, 180));
        Path2D corner = new Path2D.Float();
        corner.moveTo(10, 1); corner.lineTo(14, 5); corner.lineTo(10, 5);
        corner.closePath();
        g.fill(corner);
        // White lines (content)
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(5, 8,  11, 8);
        g.drawLine(5, 10, 11, 10);
        // Green "+" badge bottom-right
        g.setColor(new Color(56, 142, 60));
        g.fillOval(11, 12, 8, 8);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(15, 14, 15, 18);
        g.drawLine(13, 16, 17, 16);
    }

    /** Purple bar chart — Explain Plan */
    private static void tbExplain(Graphics2D g) {
        // Background rounded rect
        g.setColor(new Color(74, 20, 140));
        g.fillRoundRect(1, 1, 18, 18, 4, 4);
        g.setColor(new Color(200, 150, 255, 40));
        g.fillRoundRect(2, 2, 10, 6, 3, 3);
        // Bars
        int[] heights = {12, 8, 14, 6, 10};
        Color[] barColors = {
            new Color(206, 147, 216),
            new Color(186, 104, 200),
            new Color(171, 71, 188),
            new Color(156, 39, 176),
            new Color(123, 31, 162)
        };
        for (int i = 0; i < 5; i++) {
            g.setColor(barColors[i]);
            int bx = 2 + i * 3 + i / 2;
            g.fillRoundRect(bx, 18 - heights[i], 3, heights[i] - 1, 1, 1);
        }
    }

    /** Red trash can — Clear */
    private static void tbClear(Graphics2D g) {
        g.setColor(new Color(183, 28, 28));
        // Lid
        g.fillRoundRect(4, 3, 12, 3, 2, 2);
        g.fillRect(7, 1, 6, 3);
        // Body
        g.fillRoundRect(5, 6, 10, 12, 2, 2);
        // Lines on body
        g.setColor(new Color(255, 200, 200, 120));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(8,  8, 8,  16);
        g.drawLine(10, 8, 10, 16);
        g.drawLine(12, 8, 12, 16);
    }

    /** Teal info circle — About */
    private static void tbAbout(Graphics2D g) {
        g.setColor(new Color(0, 131, 143));
        g.fillOval(1, 1, 18, 18);
        g.setColor(new Color(200, 255, 255, 50));
        g.fillOval(3, 3, 8, 6);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        String t = "i";
        g.drawString(t, (20 - fm.stringWidth(t)) / 2, 15);
    }

    // ── Menu / dialog icon painters (16 px) ──────────────────────────────────

    /** Orange document with "DDL" — DDL export */
    private static void menuDdl(Graphics2D g) {
        g.setColor(new Color(230, 81, 0));
        g.fillRoundRect(2, 1, 9, 12, 2, 2);
        Path2D corner = new Path2D.Float();
        corner.moveTo(8, 1); corner.lineTo(11, 4); corner.lineTo(8, 4);
        corner.closePath();
        g.setColor(new Color(180, 50, 0));
        g.fill(corner);
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, 5));
        g.drawString("DDL", 3, 10);
        // Green badge
        g.setColor(new Color(56, 142, 60));
        g.fillRoundRect(9, 9, 6, 6, 2, 2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(12, 10, 12, 14);
        g.drawLine(10, 12, 14, 12);
    }

    /** Green document with "+" rows — INSERT */
    private static void menuInsert(Graphics2D g) {
        g.setColor(new Color(46, 125, 50));
        g.fillRoundRect(1, 1, 10, 14, 2, 2);
        g.setColor(new Color(200, 230, 200, 180));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(3, 5,  9, 5);
        g.drawLine(3, 7,  9, 7);
        g.drawLine(3, 9,  9, 9);
        g.drawLine(3, 11, 9, 11);
        // "+" badge
        g.setColor(new Color(56, 142, 60));
        g.fillOval(9, 9, 6, 6);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(12, 10, 12, 14);
        g.drawLine(10, 12, 14, 12);
    }

    /** Blue document with pencil — UPDATE */
    private static void menuUpdate(Graphics2D g) {
        g.setColor(new Color(21, 101, 192));
        g.fillRoundRect(1, 1, 10, 14, 2, 2);
        g.setColor(new Color(200, 220, 255, 180));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(3, 5,  9, 5);
        g.drawLine(3, 7,  9, 7);
        g.drawLine(3, 9,  7, 9);
        // Pencil
        g.setColor(new Color(255, 193, 7));
        Path2D pencil = new Path2D.Float();
        pencil.moveTo(10, 10); pencil.lineTo(14, 6);
        pencil.lineTo(15, 7); pencil.lineTo(11, 11);
        pencil.closePath();
        g.fill(pencil);
        g.setColor(new Color(180, 130, 0));
        g.setStroke(new BasicStroke(0.6f));
        g.draw(pencil);
        g.setColor(new Color(255, 100, 100));
        g.fillRect(10, 11, 3, 2);
    }

    /** Teal grid — CSV */
    private static void menuCsv(Graphics2D g) {
        // Sheet background
        g.setColor(new Color(0, 137, 123));
        g.fillRoundRect(1, 1, 14, 14, 2, 2);
        // Grid lines
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(0.7f));
        g.drawLine(1, 5,  15, 5);
        g.drawLine(1, 9,  15, 9);
        g.drawLine(1, 13, 15, 13);
        g.drawLine(5, 1,  5,  15);
        g.drawLine(10, 1, 10, 15);
        // "CSV" text
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 5));
        g.drawString("CSV", 2, 8);
    }

    /** Hexagon — Schema Diagram */
    private static void menuDiagram(Graphics2D g) {
        g.setColor(new Color(81, 45, 168));
        Path2D hex = new Path2D.Float();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60 * i - 30);
            double x = 8 + 6.5 * Math.cos(a);
            double y = 8 + 6.5 * Math.sin(a);
            if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
        }
        hex.closePath();
        g.fill(hex);
        g.setColor(new Color(200, 180, 255, 60));
        g.fillOval(4, 3, 6, 4);
        // Nodes and lines inside
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(5, 8, 11, 8);
        g.drawLine(8, 5, 8, 11);
        g.fillOval(4, 7, 3, 3);
        g.fillOval(10, 7, 3, 3);
        g.fillOval(7, 4, 3, 3);
        g.fillOval(7, 10, 3, 3);
    }

    /** Eye over table rows — View Data */
    private static void menuViewData(Graphics2D g) {
        // Table rows
        g.setColor(new Color(66, 165, 245, 160));
        g.fillRoundRect(1, 8, 14, 7, 2, 2);
        g.setColor(new Color(100, 181, 246, 100));
        g.setStroke(new BasicStroke(0.6f));
        g.drawLine(1, 11, 15, 11);
        // Eye
        g.setColor(new Color(255, 255, 255, 230));
        Path2D eye = new Path2D.Float();
        eye.moveTo(1, 5); eye.quadTo(8, 0, 15, 5); eye.quadTo(8, 10, 1, 5);
        eye.closePath();
        g.fill(eye);
        g.setColor(new Color(30, 136, 229));
        g.fill(eye);
        g.setColor(new Color(13, 71, 161));
        g.fillOval(5, 3, 6, 5);
        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(6, 4, 2, 2);
    }

    /** Two overlapping pages — Copy */
    private static void menuCopy(Graphics2D g) {
        // Back page
        g.setColor(new Color(144, 202, 249));
        g.fillRoundRect(4, 4, 10, 11, 2, 2);
        // Front page
        g.setColor(new Color(25, 118, 210));
        g.fillRoundRect(2, 1, 10, 11, 2, 2);
        // Lines
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(4, 5, 10, 5);
        g.drawLine(4, 7, 10, 7);
        g.drawLine(4, 9, 8,  9);
    }

    /** Floppy disk — Save */
    private static void menuSave(Graphics2D g) {
        // Body
        g.setColor(new Color(69, 90, 100));
        g.fillRoundRect(1, 1, 14, 14, 2, 2);
        // Label area (white rectangle)
        g.setColor(new Color(236, 239, 241));
        g.fillRect(3, 1, 8, 6);
        // Notch on label
        g.setColor(new Color(120, 144, 156));
        g.fillRect(8, 1, 3, 4);
        // Bottom slot
        g.setColor(new Color(120, 144, 156));
        g.fillRoundRect(3, 9, 10, 5, 1, 1);
        g.setColor(new Color(236, 239, 241));
        g.fillRoundRect(4, 10, 8, 3, 1, 1);
    }

    /** Arrow out of box — Export (submenu) */
    private static void menuExport(Graphics2D g) {
        // Box
        g.setColor(new Color(38, 166, 154));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(1, 6, 14, 9, 2, 2);
        // Arrow up-right
        g.setColor(new Color(38, 166, 154));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(8, 1, 8, 9);
        g.drawLine(5, 4, 8, 1);
        g.drawLine(11, 4, 8, 1);
    }
}
