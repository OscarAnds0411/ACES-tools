package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.validador.aces.comparison.Comparator;
import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;
import com.validador.aces.reporting.ExcelReportGenerator;
import com.validador.aces.validation.AttributeValidator;

/**
 * Panel de resultados de auditoría ACES.
 *
 * <p>Se activa cuando el usuario hace clic en "Ejecutar auditoría". Muestra:</p>
 * <ul>
 *   <li>Métricas agregadas: aplicaciones auditadas, compliance promedio,
 *       con errores, sin clasificar.</li>
 *   <li>Tabla con el detalle de atributos faltantes (una fila por error),
 *       limitada a {@value #MAX_TABLE_ROWS} filas para no saturar la UI.</li>
 *   <li>Botón de exportación que genera el reporte Excel mediante
 *       {@link ExcelReportGenerator#generateAndWriteBatch}.</li>
 * </ul>
 */
public class ValidationPanel extends JPanel {

    /** Máximo de filas mostradas en la tabla de errores. */
    private static final int MAX_TABLE_ROWS = 10_000;

    // Estados internos del panel
    private static final String ST_IDLE    = "IDLE";
    private static final String ST_RUNNING = "RUNNING";
    private static final String ST_RESULTS = "RESULTS";

    private final MainWindow window;

    // ── Métricas ──────────────────────────────────────────────────────────
    private final JLabel metricTotal       = metric("—");
    private final JLabel metricCompliant   = metric("—");
    private final JLabel metricErrors      = metric("—");
    private final JLabel metricUnclassified= metric("—");
    private final JLabel metricAvg         = metric("—");

    // ── Tabla de errores ──────────────────────────────────────────────────
    private final DefaultTableModel tableModel;
    private final JTable            table;
    private final JLabel            lblTableTitle = new JLabel();

    // ── Estado ────────────────────────────────────────────────────────────
    private final com.validador.aces.comparison.Comparator comparator = new com.validador.aces.comparison.Comparator("auditoria");
    private List<ComparisonResult>            lastResults;
    private Map<String, List<ComparisonResult>> lastByProductLine;

    // ── Paneles de estado ─────────────────────────────────────────────────
    private final JPanel  stateIdle;
    private final JPanel  stateRunning;
    private final JPanel  stateResults;
    private String        currentState = ST_IDLE;

    // ── Constructor ───────────────────────────────────────────────────────

    public ValidationPanel(MainWindow window) {
        this.window = window;
        window.registerPanel(MainWindow.CARD_RESULTS, this);
        window.getBtnRun().addActionListener(e -> {
            window.showCard(MainWindow.CARD_RESULTS);
            startValidation();
        });

        // Tabla
        tableModel = new DefaultTableModel(
            new String[]{"Aplicación", "Línea de Producto", "Atributo Faltante", "Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = buildTable();

        // Paneles de estado
        stateIdle    = buildIdlePanel();
        stateRunning = buildRunningPanel();
        stateResults = buildResultsPanel();

        setBackground(MainWindow.WHITE);
        setLayout(new BorderLayout());
        add(buildNorthBar(), BorderLayout.NORTH);
        add(stateIdle, BorderLayout.CENTER);
    }

    // ── Layout ────────────────────────────────────────────────────────────

    /** Barra superior: título de sección y métricas (ocultas hasta tener resultados). */
    private JPanel buildNorthBar() {
        JLabel title = new JLabel("  Resultados de Auditoría");
        title.setFont(MainWindow.font(Font.BOLD, 13));
        title.setForeground(MainWindow.WHITE);

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(MainWindow.MINE_SHAFT);
        titleBar.setPreferredSize(new Dimension(0, MainWindow.px(36)));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, MainWindow.FLUSH_ORANGE));
        titleBar.add(title, BorderLayout.WEST);

        return titleBar;
    }

