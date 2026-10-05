package com.validador.aces.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ventana principal del Validador de Atributos ACES.
 *
 * <p>Diseño sin decoración del sistema ({@code setUndecorated(true)}): la
 * barra de título y los controles de ventana son propios de la aplicación.
 * La ventana es arrastrable desde su barra de título.</p>
 *
 * <h2>Responsividad</h2>
 * <ul>
 *   <li>El tamaño inicial se calcula como porcentaje de la pantalla disponible,
 *       respetando la escala del monitor (HiDPI / 125 % / 150 %).</li>
 *   <li>Los tamaños de fuente y espaciado se derivan de un factor de escala
 *       ({@link #S}) calculado a partir del DPI lógico del dispositivo, de modo
 *       que la UI se ve igual de proporcionada en cualquier resolución o escala
 *       de Windows/macOS.</li>
 *   <li>El área de contenido central usa layouts flexibles ({@link BorderLayout},
 *       {@link BoxLayout} con glue) que se adaptan al tamaño de la ventana.</li>
 * </ul>
 *
 * <h2>Paleta</h2>
 * <table>
 *   <tr><td>#ffffff</td><td>WHITE      </td><td>fondos de tarjeta y contenido</td></tr>
 *   <tr><td>#2d2d2d</td><td>MINE_SHAFT </td><td>barra de título, toolbar, textos primarios</td></tr>
 *   <tr><td>#fd8701</td><td>FLUSH_ORANGE</td><td>acción principal (Ejecutar)</td></tr>
 *   <tr><td>#6c6c6d</td><td>MID_GRAY   </td><td>texto secundario, status</td></tr>
 *   <tr><td>#bcbcbc</td><td>SILVER     </td><td>bordes, separadores, disabled</td></tr>
 *   <tr><td>#fd0102</td><td>RED        </td><td>errores, alerta de estado</td></tr>
 *   <tr><td>#0170c8</td><td>SCI_BLUE   </td><td>acciones secundarias (Cargar...)</td></tr>
 *   <tr><td>#a25001</td><td>CHELSEA_GEM</td><td>hover del naranja</td></tr>
 *   <tr><td>#ffbe60</td><td>KOROMIKO   </td><td>highlight sutil de pasos</td></tr>
 *   <tr><td>#749eff</td><td>MALIBU     </td><td>hover del azul</td></tr>
 * </table>
 */
public class MainWindow extends JFrame {

    // ── Card names ────────────────────────────────────────────────────────
    public static final String CARD_WELCOME = "WELCOME";
    public static final String CARD_CATALOG = "CATALOG";
    public static final String CARD_ACES    = "ACES";
    public static final String CARD_RESULTS = "RESULTS";

    // ── Paleta ────────────────────────────────────────────────────────────
    public static final Color WHITE         = new Color(0xFFFFFF);
    public static final Color MINE_SHAFT    = new Color(0x2D2D2D);
    public static final Color FLUSH_ORANGE  = new Color(0xFD8701);
    public static final Color MID_GRAY      = new Color(0x6C6C6D);
    public static final Color SILVER        = new Color(0xBCBCBC);
    public static final Color RED           = new Color(0xFD0102);
    public static final Color SCI_BLUE      = new Color(0x0170C8);
    public static final Color CHELSEA_GEM   = new Color(0xA25001);
    public static final Color KOROMIKO      = new Color(0xFFBE60);
    public static final Color MALIBU        = new Color(0x749EFF);

    // Semánticos
    /** Color de estado de error. */
    public static final Color C_ERROR = RED;
    /** Color de estado de información / éxito. */
    public static final Color C_OK    = SCI_BLUE;

    // ── Escala de DPI (calculada una sola vez) ────────────────────────────
    /**
     * Factor de escala lógico del monitor principal.
     * En una pantalla estándar (96 DPI, 100 %) = 1.0.
     * En HiDPI 150 % = 1.5, en 125 % = 1.25, etc.
     */
    static final double S;
    static {
        double tmp = 1.0;
        try {
            tmp = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration()
                    .getDefaultTransform()
                    .getScaleX();
        } catch (Exception ignored) {}
        S = Math.max(1.0, tmp);
    }

    /** Escala un valor base (diseñado para 96 DPI) al DPI real del monitor. */
    static int px(int base) {
        return (int) Math.round(base * S);
    }

    /** Fuente escalada al DPI del monitor. */
    static Font font(int style, int ptBase) {
        return new Font("SansSerif", style, (int) Math.round(ptBase * S));
    }

    // ── Componentes ───────────────────────────────────────────────────────
    private final CardLayout cardLayout;
    private final JPanel     cardPanel;

    private final JButton btnCatalog;
    private final JButton btnAces;
    private final JButton btnRun;
    private final JButton btnExit;

    private final JLabel statusLabel;

    // Arrastre de ventana sin decoración
    private int dragX, dragY;

    /** Construye la ventana. Llamar a {@link #setVisible(boolean)} en el EDT. */
    public MainWindow() {
        super("Validador de Atributos ACES");
        setUndecorated(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ── Tamaño inicial proporcional a la pantalla disponible ──────────
        Rectangle avail = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();          // descuenta taskbar
        int initW = (int) (avail.width  * 0.84);
        int initH = (int) (avail.height * 0.88);
        setPreferredSize(new Dimension(initW, initH));
        setMinimumSize(new Dimension(px(780), px(520)));

        // ── Secciones ─────────────────────────────────────────────────────
        JPanel titleBar = buildTitleBar();

        btnCatalog = toolbarBtn("Cargar catálogo",   SCI_BLUE, MALIBU,       WHITE);
        btnAces    = toolbarBtn("Cargar ACES",        SCI_BLUE, MALIBU,       WHITE);
        btnRun     = toolbarBtn("Ejecutar auditoría", FLUSH_ORANGE, CHELSEA_GEM, WHITE);
        btnExit    = toolbarBtn("Salir",              MINE_SHAFT, new Color(0x444444), SILVER);
        btnRun.setEnabled(false);

        JPanel toolbar = buildToolbar();

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(WHITE);
        cardPanel.add(buildWelcomePanel(), CARD_WELCOME);

        statusLabel = new JLabel("  Bienvenido — cargue el catálogo para comenzar.");
        statusLabel.setFont(font(Font.PLAIN, 11));
        statusLabel.setForeground(MID_GRAY);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(0xF2F2F2));
        statusBar.setPreferredSize(new Dimension(0, px(26)));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SILVER));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        // ── Ensamblado ────────────────────────────────────────────────────
        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar, BorderLayout.NORTH);
        north.add(toolbar,  BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(north,     BorderLayout.NORTH);
        getContentPane().add(cardPanel, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // Borde exterior delgado para distinguir la ventana del escritorio
        getRootPane().setBorder(
                BorderFactory.createLineBorder(new Color(0x111111), 1));

        // ── Listeners ────────────────────────────────────────────────────
        btnExit.addActionListener(e -> System.exit(0));

        pack();
        setLocationRelativeTo(null);
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Registra un panel en el CardLayout bajo el nombre indicado.
     *
     * @param name  constante {@code CARD_*}
     * @param panel panel a registrar
     */
    public void registerPanel(String name, JPanel panel) {
        cardPanel.add(panel, name);
    }

    /** Muestra la card con el nombre indicado. */
    public void showCard(String name) {
        cardLayout.show(cardPanel, name);
    }

    /** Actualiza el mensaje de la barra de estado (color por defecto). */
    public void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  " + msg);
            statusLabel.setForeground(MID_GRAY);
        });
    }

    /** Actualiza el mensaje de la barra de estado con un color específico. */
    public void setStatus(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  " + msg);
            statusLabel.setForeground(color);
        });
    }

    /**
     * Habilita/deshabilita los botones de carga (durante operaciones largas).
     *
     * @param enabled true para habilitar
     */
    public void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            btnCatalog.setEnabled(enabled);
            btnAces.setEnabled(enabled);
            btnExit.setEnabled(enabled);
        });
    }

    /** Habilita/deshabilita el botón "Ejecutar auditoría". */
    public void setRunEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> btnRun.setEnabled(enabled));
    }

    // Accesores para que los paneles registren sus listeners
    public JButton getBtnCatalog() { return btnCatalog; }
    public JButton getBtnAces()    { return btnAces; }
    public JButton getBtnRun()     { return btnRun; }
    public JButton getBtnExit()    { return btnExit; }

    // ── Construcción interna ──────────────────────────────────────────────

    /** Barra de título personalizada: delgada, oscura, arrastrable. */
    private JPanel buildTitleBar() {
        // Punto decorativo de marca
        JLabel dot = new JLabel("  ▌ ");
        dot.setFont(font(Font.BOLD, 13));
        dot.setForeground(FLUSH_ORANGE);

        JLabel title = new JLabel("Validador de Atributos ACES");
        title.setFont(font(Font.PLAIN, 12));
        title.setForeground(new Color(0xDDDDDD));

        // Botón cerrar — solo X, se vuelve rojo al pasar el mouse
        JButton close = new JButton("×");
        close.setFont(font(Font.PLAIN, 16));
        close.setForeground(new Color(0x888888));
        close.setBackground(MINE_SHAFT);
        close.setOpaque(true);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setPreferredSize(new Dimension(px(42), px(32)));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                close.setBackground(RED);
                close.setForeground(WHITE);
            }
            public void mouseExited(MouseEvent e) {
                close.setBackground(MINE_SHAFT);
                close.setForeground(new Color(0x888888));
            }
        });
        close.addActionListener(e -> System.exit(0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(close);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(MINE_SHAFT);
        bar.setPreferredSize(new Dimension(0, px(32)));
        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        // Drag-to-move
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { dragX = e.getX(); dragY = e.getY(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                java.awt.Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragX, loc.y + e.getY() - dragY);
            }
        });

        return bar;
    }

    /** Toolbar de acciones. */
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(MINE_SHAFT);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, FLUSH_ORANGE),   // acento naranja inferior
            BorderFactory.createEmptyBorder(px(6), px(12), px(6), px(12))
        ));

        // Lado izquierdo: botones de carga y separador
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, px(6), 0));
        left.setOpaque(false);
        left.add(btnCatalog);
        left.add(btnAces);
        left.add(toolbarSep());
        left.add(btnRun);

        // Lado derecho: salir
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnExit);

        bar.add(left,  BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** Panel de bienvenida responsivo. */
    private JPanel buildWelcomePanel() {
        // Fondo con gradiente muy sutil: blanco → levísimo naranja
        JPanel bg = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(
                    0, 0, WHITE,
                    0, getHeight(), new Color(0xFFF8F0)  // naranja extremadamente tenue
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bg.setOpaque(false);

        // Tarjeta central — usa BoxLayout + glue para centrarse responsivamente
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(px(40), px(60), px(40), px(60)));

        JPanel card = buildCard();
        card.setAlignmentX(CENTER_ALIGNMENT);

        wrapper.add(Box.createVerticalGlue());
        wrapper.add(card);
        wrapper.add(Box.createVerticalGlue());

        bg.add(wrapper, BorderLayout.CENTER);
        return bg;
    }

    /** Tarjeta central de la bienvenida. */
    private JPanel buildCard() {
        JPanel card = new JPanel(new BorderLayout(0, px(20)));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, px(3), 0, 0, FLUSH_ORANGE),  // acento izquierdo naranja
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SILVER, 1),
                BorderFactory.createEmptyBorder(px(36), px(44), px(36), px(44))
            )
        ));
        card.setMaximumSize(new Dimension(px(660), Integer.MAX_VALUE));

        // Cabecera
        JLabel heading = new JLabel("Auditoría de Atributos ACES");
        heading.setFont(font(Font.BOLD, 18));
        heading.setForeground(MINE_SHAFT);

        JLabel sub = new JLabel(
            "<html><span style='color:#6c6c6d'>Compare aplicaciones ACES contra su catálogo"
            + " de atributos para identificar faltantes de compliance.</span></html>");
        sub.setFont(font(Font.PLAIN, 12));

        JPanel head = new JPanel(new BorderLayout(0, px(6)));
        head.setOpaque(false);
        head.add(heading, BorderLayout.NORTH);
        head.add(sub,     BorderLayout.CENTER);
        head.add(hRule(), BorderLayout.SOUTH);

        // Pasos
        JPanel steps = new JPanel();
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.setOpaque(false);
        steps.setBorder(BorderFactory.createEmptyBorder(px(4), 0, 0, 0));

        steps.add(step(1, "Cargar catálogo",
            "Seleccione el archivo de definición de atributos (Required / Optional / Not Required) por línea de producto."));
        steps.add(vgap(px(12)));
        steps.add(step(2, "Cargar ACES",
            "Seleccione el archivo de aplicaciones a auditar (hoja \"Applications\")."));
        steps.add(vgap(px(12)));
        steps.add(step(3, "Ejecutar auditoría",
            "Genere el reporte de compliance y exporte los resultados a Excel."));

        card.add(head,  BorderLayout.NORTH);
        card.add(steps, BorderLayout.CENTER);
        return card;
    }

    /** Una fila de paso numerado. */
    private JPanel step(int n, String title, String desc) {
        // Número en círculo naranja
        JPanel numPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FLUSH_ORANGE);
                int d = Math.min(getWidth(), getHeight());
                g2.fillOval(0, 0, d, d);
                g2.setColor(WHITE);
                g2.setFont(font(Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String s = String.valueOf(n);
                int x = (d - fm.stringWidth(s)) / 2;
                int y = (d - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, x, y);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(px(30), px(30)); }
            @Override public Dimension getMinimumSize()   { return getPreferredSize(); }
            @Override public Dimension getMaximumSize()   { return getPreferredSize(); }
        };
        numPanel.setOpaque(false);

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(font(Font.BOLD, 12));
        lTitle.setForeground(MINE_SHAFT);

        JLabel lDesc = new JLabel("<html><span style='color:#6c6c6d'>" + desc + "</span></html>");
        lDesc.setFont(font(Font.PLAIN, 11));

        JPanel text = new JPanel(new BorderLayout(0, px(2)));
        text.setOpaque(false);
        text.add(lTitle, BorderLayout.NORTH);
        text.add(lDesc,  BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout(px(14), 0));
        row.setOpaque(false);
        row.add(numPanel, BorderLayout.WEST);
        row.add(text,     BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + px(20)));
        return row;
    }

    // ── Fábrica de botones y elementos de toolbar ─────────────────────────

    /**
     * Botón plano para el toolbar con hover.
     *
     * @param label   texto
     * @param bg      fondo normal
     * @param bgHover fondo al pasar el mouse
     * @param fg      color del texto
     */
    private static JButton toolbarBtn(String label, Color bg, Color bgHover, Color fg) {
        JButton btn = new JButton(label);
        btn.setFont(font(Font.PLAIN, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(px(5), px(14), px(5), px(14)));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(bgHover);
            }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    /** Separador vertical del toolbar. */
    private JPanel toolbarSep() {
        JPanel sep = new JPanel();
        sep.setBackground(new Color(0x555555));
        sep.setPreferredSize(new Dimension(1, px(20)));
        return sep;
    }

    /** Línea horizontal separadora usada en la tarjeta. */
    private JPanel hRule() {
        JPanel line = new JPanel();
        line.setBackground(new Color(0xEEEEEE));
        line.setPreferredSize(new Dimension(0, 1));
        line.setBorder(BorderFactory.createEmptyBorder(px(12), 0, 0, 0));
        return line;
    }

    /** Relleno vertical rígido. */
    private static Box.Filler vgap(int h) {
        return (Box.Filler) Box.createVerticalStrut(h);
    }


    // ── Estado de carga (gestionado por los paneles de carga) ─────────────

    private com.validador.aces.models.Catalog      loadedCatalog;
    private java.util.List<com.validador.aces.models.Application> loadedApplications;

    /**
     * Registra el catálogo cargado y habilita "Ejecutar" si ambos archivos
     * ya están disponibles.
     *
     * @param catalog catálogo parseado, o {@code null} para descartar
     */
    public void setLoadedCatalog(com.validador.aces.models.Catalog catalog) {
        this.loadedCatalog = catalog;
        checkCanRun();
    }

    /** @return catálogo cargado, o {@code null} si aún no se cargó. */
    public com.validador.aces.models.Catalog getLoadedCatalog() {
        return loadedCatalog;
    }

    /**
     * Registra las aplicaciones cargadas y habilita "Ejecutar" si el catálogo
     * también está disponible.
     *
     * @param apps lista de aplicaciones parseadas, o {@code null} para descartar
     */
    public void setLoadedApplications(
            java.util.List<com.validador.aces.models.Application> apps) {
        this.loadedApplications = apps;
        checkCanRun();
    }

    /** @return aplicaciones cargadas, o {@code null} si aún no se cargaron. */
    public java.util.List<com.validador.aces.models.Application> getLoadedApplications() {
        return loadedApplications;
    }

    /** Activa "Ejecutar auditoría" solo cuando ambos archivos están disponibles. */
    private void checkCanRun() {
        boolean ready = loadedCatalog != null && loadedApplications != null;
        setRunEnabled(ready);
    }

    // ── Punto de entrada para revisión visual ─────────────────────────────

    /**
     * Lanza la ventana de forma independiente para revisión visual.
     * El punto de entrada real de la aplicación será {@code Launcher}.
     */
    public static void main(String[] args) {
        // Nimbus con paleta ajustada
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    UIManager.put("control",             new Color(0xF5F5F5));
                    UIManager.put("nimbusBase",           new Color(0x2D2D2D));
                    UIManager.put("nimbusFocus",          SCI_BLUE);
                    UIManager.put("nimbusLightBackground", WHITE);
                    UIManager.put("text",                 MINE_SHAFT);
                    UIManager.put("Button.textForeground", WHITE);
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainWindow w = new MainWindow();
            // Registrar todos los paneles disponibles
            new CatalogLoadPanel(w);
            // ApplicationLoadPanel se agregara en TASK-029
            w.setVisible(true);
        });
    }
}