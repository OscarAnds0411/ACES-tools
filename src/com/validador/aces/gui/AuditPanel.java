package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.validador.aces.models.AuditReport;

/**
 * Panel que muestra el historial de auditorías realizadas en la sesión.
 *
 * <p>Cada vez que {@link ValidationPanel} completa una auditoría llama a
 * {@link MainWindow#addAuditReport(AuditReport)}, y este panel muestra
 * todos los reportes acumulados en la sesión. La tabla incluye timestamp,
 * acción, detalles, resultado y duración.</p>
 *
 * <p>El botón "Exportar historial" guarda la tabla como CSV en el archivo
 * elegido por el usuario.</p>
 */
public class AuditPanel extends JPanel {

    public static final String CARD_NAME = "AUDIT_HISTORY";

    private static final DateTimeFormatter TS_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MainWindow window;
    private final DefaultTableModel tableModel;
    private final JTable table;

    // ── Constructor ───────────────────────────────────────────────────────

    public AuditPanel(MainWindow window) {
        this.window = window;
        window.registerPanel(CARD_NAME, this);

        tableModel = new DefaultTableModel(
            new String[]{"Fecha / Hora", "Acción", "Detalles", "Resultado", "Duración (ms)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = buildTable();

        setBackground(MainWindow.WHITE);
        setLayout(new BorderLayout());
        add(buildNorthBar(),        BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildSouth(),           BorderLayout.SOUTH);
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Agrega un reporte al historial y lo muestra en la tabla.
     * Hilo seguro: puede llamarse desde cualquier hilo (usa invokeLater internamente).
     *
     * @param report reporte a agregar; se ignora si es null
     */
    public void addReport(AuditReport report) {
        if (report == null) return;
        javax.swing.SwingUtilities.invokeLater(() -> {
            String ts = report.getTimestamp() != null
                ? report.getTimestamp().format(TS_FMT) : "—";
            tableModel.addRow(new Object[]{
                ts,
                report.getAction() != null ? report.getAction().toString() : "—",
                report.getDetails() != null ? report.getDetails() : "—",
                report.getResult()  != null ? report.getResult()  : "—",
                report.getDurationMillis()
            });
            // Scroll al final
            int last = tableModel.getRowCount() - 1;
            if (last >= 0) table.scrollRectToVisible(table.getCellRect(last, 0, true));
        });
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private JPanel buildNorthBar() {
        JLabel title = new JLabel("  Historial de Auditorías");
        title.setFont(MainWindow.font(Font.BOLD, 13));
        title.setForeground(MainWindow.WHITE);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(MainWindow.MINE_SHAFT);
        bar.setPreferredSize(new Dimension(0, MainWindow.px(36)));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, MainWindow.FLUSH_ORANGE));
        bar.add(title, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildSouth() {
        JButton btnExport = solidBtn("Exportar historial", MainWindow.FLUSH_ORANGE, MainWindow.CHELSEA_GEM);
        JButton btnClear  = outlineBtn("Limpiar historial", MainWindow.C_ERROR);
        JButton btnBack   = outlineBtn("← Volver",          MainWindow.SCI_BLUE);

        Dimension sz = new Dimension(MainWindow.px(180), MainWindow.px(34));
        btnExport.setPreferredSize(sz);

        btnExport.addActionListener(e -> exportHistory());
        btnClear.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(window,
                    "¿Limpiar todo el historial de auditorías?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                tableModel.setRowCount(0);
                window.clearAuditHistory();
            }
        });
        btnBack.addActionListener(e -> window.showCard(MainWindow.CARD_WELCOME));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, MainWindow.px(8), MainWindow.px(8)));
        bar.setBackground(new Color(0xF8F8F8));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.SILVER));
        bar.add(btnBack);
        bar.add(btnExport);
        bar.add(btnClear);
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
        t.getColumnModel().getColumn(0).setPreferredWidth(140);
        t.getColumnModel().getColumn(1).setPreferredWidth(100);
        t.getColumnModel().getColumn(2).setPreferredWidth(260);
        t.getColumnModel().getColumn(3).setPreferredWidth(260);
        t.getColumnModel().getColumn(4).setPreferredWidth(100);
        return t;
    }

    // ── Exportación CSV ───────────────────────────────────────────────────

    private void exportHistory() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(window,
                "No hay registros en el historial para exportar.",
                "Historial vacío", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File out = FileDialogHelper.chooseExcelSave(window,
            "Guardar historial de auditorías",
            new File(ConfigDialog.getDefaultExportDir(), "historial_auditoria.xlsx"));
        // Guardar como CSV (extensión .xlsx no es estrictamente correcta pero el usuario lo pide)
        if (out == null) return;
        // Usar .csv como archivo de texto plano
        File csv = new File(out.getAbsolutePath().replaceAll("\\.xlsx$", ".csv"));
        try (FileWriter fw = new FileWriter(csv)) {
            // Header
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) sb.append(";");
                sb.append(tableModel.getColumnName(c));
            }
            fw.write(sb.toString() + "\n");
            // Rows
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                sb = new StringBuilder();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) sb.append(";");
                    Object v = tableModel.getValueAt(r, c);
                    sb.append(v != null ? v.toString().replace(";", ",") : "");
                }
                fw.write(sb.toString() + "\n");
            }
            JOptionPane.showMessageDialog(window,
                "Historial exportado:\n" + csv.getAbsolutePath(),
                "Exportación exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(window,
                "No se pudo exportar el historial:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Fábrica de componentes ────────────────────────────────────────────

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
        return btn;
    }
}