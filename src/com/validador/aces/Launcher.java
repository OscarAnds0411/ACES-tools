package com.validador.aces;

import com.validador.aces.gui.*;
import javax.swing.*;

/**
 * Punto de entrada principal de la aplicación Validador de Atributos ACES.
 *
 * Esta clase inicia la interfaz gráfica de usuario (GUI) en el EDT (Event Dispatch Thread).
 * Todas las operaciones de Swing se ejecutan de forma thread-safe dentro del EDT.
 *
 * @author Validador ACES Team
 * @version 1.0.0
 */
public class Launcher {

    /**
     * Punto de entrada de la aplicación.
     *
     * Inicializa el look and feel de Nimbus (si está disponible) y crea la ventana
     * principal de la aplicación en el EDT.
     *
     * @param args argumentos de línea de comandos (no utilizados en la versión actual)
     */
    public static void main(String[] args) {
        // Configurar look and feel a Nimbus para una apariencia moderna
        initializeLookAndFeel();

        // Ejecutar la GUI en el EDT (thread-safe para Swing)
        SwingUtilities.invokeLater(() -> {
            // Crear ventana principal
            MainWindow mainWindow = new MainWindow();

            // Registrar paneles de interfaz
            new CatalogLoadPanel(mainWindow);
            new ApplicationLoadPanel(mainWindow);
            new ValidationPanel(mainWindow);
            new StatisticsPanel(mainWindow);

            // Registrar panel de historial de auditorías
            AuditPanel auditPanel = new AuditPanel(mainWindow);
            mainWindow.setAuditPanel(auditPanel);

            // Mostrar ventana
            mainWindow.setVisible(true);
        });
    }

    /**
     * Inicializa el look and feel de Nimbus para una apariencia moderna y consistente.
     *
     * Si Nimbus no está disponible en el sistema, se usa el look and feel por defecto.
     */
    private static void initializeLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Si Nimbus no está disponible, usar el look and feel por defecto
            // No es un error crítico, la aplicación funciona correctamente de todas formas
        }
    }
}