    private JPanel buildIdlePanel() {
        JLabel msg = new JLabel(
            "<html><center style='color:#6c6c6d'>"
            + "Cargue el catálogo y el archivo ACES,<br>"
            + "luego haga clic en <b>Ejecutar auditoría</b>."
            + "</center></html>", JLabel.CENTER);
        msg.setFont(MainWindow.font(Font.PLAIN, 13));

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(MainWindow.WHITE);
        p.add(msg, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRunningPanel() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(MainWindow.px(340), MainWindow.px(10)));
        bar.setBackground(new Color(0xEEEEEE));
        bar.setForeground(MainWindow.SCI_BLUE);
        bar.setBorderPainted(false);

        JLabel lbl = new JLabel("Comparando aplicaciones…  Por favor espere.", JLabel.CENTER);
        lbl.setFont(MainWindow.font(Font.ITALIC, 12));
        lbl.setForeground(MainWindow.MID_GRAY);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(MainWindow.WHITE);
        inner.add(Box.createVerticalGlue());
        bar.setAlignmentX(CENTER_ALIGNMENT);
        lbl.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(lbl);
        inner.add(Box.createVerticalStrut(MainWindow.px(16)));
        inner.add(bar);
        inner.add(Box.createVerticalGlue());

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(MainWindow.WHITE);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildResultsPanel() {
        // ── Métricas ──────────────────────────────────────────────────────
        JPanel metricsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, MainWindow.px(6), 0));
        metricsRow.setBackground(new Color(0xF8F8F8));
        metricsRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.SILVER),
            BorderFactory.createEmptyBorder(MainWindow.px(10), MainWindow.px(16),
                                            MainWindow.px(10), MainWindow.px(16))));

        metricsRow.add(metricBox("Total",        metricTotal,       MainWindow.MINE_SHAFT));
        metricsRow.add(metricSep());
        metricsRow.add(metricBox("100% compliant", metricCompliant, MainWindow.SCI_BLUE));
        metricsRow.add(metricSep());
        metricsRow.add(metricBox("Con errores",  metricErrors,      MainWindow.C_ERROR));
        metricsRow.add(metricSep());
        metricsRow.add(metricBox("Sin clasificar", metricUnclassified, MainWindow.MID_GRAY));
        metricsRow.add(metricSep());
        metricsRow.add(metricBox("Compliance prom.", metricAvg,     MainWindow.FLUSH_ORANGE));

        // ── Tabla ─────────────────────────────────────────────────────────
        lblTableTitle.setFont(MainWindow.font(Font.BOLD, 11));
        lblTableTitle.setForeground(MainWindow.MID_GRAY);
        lblTableTitle.setBorder(BorderFactory.createEmptyBorder(
            MainWindow.px(8), MainWindow.px(16), MainWindow.px(6), MainWindow.px(16)));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.SILVER));
        scroll.getViewport().setBackground(MainWindow.WHITE);

        // ── Botones ───────────────────────────────────────────────────────
        JButton btnExport = solidBtn("Exportar reporte", MainWindow.FLUSH_ORANGE, MainWindow.CHELSEA_GEM);
        JButton btnNew    = outlineBtn("Nueva auditoría", MainWindow.SCI_BLUE);

        Dimension sz = new Dimension(MainWindow.px(190), MainWindow.px(36));
        btnExport.setPreferredSize(sz); btnExport.setMaximumSize(sz);
        btnNew.setPreferredSize(new Dimension(MainWindow.px(160), MainWindow.px(36)));
        btnNew.setMaximumSize(new Dimension(MainWindow.px(160), MainWindow.px(36)));

        btnExport.addActionListener(e -> exportReport());
        JButton btnStats = outlineBtn("Estadísticas", MainWindow.SCI_BLUE);
        btnStats.setPreferredSize(new Dimension(MainWindow.px(130), MainWindow.px(36)));
        btnStats.setMaximumSize(new Dimension(MainWindow.px(130), MainWindow.px(36)));
        btnStats.addActionListener(e -> window.showCard(StatisticsPanel.CARD_NAME));
        btnNew.addActionListener(e -> {
            window.showCard(MainWindow.CARD_WELCOME);
            window.setStatus("Listo para una nueva auditoría.");
        });

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, MainWindow.px(10), MainWindow.px(8)));
        btnBar.setBackground(new Color(0xF8F8F8));
        btnBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.SILVER));
        btnBar.add(btnExport);
        btnBar.add(btnStats);
        btnBar.add(btnNew);

        // ── Composición ───────────────────────────────────────────────────
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(MainWindow.WHITE);
        center.add(lblTableTitle, BorderLayout.NORTH);
        center.add(scroll,        BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(MainWindow.WHITE);
        p.add(metricsRow, BorderLayout.NORTH);
        p.add(center,     BorderLayout.CENTER);
        p.add(btnBar,     BorderLayout.SOUTH);
        return p;
    }

    // ── Componentes de tabla ──────────────────────────────────────────────

    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setFont(MainWindow.font(Font.PLAIN, 11));
        t.setRowHeight(MainWindow.px(22));
        t.setGridColor(new Color(0xEEEEEE));
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setBackground(MainWindow.WHITE);
        t.setSelectionBackground(new Color(0xEAF3FB));
        t.setSelectionForeground(MainWindow.MINE_SHAFT);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        // Anchos de columna
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(1).setPreferredWidth(200);
        t.getColumnModel().getColumn(2).setPreferredWidth(160);
        t.getColumnModel().getColumn(3).setPreferredWidth(100);

        // Header
        t.getTableHeader().setFont(MainWindow.font(Font.BOLD, 11));
        t.getTableHeader().setBackground(new Color(0xF2F2F2));
        t.getTableHeader().setForeground(MainWindow.MINE_SHAFT);
        t.getTableHeader().setReorderingAllowed(false);

        // Renderer que colorea la columna Estado
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                String v = val != null ? val.toString() : "";
                if (!sel) {
                    if ("Faltante".equals(v))      setForeground(MainWindow.C_ERROR);
                    else if ("Sin clasificar".equals(v)) setForeground(MainWindow.MID_GRAY);
                    else setForeground(MainWindow.MINE_SHAFT);
                }
                setFont(MainWindow.font(Font.BOLD, 11));
                return this;
            }
        };
        t.getColumnModel().getColumn(3).setCellRenderer(statusRenderer);
        return t;
    }

    // ── Lógica de validación ──────────────────────────────────────────────

    private void startValidation() {
        switchState(ST_RUNNING);
        tableModel.setRowCount(0);
        window.setStatus("Ejecutando auditoría…  Por favor espere.");
        window.setButtonsEnabled(false);

        final Catalog            catalog = window.getLoadedCatalog();
        final List<Application>  apps    = window.getLoadedApplications();

        if (catalog == null || apps == null) {
            JOptionPane.showMessageDialog(window,
                "Cargue el catálogo y el archivo ACES antes de ejecutar la auditoría.",
                "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            switchState(ST_IDLE);
            window.setButtonsEnabled(true);
            return;
        }

        new SwingWorker<List<ComparisonResult>, Void>() {
            @Override
            protected List<ComparisonResult> doInBackground() {
                return comparator.compareAll(apps, catalog);
            }

            @Override
            protected void done() {
                try {
                    List<ComparisonResult> results = get();
                    buildResultsMap(results, apps);
                    populateUI(results, apps);
                    window.setLastComparisonResults(lastByProductLine);
                    com.validador.aces.models.AuditReport audit = comparator.getLastAuditReport();
                    if (audit != null) window.addAuditReport(audit);
                    switchState(ST_RESULTS);
                    window.setStatus("✔  Auditoría completada — " + fmt(results.size()) + " aplicaciones.", MainWindow.C_OK);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(window,
                        "Error durante la auditoría:\n\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    window.setStatus("✘  Error durante la auditoría.", MainWindow.C_ERROR);
                    switchState(ST_IDLE);
                } finally {
                    window.setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void buildResultsMap(List<ComparisonResult> results, List<Application> apps) {
        lastResults = results;
        lastByProductLine = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            String key = apps.get(i).getProductName();
            if (key == null || key.trim().isEmpty()) key = "(sin producto)";
            lastByProductLine.computeIfAbsent(key, k -> new ArrayList<>()).add(results.get(i));
        }
    }

    private void populateUI(List<ComparisonResult> results, List<Application> apps) {
        int total = results.size(), compliant = 0, withErrors = 0, unclassified = 0;
        double sumCompliance = 0;
        int applicableCount = 0;

        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ComparisonResult r = results.get(i);
            boolean isUnclassified = !r.getWarnings().isEmpty() &&
                r.getWarnings().stream().anyMatch(w ->
                    AttributeValidator.PRODUCT_LINE_NOT_FOUND.equals(w.getCode()));

            if (isUnclassified) {
                unclassified++;
                if (rows.size() < MAX_TABLE_ROWS) {
                    rows.add(new Object[]{
                        apps.get(i).getName(),
                        apps.get(i).getProductName() != null ? apps.get(i).getProductName() : "-",
                        "-",
                        "Sin clasificar"
                    });
                }
            } else if (!r.isApplicable()) {
                // Sin atributos requeridos: N/A, no cuenta en promedio ni en 100 % compliant
                continue;
            } else if (r.getErrorCount() > 0) {
                sumCompliance += r.getCompliancePercentage();
                applicableCount++;
                withErrors++;
                for (ValidationError err : r.getErrors()) {
                    if (!AttributeValidator.MISSING_REQUIRED_ATTRIBUTE.equals(err.getCode())) continue;
                    if (rows.size() < MAX_TABLE_ROWS) {
                        rows.add(new Object[]{
                            apps.get(i).getName(),
                            err.getProductLine() != null ? err.getProductLine() : "-",
                            err.getAttributeName() != null ? err.getAttributeName() : "-",
                            "Faltante"
                        });
                    }
                }
            } else {
                sumCompliance += r.getCompliancePercentage();
                applicableCount++;
                compliant++;
            }
        }

        // Promedio solo sobre aplicaciones clasificadas y con atributos requeridos
        String avg = applicableCount > 0 ? String.format("%.1f%%", sumCompliance / applicableCount) : "N/A";

        // Métricas
        metricTotal.setText(fmt(total));
        metricCompliant.setText(fmt(compliant));
        metricErrors.setText(fmt(withErrors));
        metricUnclassified.setText(fmt(unclassified));
        metricAvg.setText(avg);

        // Tabla
        for (Object[] row : rows) tableModel.addRow(row);

        if (rows.isEmpty()) {
            lblTableTitle.setText("  ✔  Ningún atributo faltante — todas las aplicaciones clasificadas cumplen con los atributos requeridos.");
            lblTableTitle.setForeground(MainWindow.SCI_BLUE);
        } else {
            int shown = Math.min(rows.size(), MAX_TABLE_ROWS);
            lblTableTitle.setText("  " + fmt(shown) + " registros" +
                (rows.size() >= MAX_TABLE_ROWS ? "  (mostrando primeros " + fmt(MAX_TABLE_ROWS) + ")" : ""));
            lblTableTitle.setForeground(MainWindow.MID_GRAY);
        }
    }

    // ── Exportación ───────────────────────────────────────────────────────

    private void exportReport() {
        if (lastByProductLine == null || lastByProductLine.isEmpty()) {
            JOptionPane.showMessageDialog(window, "No hay resultados para exportar.",
                "Sin datos", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Sugerir nombre basado en el archivo ACES cargado
        File acesFile = window.getLoadedAcesFile();
        File defaultOut = acesFile != null
            ? ExcelReportGenerator.defaultReportFileFor(acesFile)
            : new File(System.getProperty("user.home"), "Reporte_Auditoria_ACES.xlsx");

        JFileChooser chooser = new JFileChooser(defaultOut.getParentFile());
        chooser.setDialogTitle("Guardar reporte de auditoría");
        chooser.setSelectedFile(defaultOut);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (.xlsx)", "xlsx"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return;

        File outputFile = chooser.getSelectedFile();
        if (!outputFile.getName().toLowerCase().endsWith(".xlsx"))
            outputFile = new File(outputFile.getAbsolutePath() + ".xlsx");

        final File finalOut = outputFile;
        window.setStatus("Generando reporte Excel…");
        window.setButtonsEnabled(false);

        final Map<String, List<ComparisonResult>> snapshot = new LinkedHashMap<>(lastByProductLine);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new ExcelReportGenerator().generateAndWriteBatch(snapshot, finalOut);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    window.setStatus("✔  Reporte generado: " + finalOut.getAbsolutePath(), MainWindow.C_OK);
                    JOptionPane.showMessageDialog(window,
                        "Reporte guardado correctamente en:\n" + finalOut.getAbsolutePath(),
                        "Exportación exitosa", JOptionPane.INFORMATION_MESSAGE);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    JOptionPane.showMessageDialog(window,
                        "Error al exportar:\n\n" + (cause != null ? cause.getMessage() : ex.getMessage()),
                        "Error de exportación", JOptionPane.ERROR_MESSAGE);
                    window.setStatus("✘  Error al exportar el reporte.", MainWindow.C_ERROR);
                } catch (Exception ex) {
                    window.setStatus("✘  Error inesperado.", MainWindow.C_ERROR);
                } finally {
                    window.setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    // ── Cambio de estado ──────────────────────────────────────────────────

    private void switchState(String state) {
        if (state.equals(currentState)) return;
        remove(stateIdle); remove(stateRunning); remove(stateResults);
        switch (state) {
            case ST_IDLE:    add(stateIdle,    BorderLayout.CENTER); break;
            case ST_RUNNING: add(stateRunning, BorderLayout.CENTER); break;
            case ST_RESULTS: add(stateResults, BorderLayout.CENTER); break;
        }
        currentState = state;
        revalidate(); repaint();
    }

    // ── Fábrica de componentes ────────────────────────────────────────────

    private static JLabel metric(String text) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setFont(MainWindow.font(Font.BOLD, 20));
        l.setForeground(MainWindow.MINE_SHAFT);
        return l;
    }

    private JPanel metricBox(String label, JLabel value, Color valueColor) {
        value.setForeground(valueColor);
        JLabel lbl = new JLabel(label, JLabel.CENTER);
        lbl.setFont(MainWindow.font(Font.PLAIN, 10));
        lbl.setForeground(MainWindow.MID_GRAY);

        JPanel box = new JPanel(new BorderLayout(0, 2));
        box.setBackground(new Color(0xF8F8F8));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainWindow.SILVER, 1),
            BorderFactory.createEmptyBorder(
                MainWindow.px(8), MainWindow.px(16), MainWindow.px(8), MainWindow.px(16))));
        box.add(value, BorderLayout.CENTER);
        box.add(lbl,   BorderLayout.SOUTH);
        return box;
    }

    private static JPanel metricSep() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(MainWindow.px(6), 1));
        return p;
    }

    private static JButton solidBtn(String label, Color bg, Color hover) {
        JButton btn = new JButton(label);
        btn.setFont(MainWindow.font(Font.PLAIN, 12));
        btn.setForeground(MainWindow.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private static JButton outlineBtn(String label, Color color) {
        JButton btn = new JButton(label);
        btn.setFont(MainWindow.font(Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setBackground(MainWindow.WHITE);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 1),
            BorderFactory.createEmptyBorder(MainWindow.px(7), MainWindow.px(14),
                                            MainWindow.px(7), MainWindow.px(14))));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xEAF3FB)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(MainWindow.WHITE); }
        });
        return btn;
    }

    private static String fmt(int n) {
        return NumberFormat.getNumberInstance(new Locale("es", "ES")).format(n);
    }
}