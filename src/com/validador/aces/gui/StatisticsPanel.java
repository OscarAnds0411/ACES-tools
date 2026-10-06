package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.validador.aces.comparison.ComplianceCalculator;
import com.validador.aces.comparison.ComplianceMetrics;
import com.validador.aces.models.ComparisonResult;

/**
 * Panel de estadísticas detalladas de compliance por línea de producto.
 *
 * <p>Se actualiza automáticamente cada vez que se muestra: utiliza un
 * {@link ComponentAdapter} que llama a {@link #refresh()} cuando el panel
 * se vuelve visible, leyendo los resultados almacenados en
 * {@link MainWindow#getLastComparisonResults()}.</p>
 *
 * <p>La tabla solo muestra líneas de producto con al menos un atributo
 * faltante (ordenadas de menor a mayor compliance), seguidas de un
 * resumen de las líneas con 100 % de compliance para no saturar la UI
 * con decenas de miles de filas.</p>
 */
public class StatisticsPanel extends JPanel {

    public static final String CARD_NAME = "STATISTICS";

    private static final int MAX_ROWS = 5_000;

    private final MainWindow window;
    private final ComplianceCalculator calculator = new ComplianceCalculator();

    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JLabel            lblTitle;
    private final JLabel            lblSummary;

    // ── Constructor ───────────────────────────────────────────────────────

