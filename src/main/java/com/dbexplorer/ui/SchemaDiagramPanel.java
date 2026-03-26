package com.dbexplorer.ui;

import com.dbexplorer.model.ConnectionInfo;
import com.dbexplorer.model.DatabaseType;
import com.dbexplorer.service.SchemaExplorerService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.util.*;
import java.util.List;

/**
 * Interactive ER-style schema diagram with:
 * - Force-directed / layered auto-layout (no stacking)
 * - Draggable + resizable table boxes
 * - Scrollable canvas (preferred size tracks world bounds)
 * - Zoom (scroll wheel) and pan (right-drag)
 * - Crow-foot relation lines
 */
public class SchemaDiagramPanel extends JPanel implements Scrollable {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int MIN_TABLE_W   = 160;
    private static final int HEADER_H      = 28;
    private static final int ROW_H         = 20;
    private static final int PADDING       = 8;
    private static final int H_GAP         = 80;   // horizontal gap between tables
    private static final int V_GAP         = 70;   // vertical gap between rows
    private static final int RESIZE_HANDLE = 10;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color C_HEADER_BG = new Color(45, 85, 140);
    private static final Color C_HEADER_FG = Color.WHITE;
    private static final Color C_TABLE_BG  = new Color(30, 30, 45);
    private static final Color C_TABLE_FG  = new Color(220, 220, 230);
    private static final Color C_PK_FG     = new Color(255, 215, 80);
    private static final Color C_FK_FG     = new Color(100, 200, 255);
    private static final Color C_BORDER    = new Color(80, 120, 180);
    private static final Color C_LINE      = new Color(100, 180, 255, 200);
    private static final Color C_LINE_HL   = new Color(255, 200, 80, 230);
    private static final Color C_CANVAS_BG = new Color(18, 18, 28);
    private static final Color C_GRID      = new Color(35, 35, 50);

    // ── Model ─────────────────────────────────────────────────────────────────
    private final List<TableBox> tables    = new ArrayList<>();
    private final List<Relation> relations = new ArrayList<>();

    // ── Viewport ──────────────────────────────────────────────────────────────
    private double zoom = 1.0;
    private double panX = 40;
    private double panY = 40;

    // ── Interaction ───────────────────────────────────────────────────────────
    private TableBox dragging     = null;
    private int      dragOffX, dragOffY;
    private TableBox resizing     = null;
    private int      resizeStartX, resizeStartY, resizeOrigW, resizeOrigH;
    private Point    panStart     = null;
    private double   panStartX, panStartY;
    private TableBox highlighted  = null;
    private boolean  hoverResize  = false;

    // ── Status ────────────────────────────────────────────────────────────────
    private String  statusText = "Loading schema…";
    private boolean loading    = true;

    public SchemaDiagramPanel() {
        setBackground(C_CANVAS_BG);
        setPreferredSize(new Dimension(2400, 1800));
        installMouseHandlers();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void loadSchema(ConnectionInfo info, Connection conn, String schema) {
        loading = true;
        statusText = "Loading schema…";
        tables.clear();
        relations.clear();
        repaint();

        new SwingWorker<Void, Void>() {
            final List<TableBox> tmpTables    = new ArrayList<>();
            final List<Relation> tmpRelations = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    SchemaExplorerService svc = new SchemaExplorerService();
                    DatabaseType dbType = info.getDbType();
                    List<String> tableNames = svc.getTables(conn, dbType, schema);
                    for (String tbl : tableNames) {
                        List<String>   pks  = svc.getPrimaryKeys(conn, dbType, schema, tbl);
                        List<String[]> cols = svc.getColumnDetails(conn, dbType, schema, tbl);
                        List<String[]> fks  = svc.getImportedForeignKeys(conn, dbType, schema, tbl);
                        Set<String> pkSet  = new HashSet<>(pks);
                        Set<String> fkCols = new HashSet<>();
                        for (String[] fk : fks) fkCols.add(fk[1]);
                        TableBox box = new TableBox(tbl);
                        for (String[] col : cols)
                            box.columns.add(new ColInfo(col[0], col[1],
                                    pkSet.contains(col[0]), fkCols.contains(col[0])));
                        tmpTables.add(box);
                        for (String[] fk : fks)
                            tmpRelations.add(new Relation(fk[0], fk[1], fk[2], fk[3]));
                    }
                } catch (Exception e) {
                    statusText = "Error: " + e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                tables.addAll(tmpTables);
                relations.addAll(tmpRelations);
                computeNaturalWidths();
                autoLayout();
                loading = false;
                statusText = tables.size() + " tables, " + relations.size() + " relationships";
                updatePreferredSize();
                fitToView();
                repaint();
            }
        }.execute();
    }

