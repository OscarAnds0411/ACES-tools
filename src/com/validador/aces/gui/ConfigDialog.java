package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Diálogo modal de configuración de la aplicación.
 *
 * <p>Permite al usuario ajustar opciones básicas que se persisten en un
 * archivo {@code .properties} dentro de su directorio de usuario. Las
 * opciones surten efecto inmediatamente al guardar; no es necesario
 * reiniciar la aplicación.</p>
 *
 * <h2>Opciones disponibles</h2>
 * <ul>
 *   <li><b>Máximo de filas en la tabla de resultados</b> (1&nbsp;000 – 50&nbsp;000).</li>
 *   <li><b>Directorio de exportación por defecto</b>.</li>
 * </ul>
 */
public class ConfigDialog extends JDialog {

    // ── Archivo de propiedades ─────────────────────────────────────────────
    private static final File CONFIG_FILE = new File(
            System.getProperty("user.home"), ".validador_aces_config.properties");

    private static final String KEY_MAX_ROWS   = "maxTableRows";
    private static final String KEY_EXPORT_DIR = "defaultExportDir";

    // ── Valores por defecto ────────────────────────────────────────────────
    private static final int     DEFAULT_MAX_ROWS   = 10_000;
    private static final String  DEFAULT_EXPORT_DIR = System.getProperty("user.home");

    // ── Componentes de edición ─────────────────────────────────────────────
    private final JSpinner   spnMaxRows;
    private final JTextField txfExportDir;

    // ── Constructor ────────────────────────────────────────────────────────

    public ConfigDialog(MainWindow owner) {
        super(owner, "Configuración", true);
        setUndecorated(false);
        setResizable(false);

        Properties props = load();

        int    maxRows   = parseInt(props.getProperty(KEY_MAX_ROWS),   DEFAULT_MAX_ROWS);
        String exportDir = props.getProperty(KEY_EXPORT_DIR, DEFAULT_EXPORT_DIR);

        spnMaxRows   = new JSpinner(new SpinnerNumberModel(maxRows, 1_000, 50_000, 1_000));
        txfExportDir = new JTextField(exportDir, 32);

        setLayout(new BorderLayout());
        add(buildTitleBar("Configuración"), BorderLayout.NORTH);
        add(buildForm(),                    BorderLayout.CENTER);
        add(buildButtons(),                 BorderLayout.SOUTH);

        getRootPane().setBorder(BorderFactory.createLineBorder(MainWindow.MINE_SHAFT, 1));
        pack();
        setLocationRelativeTo(owner);
    }

    // ── API estática para leer opciones ───────────────────────────────────

    /** @return máximo de filas para la tabla de resultados de auditoría. */
    public static int getMaxTableRows() {
        return parseInt(load().getProperty(KEY_MAX_ROWS), DEFAULT_MAX_ROWS);
    }

    /** @return directorio de exportación por defecto. */
    public static String getDefaultExportDir() {
        return load().getProperty(KEY_EXPORT_DIR, DEFAULT_EXPORT_DIR);
    }

    // ── Construcción de la UI ─────────────────────────────────────────────

    private JPanel buildTitleBar(String title) {
        JLabel lbl = new JLabel("  ⚙  " + title);
        lbl.setFont(MainWindow.font(Font.BOLD, 12));
        lbl.setForeground(MainWindow.WHITE);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(MainWindow.MINE_SHAFT);
        bar.setPreferredSize(new Dimension(0, MainWindow.px(32)));
        bar.add(lbl, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(MainWindow.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(
                MainWindow.px(20), MainWindow.px(24),
                MainWindow.px(10), MainWindow.px(24)));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(0, 0, MainWindow.px(14), MainWindow.px(16));
        lc.gridx = 0; lc.gridy = 0;

        GridBagConstraints vc = new GridBagConstraints();
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.weightx = 1.0;
        vc.insets = new Insets(0, 0, MainWindow.px(14), 0);
        vc.gridx = 1; vc.gridy = 0;

        // Max filas
        JLabel lblRows = label("Máximo de filas en tabla de resultados");
        spnMaxRows.setFont(MainWindow.font(Font.PLAIN, 11));
        form.add(lblRows,    lc);
        form.add(spnMaxRows, vc);

        // Directorio exportación
        lc.gridy = 1; vc.gridy = 1;
        JLabel lblDir = label("Directorio de exportación por defecto");
        txfExportDir.setFont(MainWindow.font(Font.PLAIN, 11));

        JButton btnBrowse = new JButton("…");
        btnBrowse.setFont(MainWindow.font(Font.PLAIN, 11));
        btnBrowse.setFocusPainted(false);
        btnBrowse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBrowse.addActionListener(e -> {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser(txfExportDir.getText());
            fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Seleccionar directorio");
            if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                txfExportDir.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel dirRow = new JPanel(new BorderLayout(MainWindow.px(6), 0));
        dirRow.setOpaque(false);
        dirRow.add(txfExportDir, BorderLayout.CENTER);
        dirRow.add(btnBrowse,    BorderLayout.EAST);

        form.add(lblDir, lc);
        form.add(dirRow, vc);

        return form;
    }

    private JPanel buildButtons() {
        JButton btnSave   = solidBtn("Guardar", MainWindow.FLUSH_ORANGE, MainWindow.CHELSEA_GEM);
        JButton btnCancel = outlineBtn("Cancelar", MainWindow.SCI_BLUE);

        Dimension sz = new Dimension(MainWindow.px(110), MainWindow.px(34));
        btnSave.setPreferredSize(sz);
        btnCancel.setPreferredSize(sz);

        btnSave.addActionListener(e -> {
            if (save()) {
                JOptionPane.showMessageDialog(this,
                    "Configuración guardada correctamente.",
                    "Configuración", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        });
        btnCancel.addActionListener(e -> dispose());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, MainWindow.px(8), MainWindow.px(8)));
        bar.setBackground(new Color(0xF5F5F5));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.SILVER));
        bar.add(btnCancel);
        bar.add(btnSave);
        return bar;
    }

    // ── Persistencia ──────────────────────────────────────────────────────

    private boolean save() {
        Properties props = new Properties();
        props.setProperty(KEY_MAX_ROWS,   String.valueOf(spnMaxRows.getValue()));
        props.setProperty(KEY_EXPORT_DIR, txfExportDir.getText().trim());
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Validador de Atributos ACES — configuracion");
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                "No se pudo guardar la configuración:\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        if (CONFIG_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
        return props;
    }

    private static int parseInt(String v, int def) {
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    // ── Fábrica de componentes ────────────────────────────────────────────

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(MainWindow.font(Font.PLAIN, 11));
        l.setForeground(MainWindow.MID_GRAY);
        return l;
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