    public StatisticsPanel(MainWindow window) {
        this.window = window;
        window.registerPanel(CARD_NAME, this);

        tableModel = new DefaultTableModel(
            new String[]{"Línea de Producto", "Aplicaciones",
                         "Req. Totales", "Req. Satisfechos", "Compliance %"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table    = buildTable();
        lblTitle = buildHeaderLabel();
        lblSummary = new JLabel("  —");
        lblSummary.setFont(MainWindow.font(Font.ITALIC, 11));
        lblSummary.setForeground(MainWindow.MID_GRAY);

        setBackground(MainWindow.WHITE);
        setLayout(new BorderLayout());
        add(buildNorth(),            BorderLayout.NORTH);
        add(new JScrollPane(table),  BorderLayout.CENTER);
        add(buildSouth(),            BorderLayout.SOUTH);

        // ── Auto-refresh al hacerse visible ──────────────────────────────
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) { refresh(); }
        });
    }

    // ── Refresh ───────────────────────────────────────────────────────────

    /** Recarga la tabla con los últimos resultados de auditoría disponibles. */
    public void refresh() {
        Map<String, List<ComparisonResult>> byLine = window.getLastComparisonResults();
        tableModel.setRowCount(0);

        if (byLine == null || byLine.isEmpty()) {
            lblTitle.setText("  Sin resultados — ejecute una auditoría primero.");
            lblSummary.setText("");
            return;
        }

        Map<String, ComplianceMetrics> metrics = calculator.calculateByProductLine(byLine);

        // Separar: con errores vs. sin errores
        List<Object[]> withErrors   = new ArrayList<>();
        int compliantLines = 0;

        for (Map.Entry<String, ComplianceMetrics> e : metrics.entrySet()) {
            ComplianceMetrics m = e.getValue();
            if (m.getUnmetRequiredAttributes() > 0) {
                int appCount = byLine.get(e.getKey()).size();
                withErrors.add(new Object[]{
                    e.getKey(),
                    appCount,
                    m.getTotalRequiredAttributes(),
                    m.getSatisfiedRequiredAttributes(),
                    String.format("%.1f%%", m.getCompliancePercentage())
                });
            } else {
                compliantLines++;
            }
        }

        // Ordenar por compliance ascendente
        withErrors.sort((a, b) -> {
            double ca = parseCompliance((String) a[4]);
            double cb = parseCompliance((String) b[4]);
            return Double.compare(ca, cb);
        });

        int shown = Math.min(withErrors.size(), MAX_ROWS);
        for (int i = 0; i < shown; i++) tableModel.addRow(withErrors.get(i));

        // Resumen
        String title = withErrors.isEmpty()
            ? "  ✔  Todas las líneas de producto cumplen con los atributos requeridos."
            : "  " + fmt(shown) + " líneas con atributos faltantes"
                + (withErrors.size() > MAX_ROWS ? "  (mostrando primeras " + fmt(MAX_ROWS) + ")" : "");
        lblTitle.setText(title);
        lblTitle.setForeground(withErrors.isEmpty() ? MainWindow.SCI_BLUE : MainWindow.MINE_SHAFT);

        lblSummary.setText("  " + fmt(compliantLines) + " líneas adicionales con 100% compliance no se muestran.");
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private JPanel buildNorth() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(MainWindow.MINE_SHAFT);
        bar.setPreferredSize(new Dimension(0, MainWindow.px(36)));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, MainWindow.FLUSH_ORANGE));

        JLabel title = new JLabel("  Estadísticas por Línea de Producto");
        title.setFont(MainWindow.font(Font.BOLD, 13));
        title.setForeground(MainWindow.WHITE);
        bar.add(title, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout());
        info.setBackground(MainWindow.WHITE);
        info.add(lblTitle,   BorderLayout.NORTH);
        info.add(lblSummary, BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(bar,  BorderLayout.NORTH);
        north.add(info, BorderLayout.CENTER);
        return north;
    }

    private JPanel buildSouth() {
        JButton btnBack = outlineBtn("← Volver a Resultados", MainWindow.SCI_BLUE);
        btnBack.addActionListener(e -> window.showCard(MainWindow.CARD_RESULTS));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, MainWindow.px(12), MainWindow.px(8)));
        bar.setBackground(new Color(0xF8F8F8));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.SILVER));
        bar.add(btnBack);
        return bar;
    }

    // ── Tabla ─────────────────────────────────────────────────────────────

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setFont(MainWindow.font(Font.PLAIN, 11));
        t.setRowHeight(MainWindow.px(22));
        t.setGridColor(new Color(0xEEEEEE));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setBackground(MainWindow.WHITE);
        t.setSelectionBackground(new Color(0xEAF3FB));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        t.getTableHeader().setFont(MainWindow.font(Font.BOLD, 11));
        t.getTableHeader().setBackground(new Color(0xF2F2F2));
        t.getTableHeader().setForeground(MainWindow.MINE_SHAFT);
        t.getTableHeader().setReorderingAllowed(false);

        t.getColumnModel().getColumn(0).setPreferredWidth(280);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);
        t.getColumnModel().getColumn(2).setPreferredWidth(100);
        t.getColumnModel().getColumn(3).setPreferredWidth(120);
        t.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Colorear columna Compliance %
        DefaultTableCellRenderer compRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                if (!sel && val != null) {
                    double v = parseCompliance(val.toString());
                    setForeground(v < 50 ? MainWindow.C_ERROR
                                : v < 100 ? MainWindow.FLUSH_ORANGE
                                : MainWindow.SCI_BLUE);
                    setFont(MainWindow.font(Font.BOLD, 11));
                }
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return this;
            }
        };
        t.getColumnModel().getColumn(4).setCellRenderer(compRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        t.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        t.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        t.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        return t;
    }

    private static JLabel buildHeaderLabel() {
        JLabel l = new JLabel("  Ejecute una auditoría para ver las estadísticas.");
        l.setFont(MainWindow.font(Font.BOLD, 11));
        l.setForeground(MainWindow.MID_GRAY);
        l.setBorder(BorderFactory.createEmptyBorder(
            MainWindow.px(6), 0, MainWindow.px(4), 0));
        return l;
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private static double parseCompliance(String s) {
        if (s == null) return 0;
        try { return Double.parseDouble(s.replace("%", "").replace(",", ".").trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String fmt(int n) {
        return NumberFormat.getNumberInstance(new Locale("es", "ES")).format(n);
    }

    private static JButton outlineBtn(String label, Color c) {
        JButton btn = new JButton(label);
        btn.setFont(MainWindow.font(Font.PLAIN, 12));
        btn.setForeground(c);
        btn.setBackground(MainWindow.WHITE);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(c, 1),
            BorderFactory.createEmptyBorder(
                MainWindow.px(6), MainWindow.px(12), MainWindow.px(6), MainWindow.px(12))));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xEAF3FB)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(MainWindow.WHITE); }
        });
        return btn;
    }
}