    public void fitToView() {
        if (tables.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (TableBox t : tables) {
            minX = Math.min(minX, t.x);  minY = Math.min(minY, t.y);
            maxX = Math.max(maxX, t.x + t.w); maxY = Math.max(maxY, t.y + t.height());
        }
        int w = getWidth()  > 0 ? getWidth()  : 1200;
        int h = getHeight() > 0 ? getHeight() : 800;
        double zx = (w - 80.0) / Math.max(1, maxX - minX);
        double zy = (h - 80.0) / Math.max(1, maxY - minY);
        zoom = Math.min(Math.min(zx, zy), 1.4);
        panX = 40 - minX * zoom;
        panY = 40 - minY * zoom;
        repaint();
    }

    // ── Preferred size for JScrollPane ────────────────────────────────────────

    private void updatePreferredSize() {
        if (tables.isEmpty()) return;
        int maxX = 0, maxY = 0;
        for (TableBox t : tables) {
            maxX = Math.max(maxX, t.x + t.w);
            maxY = Math.max(maxY, t.y + t.height());
        }
        // Add margin and scale by zoom
        int pw = (int) (maxX * zoom + panX + 80);
        int ph = (int) (maxY * zoom + panY + 80);
        setPreferredSize(new Dimension(Math.max(pw, 800), Math.max(ph, 600)));
        revalidate();
    }

    // ── Scrollable interface ──────────────────────────────────────────────────

    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(1100, 750); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return o == SwingConstants.VERTICAL ? r.height : r.width;
    }
    @Override public boolean getScrollableTracksViewportWidth()  { return false; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    // ── Natural width calculation ─────────────────────────────────────────────

    private void computeNaturalWidths() {
        BufferedImage tmp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = tmp.createGraphics();
        Font nameFont = new Font(Font.MONOSPACED, Font.BOLD,  11);
        Font typeFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        Font hdrFont  = new Font(Font.SANS_SERIF, Font.BOLD,  12);
        for (TableBox t : tables) {
            int maxW = MIN_TABLE_W;
            g2.setFont(hdrFont);
            maxW = Math.max(maxW, g2.getFontMetrics().stringWidth(t.name) + PADDING * 2 + 4);
            for (ColInfo col : t.columns) {
                g2.setFont(nameFont);
                int nameW = g2.getFontMetrics().stringWidth(col.name);
                g2.setFont(typeFont);
                int typeW = g2.getFontMetrics().stringWidth(col.type != null ? col.type : "");
                maxW = Math.max(maxW, PADDING + 22 + nameW + 8 + typeW + PADDING);
            }
            t.w = maxW + 8;
        }
        g2.dispose();
    }

    // ── Auto layout — layered + force-directed spread ────────────────────────

    /**
     * Two-phase layout:
     * Phase 1 – Layered placement: tables with FK relations are arranged in
     *   topological layers (referenced tables on the left, referencing on the right).
     *   Each layer is spread vertically with generous gaps.
     * Phase 2 – Force-directed relaxation: repulsion between all table pairs
     *   pushes overlapping boxes apart; FK attraction pulls related tables closer.
     *   This gives a natural, dispersed look instead of a rigid grid.
     * Isolated tables (no relations) are placed in a grid to the right.
     */
    private void autoLayout() {
        if (tables.isEmpty()) return;

        // ── Phase 1: Layered seed positions ──────────────────────────────────
        Map<String, Set<String>> deps = new HashMap<>();
        for (TableBox t : tables) deps.put(t.name, new HashSet<>());
        for (Relation r : relations) {
            if (deps.containsKey(r.fromTable)) deps.get(r.fromTable).add(r.toTable);
        }

        Map<String, Integer> layer = new HashMap<>();
        for (TableBox t : tables) layer.put(t.name, 0);
        boolean changed = true;
        for (int pass = 0; pass < tables.size() && changed; pass++) {
            changed = false;
            for (Relation r : relations) {
                if (!layer.containsKey(r.fromTable) || !layer.containsKey(r.toTable)) continue;
                int newLayer = layer.get(r.toTable) + 1;
                if (newLayer > layer.get(r.fromTable)) {
                    layer.put(r.fromTable, newLayer);
                    changed = true;
                }
            }
        }

        Set<String> connected = new HashSet<>();
        for (Relation r : relations) { connected.add(r.fromTable); connected.add(r.toTable); }

        Map<Integer, List<TableBox>> byLayer = new TreeMap<>();
        List<TableBox> isolated = new ArrayList<>();
        for (TableBox t : tables) {
            if (!connected.contains(t.name)) { isolated.add(t); continue; }
            byLayer.computeIfAbsent(layer.get(t.name), k -> new ArrayList<>()).add(t);
        }
        for (List<TableBox> lst : byLayer.values()) lst.sort(Comparator.comparing(t -> t.name));

        // Assign seed positions: layers go left-to-right (columns), tables in layer go top-to-bottom
        int LAYER_GAP = 120;
        int x = 60;
        for (Map.Entry<Integer, List<TableBox>> entry : byLayer.entrySet()) {
            List<TableBox> col = entry.getValue();
            int maxW = col.stream().mapToInt(t -> t.w).max().orElse(200);
            int y = 60;
            for (TableBox t : col) {
                t.x = x;
                t.y = y;
                y += t.height() + V_GAP + 20;
            }
            x += maxW + LAYER_GAP;
        }

        // Isolated tables: grid to the right of layered section
        if (!isolated.isEmpty()) {
            isolated.sort(Comparator.comparing(t -> t.name));
            int cols = Math.max(1, (int) Math.ceil(Math.sqrt(isolated.size())));
            int col = 0, rowH = 0, iy = 60, ix = x;
            for (TableBox t : isolated) {
                t.x = ix;
                t.y = iy;
                ix += t.w + H_GAP;
                rowH = Math.max(rowH, t.height());
                col++;
                if (col >= cols) {
                    col = 0; ix = x;
                    iy += rowH + V_GAP;
                    rowH = 0;
                }
            }
        }

        // ── Phase 2: Force-directed relaxation (50 iterations) ───────────────
        double[] fx = new double[tables.size()];
        double[] fy = new double[tables.size()];
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < tables.size(); i++) idx.put(tables.get(i).name, i);

        for (int iter = 0; iter < 60; iter++) {
            Arrays.fill(fx, 0); Arrays.fill(fy, 0);

            // Repulsion between all pairs
            for (int i = 0; i < tables.size(); i++) {
                TableBox a = tables.get(i);
                for (int j = i + 1; j < tables.size(); j++) {
                    TableBox b = tables.get(j);
                    double cx1 = a.x + a.w / 2.0, cy1 = a.y + a.height() / 2.0;
                    double cx2 = b.x + b.w / 2.0, cy2 = b.y + b.height() / 2.0;
                    double dx = cx1 - cx2, dy = cy1 - cy2;
                    double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                    // Desired minimum separation
                    double minDist = (a.w + b.w) / 2.0 + H_GAP + 20;
                    if (dist < minDist) {
                        double force = (minDist - dist) * 0.4;
                        fx[i] += (dx / dist) * force;
                        fy[i] += (dy / dist) * force;
                        fx[j] -= (dx / dist) * force;
                        fy[j] -= (dy / dist) * force;
                    }
                }
            }

            // Attraction along FK edges (spring)
            for (Relation r : relations) {
                Integer ai = idx.get(r.fromTable), bi = idx.get(r.toTable);
                if (ai == null || bi == null) continue;
                TableBox a = tables.get(ai), b = tables.get(bi);
                double cx1 = a.x + a.w / 2.0, cy1 = a.y + a.height() / 2.0;
                double cx2 = b.x + b.w / 2.0, cy2 = b.y + b.height() / 2.0;
                double dx = cx2 - cx1, dy = cy2 - cy1;
                double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
                double ideal = a.w / 2.0 + b.w / 2.0 + LAYER_GAP;
                double force = (dist - ideal) * 0.05;
                fx[ai] += (dx / dist) * force;
                fy[ai] += (dy / dist) * force;
                fx[bi] -= (dx / dist) * force;
                fy[bi] -= (dy / dist) * force;
            }

            // Apply forces
            for (int i = 0; i < tables.size(); i++) {
                tables.get(i).x = Math.max(20, tables.get(i).x + (int) fx[i]);
                tables.get(i).y = Math.max(20, tables.get(i).y + (int) fy[i]);
            }
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        g2.translate(panX, panY);
        g2.scale(zoom, zoom);

        if (loading) {
            g2.setColor(C_TABLE_FG);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g2.drawString(statusText, 40, 60);
            g2.dispose();
            return;
        }

        for (Relation r : relations) {
            TableBox from = findTable(r.fromTable), to = findTable(r.toTable);
            if (from != null && to != null)
                drawRelation(g2, r, from, to, from == highlighted || to == highlighted);
        }
        for (TableBox t : tables) drawTable(g2, t, t == highlighted);
        g2.dispose();

        // Status bar — screen space
        Graphics2D gs = (Graphics2D) g.create();
        gs.setColor(new Color(0, 0, 0, 150));
        gs.fillRect(0, getHeight() - 22, getWidth(), 22);
        gs.setColor(new Color(180, 180, 200));
        gs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        gs.drawString(statusText + "   zoom: " + String.format("%.0f%%", zoom * 100)
                + "   Left-drag: move  •  Right-drag: pan  •  Scroll: zoom  •  Corner: resize",
                8, getHeight() - 6);
        gs.dispose();
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(C_GRID);
        int step = 40;
        for (int x = 0; x < getWidth();  x += step) g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += step) g2.drawLine(0, y, getWidth(), y);
    }

