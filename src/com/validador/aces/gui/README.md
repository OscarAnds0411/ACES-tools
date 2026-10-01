# Paquete: gui

## Descripción

Contiene la interfaz gráfica (GUI) de usuario para el Validador ACES.

## Clases

### TASK-027: MainWindow
- **Descripción**: Ventana principal de la aplicación
- **Componentes**: Menu bar, toolbar, panels
- **Métodos**: initUI(), setupLayout(), registerHandlers()
- **Utiliza**: Swing o JavaFX (a decidir)
- **Estado**: [TODO]

### TASK-028: CatalogPanel
- **Descripción**: Panel para cargar y visualizar catálogos
- **Componentes**: File chooser, tree view, properties panel
- **Métodos**: loadCatalog(), displayCatalog(), highlightAttribute()
- **Estado**: [TODO]

### TASK-029: ApplicationPanel
- **Descripción**: Panel para cargar y visualizar aplicaciones
- **Componentes**: File chooser, table view, edit fields
- **Métodos**: loadApplication(), displayApplication(), editAttribute()
- **Estado**: [TODO]

### TASK-030: ValidationPanel
- **Descripción**: Panel para ejecutar validaciones
- **Componentes**: Botón ejecutar, progress bar, results view
- **Métodos**: runValidation(), displayResults(), exportReport()
- **Estado**: [TODO]

### TASK-031: ComparisonPanel
- **Descripción**: Panel para comparar catálogos
- **Componentes**: Combo boxes, diff view, statistics
- **Métodos**: selectCatalog(), compare(), displayDifferences()
- **Estado**: [TODO]

### TASK-032: ResultsPanel
- **Descripción**: Panel para visualizar y exportar resultados
- **Componentes**: Table, progress indicator, export buttons
- **Métodos**: displayResults(), sortResults(), exportResults()
- **Estado**: [TODO]

### TASK-033: SettingsDialog
- **Descripción**: Diálogo de configuración
- **Opciones**: Rutas de archivos, validaciones, idioma
- **Métodos**: loadSettings(), saveSettings(), applySettings()
- **Estado**: [TODO]

### TASK-034: HelpDialog
- **Descripción**: Diálogo de ayuda y documentación
- **Contenido**: Manual de usuario, FAQ, ejemplos
- **Métodos**: showHelp(), searchHelp()
- **Estado**: [TODO]

## Dependencias

- Depende de: models, validation, comparison, reporting
- Depende de: parsers (para cargar archivos)
- Framework: Swing (incluido en JDK) o JavaFX

## Archivos Esperados

```
gui/
├── MainWindow.java
├── CatalogPanel.java
├── ApplicationPanel.java
├── ValidationPanel.java
├── ComparisonPanel.java
├── ResultsPanel.java
├── SettingsDialog.java
├── HelpDialog.java
├── resources/
│   ├── icons/
│   ├── styles/
│   └── help/
└── README.md
```

## Componentes Principales

### Menú Principal
- File (Open, Save, Exit)
- Edit (Preferences)
- Tools (Validate, Compare)
- Help (About, Documentation)

### Toolbar
- Botones rápidos: Open Catalog, Open App, Validate, Compare
- Indicador de estado

### Tabs/Panels
- Tab 1: Catálogos
- Tab 2: Aplicaciones
- Tab 3: Validación
- Tab 4: Comparación
- Tab 5: Resultados

## Recomendaciones

### Swing vs JavaFX
- **Swing**: Simple, menos peso, bien conocido
- **JavaFX**: Moderno, mejor gráficos, más peso

Recomendación: **Swing** para MVP, **JavaFX** si se requiere más capas

## Funcionalidades de GUI

- Drag & drop de archivos
- Árbol jerárquico de atributos
- Búsqueda/filtrado
- Sorting de tablas
- Exportar resultados (HTML, Excel)
- Historial de operaciones
- Undo/Redo básico

## Usabilidad

- Mensajes de error claros
- Confirmaciones antes de acciones destructivas
- Progress indicators para operaciones largas
- Tooltips en botones
- Atajos de teclado (Ctrl+O, Ctrl+S, etc.)
- Responsivo a cambios de tamaño

## Notas

- Usar Layout Managers (BorderLayout, GridLayout)
- Separar lógica de presentación
- Usar Model-View-Controller (MVC)
- Internacionalización (i18n) para múltiples idiomas
- Temas claros y oscuros (opcional)
