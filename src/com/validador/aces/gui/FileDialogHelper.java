package com.validador.aces.gui;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Centraliza la creación y configuración de {@link JFileChooser} para
 * archivos Excel (.xlsx) en toda la aplicación.
 *
 * <p>La clase recuerda el último directorio visitado entre invocaciones
 * mediante un campo {@code lastDir} compartido (estático), de modo que
 * tanto el diálogo de apertura del catálogo como el del archivo ACES
 * arrancan en la misma carpeta que el usuario usó por última vez.</p>
 */
public final class FileDialogHelper {

    /** Último directorio visitado. Compartido entre todas las invocaciones. */
    private static File lastDir = null;

    private FileDialogHelper() {}   // clase no instanciable

    /**
     * Abre un diálogo de apertura de archivo Excel (.xlsx).
     *
     * @param parent componente padre para posicionar el diálogo
     * @param title  título del diálogo
     * @return el archivo seleccionado, o {@code null} si el usuario canceló
     */
    public static File chooseExcelOpen(Component parent, String title) {
        JFileChooser chooser = buildChooser(title, null);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            lastDir = f.getParentFile();
            return f;
        }
        return null;
    }

    /**
     * Abre un diálogo de guardado de archivo Excel (.xlsx).
     *
     * @param parent    componente padre para posicionar el diálogo
     * @param title     título del diálogo
     * @param suggested archivo sugerido como nombre por defecto (puede ser null)
     * @return el archivo de destino, o {@code null} si el usuario canceló
     */
    public static File chooseExcelSave(Component parent, String title, File suggested) {
        JFileChooser chooser = buildChooser(title, suggested);
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".xlsx")) {
                f = new File(f.getAbsolutePath() + ".xlsx");
            }
            lastDir = f.getParentFile();
            return f;
        }
        return null;
    }

    /** Actualiza el directorio de referencia (usado por CatalogLoadPanel / ApplicationLoadPanel). */
    public static void setLastDir(File dir) {
        if (dir != null && dir.isDirectory()) lastDir = dir;
    }

    /** @return el último directorio visitado, o el directorio home si aún no se abrió ninguno. */
    public static File getLastDir() {
        return lastDir != null ? lastDir : new File(System.getProperty("user.home"));
    }

    // ── Construcción interna ──────────────────────────────────────────────

    private static JFileChooser buildChooser(String title, File suggested) {
        JFileChooser chooser = new JFileChooser(getLastDir());
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (.xlsx)", "xlsx"));
        chooser.setAcceptAllFileFilterUsed(false);
        if (suggested != null) {
            chooser.setCurrentDirectory(suggested.getParentFile());
            chooser.setSelectedFile(suggested);
        }
        return chooser;
    }
}