    private void drawTable(Graphics2D g2, TableBox t, boolean hl) {
        int x = t.x, y = t.y, w = t.w, h = t.height();

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 4, y + 4, w, h, 8, 8);

        g2.setColor(C_TABLE_BG);
        g2.fillRoundRect(x, y, w, h, 8, 8);

        g2.setColor(hl ? C_LINE_HL : C_BORDER);
        g2.setStroke(new BasicStroke(hl ? 2f : 1.2f));
        g2.drawRoundRect(x, y, w, h, 8, 8);

        GradientPaint gp = new GradientPaint(x, y, C_HEADER_BG, x, y + HEADER_H, C_HEADER_BG.darker());
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, HEADER_H, 8, 8);
        g2.fillRect(x, y + HEADER_H / 2, w, HEADER_H / 2);

        g2.setColor(C_HEADER_FG);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g2.drawString(t.name, x + PADDING, y + HEADER_H - 8);

        g2.setColor(C_BORDER);
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawLine(x, y + HEADER_H, x + w, y + HEADER_H);

        Font pkFont  = new Font(Font.MONOSPACED, Font.BOLD,  11);
        Font colFont = new Font(Font.MONOSPACED, Font.PLAIN, 11);
        Font typFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        int rowY = y + HEADER_H;

        for (ColInfo col : t.columns) {
            g2.setColor(new Color(255, 255, 255, col.isPk ? 12 : 5));
            g2.fillRect(x + 1, rowY, w - 2, ROW_H);

            if (col.isPk) {
                g2.setColor(C_PK_FG);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                g2.drawString("PK", x + PADDING, rowY + ROW_H - 5);
            } else if (col.isFk) {
                g2.setColor(C_FK_FG);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                g2.drawString("FK", x + PADDING, rowY + ROW_H - 5);
            }

            g2.setFont(col.isPk ? pkFont : colFont);
            g2.setColor(col.isPk ? C_PK_FG : col.isFk ? C_FK_FG : C_TABLE_FG);
            g2.drawString(col.name, x + PADDING + 22, rowY + ROW_H - 5);

            if (col.type != null) {
                g2.setFont(typFont);
                g2.setColor(new Color(140, 140, 160));
                int tw = g2.getFontMetrics().stringWidth(col.type);
                g2.drawString(col.type, x + w - tw - PADDING, rowY + ROW_H - 5);
            }

            g2.setColor(new Color(255, 255, 255, 15));
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawLine(x + 1, rowY + ROW_H, x + w - 1, rowY + ROW_H);
            rowY += ROW_H;
        }

        // Resize handle
        g2.setColor(new Color(150, 180, 220, hl ? 180 : 80));
        g2.setStroke(new BasicStroke(1f));
        for (int i = 1; i <= 3; i++) {
            int off = i * 3;
            g2.drawLine(x + w - off, y + h - 2, x + w - 2, y + h - off);
        }
    }

    private void drawRelation(Graphics2D g2, Relation r, TableBox from, TableBox to, boolean hl) {
        int fromY = getColumnMidY(from, r.fromCol);
        int toY   = getColumnMidY(to,   r.toCol);
        boolean fromRight = from.x + from.w / 2 <= to.x + to.w / 2;
        int x1 = fromRight ? from.x + from.w : from.x;
        int x2 = fromRight ? to.x : to.x + to.w;
        int cx = (x1 + x2) / 2;
        CubicCurve2D curve = new CubicCurve2D.Double(x1, fromY, cx, fromY, cx, toY, x2, toY);
        g2.setColor(hl ? C_LINE_HL : C_LINE);
        g2.setStroke(new BasicStroke(hl ? 2f : 1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(curve);
        drawCrowFoot(g2, x1, fromY, fromRight ? -1 : 1, hl);
        drawOneEnd(g2, x2, toY, fromRight ? 1 : -1, hl);
    }

    private void drawCrowFoot(Graphics2D g2, int x, int y, int dir, boolean hl) {
        g2.setColor(hl ? C_LINE_HL : C_LINE);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int d = dir * 10;
        g2.drawLine(x, y, x + d, y);
        g2.drawLine(x + d, y, x + d * 2, y - 6);
        g2.drawLine(x + d, y, x + d * 2, y + 6);
        g2.drawLine(x + d, y, x + d * 2, y);
    }

    private void drawOneEnd(Graphics2D g2, int x, int y, int dir, boolean hl) {
        g2.setColor(hl ? C_LINE_HL : C_LINE);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int d = dir * 8;
        g2.drawLine(x, y, x + d, y);
        g2.drawLine(x + d, y - 6, x + d, y + 6);
    }

    private int getColumnMidY(TableBox t, String colName) {
        int rowY = t.y + HEADER_H;
        for (ColInfo col : t.columns) {
            if (col.name.equalsIgnoreCase(colName)) return rowY + ROW_H / 2;
            rowY += ROW_H;
        }
        return t.y + HEADER_H / 2;
    }

    private TableBox findTable(String name) {
        for (TableBox t : tables) if (t.name.equalsIgnoreCase(name)) return t;
        return null;
    }

    // ── Mouse interaction ─────────────────────────────────────────────────────

    private void installMouseHandlers() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Point world = toWorld(e.getPoint());
                    TableBox hit = hitTest(world);
                    if (hit != null && isOnResizeHandle(world, hit)) {
                        resizing = hit;
                        resizeStartX = e.getX(); resizeStartY = e.getY();
                        resizeOrigW  = hit.w;
                        resizeOrigH  = hit.userH > 0 ? hit.userH : hit.height();
                        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    } else if (hit != null) {
                        dragging = hit;
                        dragOffX = world.x - hit.x; dragOffY = world.y - hit.y;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                } else if (SwingUtilities.isMiddleMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
                    panStart = e.getPoint(); panStartX = panX; panStartY = panY;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                dragging = null; resizing = null; panStart = null;
                setCursor(Cursor.getDefaultCursor());
                updatePreferredSize();
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (resizing != null) {
                    int dx = (int) ((e.getX() - resizeStartX) / zoom);
                    int dy = (int) ((e.getY() - resizeStartY) / zoom);
                    resizing.w     = Math.max(MIN_TABLE_W, resizeOrigW + dx);
                    resizing.userH = Math.max(HEADER_H + ROW_H, resizeOrigH + dy);
                    repaint();
                } else if (dragging != null) {
                    Point world = toWorld(e.getPoint());
                    dragging.x = world.x - dragOffX;
                    dragging.y = world.y - dragOffY;
                    repaint();
                } else if (panStart != null) {
                    panX = panStartX + (e.getX() - panStart.x);
                    panY = panStartY + (e.getY() - panStart.y);
                    updatePreferredSize();
                    repaint();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                Point world = toWorld(e.getPoint());
                TableBox hit = hitTest(world);
                boolean onHandle = hit != null && isOnResizeHandle(world, hit);
                if (hit != highlighted || onHandle != hoverResize) {
                    highlighted = hit; hoverResize = onHandle;
                    if (onHandle)      setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    else if (hit != null) setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    else               setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                double factor  = e.getWheelRotation() < 0 ? 1.1 : 0.9;
                double newZoom = Math.max(0.15, Math.min(3.0, zoom * factor));
                double mx = e.getX(), my = e.getY();
                panX = mx - (mx - panX) * (newZoom / zoom);
                panY = my - (my - panY) * (newZoom / zoom);
                zoom = newZoom;
                updatePreferredSize();
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    private boolean isOnResizeHandle(Point world, TableBox t) {
        return world.x >= t.x + t.w - RESIZE_HANDLE && world.x <= t.x + t.w
            && world.y >= t.y + t.height() - RESIZE_HANDLE && world.y <= t.y + t.height();
    }

    private Point toWorld(Point screen) {
        return new Point((int) ((screen.x - panX) / zoom), (int) ((screen.y - panY) / zoom));
    }

    private TableBox hitTest(Point world) {
        for (int i = tables.size() - 1; i >= 0; i--) {
            TableBox t = tables.get(i);
            if (world.x >= t.x && world.x <= t.x + t.w
                    && world.y >= t.y && world.y <= t.y + t.height()) return t;
        }
        return null;
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    static class ColInfo {
        final String name, type; final boolean isPk, isFk;
        ColInfo(String n, String t, boolean pk, boolean fk) { name=n; type=t; isPk=pk; isFk=fk; }
    }

    static class TableBox {
        final String name; final List<ColInfo> columns = new ArrayList<>();
        int x, y, w = 200, userH = 0;
        TableBox(String n) { name = n; }
        int height() {
            int nat = HEADER_H + columns.size() * ROW_H + 2;
            return userH > 0 ? Math.max(HEADER_H + ROW_H, Math.min(userH, nat)) : nat;
        }
    }

    static class Relation {
        final String fromTable, fromCol, toTable, toCol;
        Relation(String ft, String fc, String tt, String tc) {
            fromTable=ft; fromCol=fc; toTable=tt; toCol=tc;
        }
    }
}
