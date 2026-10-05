package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.validador.aces.models.Catalog;
import com.validador.aces.parsers.ExcelCatalogParser;

/**
 * Panel de carga del catálogo de atributos ACES.
 *
 * <h2>Flujo de uso</h2>
 * <ol>
 *   <li>El usuario hace clic en "Examinar…": se abre un {@link JFileChooser}
 *       filtrado por .xlsx.</li>
 *   <li>Al seleccionar el archivo, se leen los nombres de hojas en segundo plano
 *       y aparece un {@link JComboBox} para que el usuario elija cuál auditar.</li>
 *   <li>El botón "Cargar catálogo" se habilita únicamente cuando hay un archivo
 *       y una hoja seleccionados.</li>
 *   <li>La carga real se ejecuta en {@link SwingWorker} para no bloquear el EDT.
 *       Tras el éxito se muestra una card de resultados con las métricas del
 *       catálogo.</li>
 * </ol>
 */
public class CatalogLoadPanel extends JPanel {

    private static File lastDir = null;   // recuerda el último directorio

    private final MainWindow window;
    private File   selectedFile  = null;
    private String selectedSheet = null;

    // ── Componentes mutables ──────────────────────────────────────────────
    private final JTextField         pathField;
    private final JButton            btnBrowse;
    private final JButton            btnLoad;
    private final JComboBox<String>  sheetCombo;
    private final JPanel             sheetSection;   // fila hoja (oculta al inicio)
    private final JLabel             sheetStatus;    // "Leyendo hojas…"

    // Card de resultado
    private final JPanel  resultCard;
    private final JLabel  lblResultTitle;
    private final JLabel  lblLines;
    private final JLabel  lblAttrs;
    private final JLabel  lblSheet;

    // Altura uniforme para todos los controles de entrada
    private static final int ROW_H = MainWindow.px(32);

    // ── Constructor ───────────────────────────────────────────────────────

    public CatalogLoadPanel(MainWindow window) {
        this.window = window;
        window.registerPanel(MainWindow.CARD_CATALOG, this);
        window.getBtnCatalog().addActionListener(e ->
                window.showCard(MainWindow.CARD_CATALOG));

        // Inicializar componentes antes del layout
        pathField    = buildPathField();
        btnBrowse    = buildBrowseBtn();
        btnLoad      = buildLoadBtn();
        sheetCombo   = buildSheetCombo();
        sheetStatus  = new JLabel("Leyendo hojas…");
        sheetStatus.setFont(MainWindow.font(Font.ITALIC, 10));
        sheetStatus.setForeground(MainWindow.MID_GRAY);
        sheetSection = buildSheetSection();
        sheetSection.setVisible(false);

        lblResultTitle = new JLabel();
        lblLines       = new JLabel();
        lblAttrs       = new JLabel();
        lblSheet       = new JLabel();
        resultCard     = buildResultCard();
        resultCard.setVisible(false);

        setBackground(MainWindow.WHITE);
        setLayout(new BorderLayout());
        add(buildContent(), BorderLayout.CENTER);

        wireActions();
    }

    // ── Layout ────────────────────────────────────────────────────────────

    private JPanel buildContent() {
        JPanel wrapper = new JPanel();
        wrapper.setBackground(MainWindow.WHITE);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createEmptyBorder(
                MainWindow.px(32), MainWindow.px(60),
                MainWindow.px(32), MainWindow.px(60)));

        JPanel col = new JPanel();
        col.setBackground(MainWindow.WHITE);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setMaximumSize(new Dimension(MainWindow.px(680), Integer.MAX_VALUE));
        col.setAlignmentX(CENTER_ALIGNMENT);

        col.add(buildSectionHeader());
        col.add(Box.createVerticalStrut(MainWindow.px(18)));
        col.add(buildSelectionCard());
        col.add(Box.createVerticalStrut(MainWindow.px(16)));
        resultCard.setAlignmentX(CENTER_ALIGNMENT);
        col.add(resultCard);

