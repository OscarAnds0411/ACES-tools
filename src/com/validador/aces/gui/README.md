# Paquete: gui

Interfaz Swing (look and feel Nimbus). Punto de entrada: `com.validador.aces.Launcher`, que crea `MainWindow` y registra los paneles.

| Clase | Rol |
|---|---|
| `MainWindow` | Ventana principal con `CardLayout` (Bienvenida, Catálogo, ACES, Resultados, Estadísticas, Historial). Guarda el estado compartido: catálogo, aplicaciones, archivo ACES, últimos resultados e historial de auditoría |
| `CatalogLoadPanel` | Elegir archivo y hoja del catálogo, cargarlo y mostrar los avisos de lectura |
| `ApplicationLoadPanel` | Elegir archivo y hoja del ACES, cargarlo (solo las columnas que conoce el catálogo) y mostrar las filas omitidas |
| `ValidationPanel` | Ejecuta la auditoría y muestra métricas y tabla de faltantes; exporta el reporte Excel |
| `StatisticsPanel` | Estadísticas por línea de producto y detalle por número de parte |
| `AuditPanel` | Historial de auditorías de la sesión; exporta a CSV |
| `ConfigDialog` | Configuración: máximo de filas de la tabla y directorio de exportación (`~/.validador_aces_config.properties`) |
| `FileDialogHelper` | Selector de archivos `.xlsx` con directorio recordado; hoy solo lo usa `AuditPanel` |

## Comportamientos a tener en cuenta

- Leer Excel, comparar y exportar corren en un `SwingWorker`; la interfaz solo se toca en el hilo de Swing.
- Cambiar el catálogo o el ACES (o que la carga falle) **descarta los resultados** de la auditoría anterior: `MainWindow` avisa a sus oyentes (`addResultsInvalidatedListener`) y `ValidationPanel`/`StatisticsPanel` vuelven a su estado vacío.
- Exportar: no permite elegir el archivo ACES cargado, pide confirmación antes de reemplazar un archivo existente y, si falla porque el archivo está abierto en otro programa, lo explica.
- No hay pruebas automáticas de la GUI.

Ver `LOGICA_DEL_PROGRAMA.md` §4.5–4.7.