        wrapper.add(Box.createVerticalGlue());
        wrapper.add(col);
        wrapper.add(Box.createVerticalGlue());
        return wrapper;
    }

    private JPanel buildSectionHeader() {
        JLabel title = new JLabel("Catálogo de atributos");
        title.setFont(MainWindow.font(Font.BOLD, 15));
        title.setForeground(MainWindow.MINE_SHAFT);

        JLabel sub = new JLabel("Archivo Excel con la definición de atributos por línea de producto");
        sub.setFont(MainWindow.font(Font.PLAIN, 12));
        sub.setForeground(MainWindow.MID_GRAY);

        JPanel p = new JPanel(new BorderLayout(0, MainWindow.px(4)));
        p.setOpaque(false);
        p.add(title, BorderLayout.NORTH);
        p.add(sub,   BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSelectionCard() {
        // ── Título de card ─────────────────────────────────────────────
        JLabel title = new JLabel("Selección de archivo");
        title.setFont(MainWindow.font(Font.BOLD, 12));
        title.setForeground(MainWindow.MINE_SHAFT);

        JPanel sep = hRule();

        JPanel head = new JPanel(new BorderLayout(0, MainWindow.px(8)));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(sep,   BorderLayout.SOUTH);

        // ── Fila de archivo (campo + botón misma altura) ───────────────
        JLabel fileLbl = fieldLabel("Archivo de catálogo (.xlsx)");

        JPanel fileRow = new JPanel();
        fileRow.setLayout(new BoxLayout(fileRow, BoxLayout.X_AXIS));
        fileRow.setOpaque(false);
        fileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));

        pathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        pathField.setMinimumSize(new Dimension(MainWindow.px(100), ROW_H));
        pathField.setPreferredSize(new Dimension(MainWindow.px(340), ROW_H));

        btnBrowse.setMaximumSize(new Dimension(MainWindow.px(110), ROW_H));
        btnBrowse.setMinimumSize(new Dimension(MainWindow.px(90),  ROW_H));
        btnBrowse.setPreferredSize(new Dimension(MainWindow.px(100), ROW_H));

        fileRow.add(pathField);
        fileRow.add(Box.createHorizontalStrut(MainWindow.px(8)));
        fileRow.add(btnBrowse);

        JLabel hint = new JLabel("Ej: Atributos ACES por línea de producto.xlsx");
        hint.setFont(MainWindow.font(Font.PLAIN, 10));
        hint.setForeground(MainWindow.SILVER);

        // ── Sección de hojas (aparece después de seleccionar archivo) ──
        // (construida en buildSheetSection(), asignada en campo)

        // ── Botón cargar (rectángulo fijo debajo del selector de hoja) ─
        btnLoad.setAlignmentX(LEFT_ALIGNMENT);
        JPanel loadRow = new JPanel(new BorderLayout());
        loadRow.setOpaque(false);
        loadRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, MainWindow.px(36)));
        loadRow.setBorder(BorderFactory.createEmptyBorder(MainWindow.px(14), 0, 0, 0));
        loadRow.add(btnLoad, BorderLayout.WEST);

        // ── Composición del cuerpo ─────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(fileLbl);
        body.add(Box.createVerticalStrut(MainWindow.px(6)));
        body.add(fileRow);
        body.add(Box.createVerticalStrut(MainWindow.px(4)));
        body.add(hint);
        body.add(Box.createVerticalStrut(MainWindow.px(12)));
        body.add(sheetSection);   // ← aparece dinámicamente
        body.add(loadRow);

        JPanel inner = new JPanel(new BorderLayout(0, MainWindow.px(14)));
        inner.setOpaque(false);
        inner.add(head, BorderLayout.NORTH);
        inner.add(body, BorderLayout.CENTER);

        return card(MainWindow.FLUSH_ORANGE, inner);
    }

    /** Fila dinámica con selector de hoja. */
    private JPanel buildSheetSection() {
        JLabel lbl = fieldLabel("Hoja a auditar");

        sheetCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        sheetCombo.setMinimumSize(new Dimension(MainWindow.px(100), ROW_H));
        sheetCombo.setPreferredSize(new Dimension(MainWindow.px(340), ROW_H));

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.add(lbl);
        row.add(Box.createVerticalStrut(MainWindow.px(6)));
        row.add(sheetCombo);
        row.add(Box.createVerticalStrut(MainWindow.px(2)));
        row.add(sheetStatus);
        return row;
    }

    private JPanel buildResultCard() {
        lblResultTitle.setFont(MainWindow.font(Font.BOLD, 12));

        JPanel head = new JPanel(new BorderLayout(0, MainWindow.px(8)));
        head.setOpaque(false);
        head.add(lblResultTitle, BorderLayout.NORTH);
        head.add(hRule(),        BorderLayout.SOUTH);

        JPanel statsGrid = new JPanel(new GridLayout(0, 2, MainWindow.px(16), MainWindow.px(6)));
        statsGrid.setOpaque(false);
        statsGrid.add(statLabel("Hoja auditada"));
        statsGrid.add(lblSheet);
        statsGrid.add(statLabel("Líneas de producto"));
        statsGrid.add(lblLines);
        statsGrid.add(statLabel("Atributos por línea (aprox.)"));
        statsGrid.add(lblAttrs);

        JButton btnCont = solidBtn("  Continuar: Cargar ACES  →",
                MainWindow.SCI_BLUE, MainWindow.MALIBU);
        btnCont.addActionListener(e -> window.showCard(MainWindow.CARD_ACES));

        JPanel contRow = new JPanel(new BorderLayout());
        contRow.setOpaque(false);
        contRow.setBorder(BorderFactory.createEmptyBorder(MainWindow.px(18), 0, 0, 0));
        contRow.add(btnCont, BorderLayout.WEST);

        JPanel inner = new JPanel(new BorderLayout(0, 0));
        inner.setOpaque(false);
        inner.add(head,      BorderLayout.NORTH);
        inner.add(statsGrid, BorderLayout.CENTER);
        inner.add(contRow,   BorderLayout.SOUTH);

        return card(MainWindow.SCI_BLUE, inner);
    }

    // ── Fábrica de componentes ────────────────────────────────────────────

    private static JPanel card(Color accent, JPanel inner) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(MainWindow.WHITE);
        card.setMaximumSize(new Dimension(MainWindow.px(680), Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, MainWindow.px(3), 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainWindow.SILVER, 1),
                BorderFactory.createEmptyBorder(
                    MainWindow.px(24), MainWindow.px(30),
                    MainWindow.px(24), MainWindow.px(30))
            )
        ));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private static JTextField buildPathField() {
        JTextField f = new JTextField();
        f.setEditable(false);
        f.setFont(MainWindow.font(Font.PLAIN, 11));
        f.setForeground(MainWindow.MINE_SHAFT);
        f.setBackground(new Color(0xF8F8F8));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainWindow.SILVER, 1),
            BorderFactory.createEmptyBorder(0, MainWindow.px(8), 0, MainWindow.px(8))
        ));
        return f;
    }

    private static JButton buildBrowseBtn() {
        JButton btn = new JButton("Examinar…");
        btn.setFont(MainWindow.font(Font.PLAIN, 11));
        btn.setForeground(MainWindow.SCI_BLUE);
        btn.setBackground(MainWindow.WHITE);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MainWindow.SCI_BLUE, 1),
            BorderFactory.createEmptyBorder(0, MainWindow.px(10), 0, MainWindow.px(10))
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xEAF3FB)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(MainWindow.WHITE); }
        });
        return btn;
    }

    private static JButton buildLoadBtn() {
        JButton btn = new JButton("Cargar catálogo");
        btn.setFont(MainWindow.font(Font.PLAIN, 12));
        btn.setForeground(MainWindow.WHITE);
        btn.setBackground(MainWindow.FLUSH_ORANGE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Rectangulo proporcional: ancho fijo, padding vertical razonable
        Dimension sz = new Dimension(MainWindow.px(190), MainWindow.px(36));
        btn.setPreferredSize(sz);
        btn.setMaximumSize(sz);
        btn.setMinimumSize(sz);
        btn.setEnabled(false);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(MainWindow.CHELSEA_GEM);
            }
            public void mouseExited(MouseEvent e) { btn.setBackground(MainWindow.FLUSH_ORANGE); }
        });
        return btn;
    }

    private static JComboBox<String> buildSheetCombo() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setFont(MainWindow.font(Font.PLAIN, 11));
        cb.setFocusable(false);
        return cb;
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
        btn.setBorder(BorderFactory.createEmptyBorder(
                MainWindow.px(7), MainWindow.px(18),
                MainWindow.px(7), MainWindow.px(18)));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(MainWindow.font(Font.PLAIN, 11));
        l.setForeground(MainWindow.MID_GRAY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel statLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(MainWindow.font(Font.PLAIN, 11));
        l.setForeground(MainWindow.MID_GRAY);
        return l;
    }

    private static JPanel hRule() {
        JPanel p = new JPanel();
        p.setBackground(new Color(0xEEEEEE));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.setPreferredSize(new Dimension(0, 1));
        return p;
    }

    // ── Lógica ────────────────────────────────────────────────────────────

    private void wireActions() {
        btnBrowse.addActionListener(e -> openFileChooser());
        btnLoad.addActionListener(e -> loadCatalog());
        sheetCombo.addActionListener(e -> {
            selectedSheet = (String) sheetCombo.getSelectedItem();
            btnLoad.setEnabled(selectedSheet != null && !selectedSheet.isEmpty());
        });
    }

    /** Abre el selector de archivos; si se elige uno, carga sus hojas. */
    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser(
                lastDir != null ? lastDir : new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Seleccionar catálogo de atributos ACES");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (.xlsx)", "xlsx"));
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) return;

        selectedFile = chooser.getSelectedFile();
        lastDir      = selectedFile.getParentFile();
        pathField.setText(selectedFile.getAbsolutePath());

        // Resetear estado anterior
        btnLoad.setEnabled(false);
        resultCard.setVisible(false);
        sheetSection.setVisible(false);
        sheetStatus.setText("Leyendo hojas…");
        sheetStatus.setForeground(MainWindow.MID_GRAY);
        sheetCombo.setModel(new DefaultComboBoxModel<>());

        loadSheetNames(selectedFile);
    }

    /** Lee los nombres de hojas en background y puebla el JComboBox. */
    private void loadSheetNames(File file) {
        sheetSection.setVisible(true);
        sheetStatus.setVisible(true);
        revalidate();

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return ExcelCatalogParser.readSheetNames(file);
            }

            @Override
            protected void done() {
                try {
                    List<String> sheets = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    for (String s : sheets) model.addElement(s);
                    sheetCombo.setModel(model);

                    // Auto-seleccionar la hoja predeterminada si existe
                    String def = ExcelCatalogParser.DEFAULT_SHEET_NAME;
                    for (int i = 0; i < model.getSize(); i++) {
                        if (def.equals(model.getElementAt(i))) {
                            sheetCombo.setSelectedIndex(i);
                            break;
                        }
                    }

                    sheetStatus.setVisible(false);
                    selectedSheet = (String) sheetCombo.getSelectedItem();
                    btnLoad.setEnabled(selectedSheet != null);
                } catch (Exception ex) {
                    sheetStatus.setText("No se pudieron leer las hojas del archivo.");
                    sheetStatus.setForeground(MainWindow.C_ERROR);
                    btnLoad.setEnabled(false);
                }
                revalidate();
                repaint();
            }
        }.execute();
    }

    /** Parsea el catálogo en background usando la hoja seleccionada. */
    private void loadCatalog() {
        btnLoad.setEnabled(false);
        btnBrowse.setEnabled(false);
        window.setButtonsEnabled(false);
        window.setStatus("Cargando catálogo…  Por favor espere.");

        final File   file  = selectedFile;
        final String sheet = selectedSheet;

        new SwingWorker<Catalog, Void>() {
            @Override
            protected Catalog doInBackground() throws Exception {
                return new ExcelCatalogParser().parse(file, sheet);
            }

            @Override
            protected void done() {
                try {
                    Catalog catalog = get();
                    window.setLoadedCatalog(catalog);
                    showSuccess(catalog, sheet);
                    window.setStatus(
                        "✔  Catálogo cargado — "
                            + fmt(catalog.getProductLineCount())
                            + " líneas de producto.",
                        MainWindow.C_OK);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    JOptionPane.showMessageDialog(window,
                        "No se pudo cargar el catálogo:\n\n"
                            + (cause != null ? cause.getMessage() : ex.getMessage()),
                        "Error de carga", JOptionPane.ERROR_MESSAGE);
                    window.setStatus("✘  Error al cargar el catálogo.", MainWindow.C_ERROR);
                    window.setLoadedCatalog(null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(window, "Error inesperado:\n\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    window.setStatus("✘  Error inesperado.", MainWindow.C_ERROR);
                } finally {
                    btnLoad.setEnabled(true);
                    btnBrowse.setEnabled(true);
                    window.setButtonsEnabled(true);
                }
            }
        }.execute();
    }

    private void showSuccess(Catalog catalog, String sheetName) {
        int totalAttrs = 0;
        for (com.validador.aces.models.ProductLine pl : catalog.getProductLines()) {
            totalAttrs += pl.getAttributeCount();
        }
        int attrPerLine = catalog.getProductLineCount() > 0
                ? totalAttrs / catalog.getProductLineCount() : 0;

        lblResultTitle.setText("✔  Catálogo cargado correctamente");
        lblResultTitle.setForeground(MainWindow.SCI_BLUE);

        lblSheet.setText(sheetName);
        lblSheet.setFont(MainWindow.font(Font.BOLD, 12));
        lblSheet.setForeground(MainWindow.SCI_BLUE);

        lblLines.setText(fmt(catalog.getProductLineCount()));
        lblLines.setFont(MainWindow.font(Font.BOLD, 12));
        lblLines.setForeground(MainWindow.SCI_BLUE);

        lblAttrs.setText(String.valueOf(attrPerLine));
        lblAttrs.setFont(MainWindow.font(Font.BOLD, 12));
        lblAttrs.setForeground(MainWindow.SCI_BLUE);

        resultCard.setVisible(true);
        revalidate();
        repaint();
    }

    private static String fmt(int n) {
        return NumberFormat.getNumberInstance(new Locale("es", "ES")).format(n);
    }
}