# Documento de Diseño Técnico: Validador de Atributos ACES

## 1. Overview

El Validador de Atributos ACES es una herramienta Java con interfaz gráfica que automatiza la auditoría de archivos Excel ACES validando la completitud de atributos contra un catálogo de requisitos organizados por línea de producto. 

### Propósito
- **Objetivo Principal**: Automatizar la validación de compliance de atributos en aplicaciones
- **Usuarios Finales**: Auditores sin experiencia técnica
- **Salida**: Reporte detallado en una nueva hoja del archivo ACES, preservando archivos originales
- **Enfoque**: Interfaz intuitiva con mensajes claros en español

### Características Clave
1. Carga de catálogo de atributos por línea de producto
2. Carga de archivo ACES con aplicaciones a auditar
3. Comparación automática de atributos requeridos vs. presentes
4. Generación de matriz de compliance
5. Estadísticas agregadas de compliance
6. Reporte exportado a nueva hoja del archivo ACES
7. Preservación total de archivos originales

---

## 2. Arquitectura de Alto Nivel

### 2.1 Componentes Principales

```
┌─────────────────────────────────────────────────────┐
│          Interfaz Gráfica (GUI Layer)               │
│  - MainWindow (ventana principal)                   │
│  - FileDialogManager (selección de archivos)        │
│  - StatusPanel (estado actual)                      │
│  - ErrorDialog (mensajes de error)                  │
└────────────┬──────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────┐
│       Business Logic Layer (Controladores)         │
│  - CatalogController                               │
│  - AcesController                                  │
│  - ValidationController                           │
│  - ComparisonController                           │
│  - ReportGeneratorController                      │
└────────────┬──────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────┐
│         Core Logic & Data Models Layer             │
│  - Catalog, CatalogEntry, ProductLine, Attribute  │
│  - AcesFile, Application, ApplicationAttribute    │
│  - ComparisonResult, AuditReport, Statistic       │
│  - Comparator, ComplianceCalculator               │
│  - ReportGenerator, ReportPrinter                 │
└────────────┬──────────────────────────────────────┘
             │
┌────────────▼──────────────────────────────────────┐
│      File I/O & Persistence Layer                 │
│  - ExcelFileReader (fastexcel)                    │
│  - ExcelFileWriter (fastexcel)                    │
│  - CatalogParser                                  │
│  - AcesParser                                     │
│  - ReportExporter                                 │
│  - FileValidator                                  │
│  - ErrorHandler                                   │
└────────────────────────────────────────────────────┘
```

### 2.2 Flujo de Datos Principal

```
1. ENTRADA
   ├─ Archivo Catálogo (XLSX)
   └─ Archivo ACES (XLSX)
            ↓
2. VALIDACIÓN
   ├─ FileValidator (formato, estructura, columnas)
   ├─ CatalogParser (extrae líneas y atributos)
   └─ AcesParser (extrae aplicaciones y sus atributos)
            ↓
3. COMPARACIÓN
   ├─ Comparator (itera aplicaciones)
   ├─ Por cada aplicación:
   │  ├─ Identificar línea de producto
   │  ├─ Buscar atributos requeridos en catálogo
   │  └─ Marcar atributos como Present/Missing
   └─ → ComparisonResult
            ↓
4. GENERACIÓN DE REPORTE
   ├─ ReportGenerator recibe ComparisonResult
   ├─ Genera:
   │  ├─ Matriz de Compliance (apps × atributos)
   │  ├─ Lista de Atributos Faltantes
   │  └─ Estadísticas Generales (totales, promedios)
   └─ → AuditReport
            ↓
5. EXPORTACIÓN
   ├─ ReportExporter recibe AuditReport
   ├─ Abre archivo ACES original (read-only buffer)
   ├─ Crea nueva hoja: "Reporte Auditoría ACES"
   ├─ Escribe contenido del reporte con ReportPrinter
   ├─ Verifica integridad de hojas originales
   └─ Guarda archivo en misma ubicación
            ↓
6. SALIDA
   └─ Archivo ACES con nueva hoja "Reporte Auditoría ACES"
      (Archivos originales preservados sin cambios)
```

### 2.3 Separación de Responsabilidades

| Capa | Responsabilidad | Componentes |
|------|-----------------|------------|
| **GUI Layer** | Interfaz usuario, eventos, estado UI | MainWindow, FileDialogManager, StatusPanel, ErrorDialog |
| **Controller Layer** | Orquestación, flujo de operaciones | CatalogController, AcesController, ValidationController, ComparisonController |
| **Business Logic** | Algoritmos, cálculos, transformaciones | Comparator, ComplianceCalculator, ReportGenerator, ReportPrinter |
| **Data Models** | Representación de datos | Catalog, Application, ComparisonResult, AuditReport |
| **I/O Layer** | Lectura/escritura archivos | ExcelFileReader, ExcelFileWriter, CatalogParser, AcesParser, ReportExporter |
| **Error Handling** | Validación, manejo excepciones | FileValidator, ErrorHandler |

---

## 3. Modelos de Datos (Clases Principales)

### 3.1 Estructura del Catálogo

```java
// Representa un atributo con su estado
public class Attribute {
    private String name;           // "Autenticación", "Encriptación", etc.
    private AttributeStatus status; // REQUIRED, OPTIONAL, NOT_NEEDED
    
    public enum AttributeStatus {
        REQUIRED("Requerido"),
        OPTIONAL("Opcional"),
        NOT_NEEDED("No necesario");
        
        private String label;
    }
}

// Representa una línea de producto con sus atributos
public class ProductLine {
    private String name;                    // "Portal", "Móvil", "Backend"
    private List<Attribute> attributes;     // Lista de atributos
    
    public Attribute getRequiredAttributes()     // Solo status=REQUIRED
    public Attribute getOptionalAttributes()     // Solo status=OPTIONAL
}

// Representa el catálogo completo
public class Catalog {
    private Map<String, ProductLine> productLines;  // name → ProductLine
    private int totalAttributes;                    // Total de atributos únicos
    
    public ProductLine getProductLine(String name)
    public List<ProductLine> getAllProductLines()
    public int getProductLineCount()
    public int getTotalAttributeCount()
}
```

### 3.2 Estructura de la Aplicación (ACES)

```java
// Representa un atributo presente en una aplicación
public class ApplicationAttribute {
    private String name;           // "Autenticación", etc.
    private boolean present;       // ¿Está presente?
    private String value;          // Valor del atributo (si aplica)
}

// Representa una aplicación en el archivo ACES
public class Application {
    private String id;             // Identificador único
    private String name;           // Nombre de la aplicación
    private String productLine;    // Línea de producto asignada (puede ser null)
    private List<ApplicationAttribute> attributes;  // Atributos presentes
    private boolean isClassified;  // ¿Tiene línea de producto en catálogo?
    
    public boolean hasAttribute(String attributeName)
    public ApplicationAttribute getAttribute(String attributeName)
    public List<String> getAttributeNames()
}

// Representa el archivo ACES completo
public class AcesFile {
    private String filePath;
    private List<Application> applications;
    private int totalApplicationsCount;
    private int unclassifiedApplicationsCount;
    
    public List<Application> getAllApplications()
    public List<Application> getClassifiedApplications()
    public List<Application> getUnclassifiedApplications()
    public int getApplicationCount()
    public int getClassifiedApplicationCount()
}
```

### 3.3 Estructura de Resultados de Auditoría

```java
// Resultado de la comparación para una aplicación
public class ApplicationComparisonResult {
    private String applicationId;
    private String applicationName;
    private String productLine;
    private Map<String, AttributeComparisonStatus> attributeStatus;
    // key: attribute name, value: PRESENT/MISSING/NOT_APPLICABLE
    private int requiredAttributeCount;
    private int presentRequiredAttributes;
    private int missingRequiredAttributes;
    private double compliancePercentage;  // 0-100
    
    public enum AttributeComparisonStatus {
        PRESENT("✓"),
        MISSING("✗"),
        NOT_APPLICABLE("N/A");
    }
}

// Atributo faltante en una aplicación
public class MissingAttribute {
    private String applicationName;
    private String productLine;
    private String attributeName;
    private Attribute.AttributeStatus status;  // REQUIRED vs OPTIONAL
}

// Resultados completos de la comparación
public class ComparisonResult {
    private List<ApplicationComparisonResult> applicationResults;
    private List<MissingAttribute> missingAttributes;
    private List<String> warningsForUnclassifiedApps;
    
    public ApplicationComparisonResult getApplicationResult(String appId)
    public List<MissingAttribute> getMissingAttributesForApp(String appId)
    public int getTotalApplicationsProcessed()
}
```

### 3.4 Estructura del Reporte de Auditoría

```java
// Estadísticas generales del reporte
public class ComplianceStatistics {
    private int totalApplicationsAudited;
    private int applicationsWithFullCompliance;  // 100%
    private int applicationsWithPartialCompliance; // 1-99%
    private int applicationsWithNoCompliance;     // 0%
    private int unclassifiedApplicationsCount;
    private double averageCompliancePercentage;
    
    public String toSummaryStatement()
    // Retorna: "X de Y aplicaciones (Z%) cumplen con todos los atributos requeridos"
}

// Matriz de compliance
public class ComplianceMatrix {
    private List<String> applicationNames;        // Filas
    private List<String> requiredAttributeNames;  // Columnas
    private Map<String, Map<String, String>> matrix; 
    // [appName][attrName] = "✓" | "✗" | "N/A"
    private List<Double> compliancePercentages;   // % por aplicación
    
    public void sort() // Ordena por compliance % ascendente
}

// Reporte completo de auditoría
public class AuditReport {
    private LocalDateTime auditDate;
    private String auditedFileName;
    private String catalogFileName;
    private ComplianceMatrix complianceMatrix;
    private List<MissingAttribute> missingAttributesList;
    private ComplianceStatistics statistics;
    
    public String[] getReportSectionHeaders()
    // ["Título", "Estadísticas", "Matriz", "Faltantes"]
}
```

---

## 4. Algoritmo de Comparación (Comparator)

### 4.1 Algoritmo Principal

```
ENTRADA: Catalog, AcesFile
SALIDA: ComparisonResult

PROCEDIMIENTO Comparar():
  resultados = nueva ComparisonResult()
  
  PARA CADA aplicación EN AcesFile.aplicaciones:
    resultado_app = nuevo ApplicationComparisonResult()
    resultado_app.id = aplicación.id
    resultado_app.nombre = aplicación.nombre
    resultado_app.productLine = aplicación.productLine
    
    SI aplicación.productLine ES NULL O VACÍO:
      resultado_app.isClassified = FALSE
      resultados.agregar_warning("Aplicación sin línea de producto: {nombre}")
      aplicación.markAsUnclassified()
      CONTINUAR  // Saltar a siguiente aplicación
    FIN SI
    
    linea_producto = Catalog.getProductLine(aplicación.productLine)
    
    SI linea_producto NO EXISTE EN catálogo:
      resultado_app.isClassified = FALSE
      resultados.agregar_warning("Línea de producto no encontrada: {productLine}")
      CONTINUAR  // Saltar a siguiente aplicación
    FIN SI
    
    resultado_app.isClassified = TRUE
    
    // Obtener atributos requeridos para esta línea de producto
    atributos_requeridos = linea_producto.getRequiredAttributes()
    atributos_opcionales = linea_producto.getOptionalAttributes()
    
    // COMPARACIÓN DE ATRIBUTOS REQUERIDOS
    contador_presentes = 0
    contador_faltantes = 0
    
    PARA CADA atributo EN atributos_requeridos:
      SI aplicación.hasAttribute(atributo.nombre):
        resultado_app.marcar_presente(atributo.nombre)
        contador_presentes++
      SINO:
        resultado_app.marcar_faltante(atributo.nombre)
        contador_faltantes++
        resultados.agregar_atributo_faltante(
          nueva MissingAttribute(
            applicationName = aplicación.nombre,
            productLine = aplicación.productLine,
            attributeName = atributo.nombre,
            status = AttributeStatus.REQUIRED
          )
        )
      FIN SI
    FIN PARA
    
    // COMPARACIÓN DE ATRIBUTOS OPCIONALES (no afectan compliance %)
    PARA CADA atributo EN atributos_opcionales:
      SI aplicación.hasAttribute(atributo.nombre):
        resultado_app.marcar_presente(atributo.nombre)
      SINO:
        resultado_app.marcar_faltante(atributo.nombre)
        resultados.agregar_atributo_faltante(
          nueva MissingAttribute(
            applicationName = aplicación.nombre,
            productLine = aplicación.productLine,
            attributeName = atributo.nombre,
            status = AttributeStatus.OPTIONAL
          )
        )
      FIN SI
    FIN PARA
    
    // CÁLCULO DE COMPLIANCE %
    resultado_app.requiredAttributeCount = atributos_requeridos.length
    resultado_app.presentRequiredAttributes = contador_presentes
    resultado_app.missingRequiredAttributes = contador_faltantes
    resultado_app.compliancePercentage = calcularCompliancePercentage(
      contador_presentes, 
      atributos_requeridos.length
    )
    
    resultados.agregar_resultado_aplicacion(resultado_app)
  FIN PARA
  
  RETORNAR resultados
FIN PROCEDIMIENTO
```

### 4.2 Cálculo de Compliance %

```
FUNCIÓN calcularCompliancePercentage(presentes: int, total: int) → double:
  SI total == 0:
    RETORNAR 0.0  // Sin atributos requeridos
  FIN SI
  
  percentage = (presentes / total) * 100.0
  RETORNAR ROUND(percentage, 1 decimal)  // e.g., 78.5%
FIN FUNCIÓN
```

### 4.3 Manejo de Casos Especiales

| Caso | Tratamiento |
|------|------------|
| **Aplicación sin línea de producto** | Marcar como unclassified, crear warning, saltar validación de atributos |
| **Línea de producto no en catálogo** | Marcar como unclassified, crear warning, saltar validación de atributos |
| **Aplicación con 0 atributos requeridos** | Compliance = 0% (sin atributos para validar) |
| **Aplicación con todos atributos presentes** | Compliance = 100% |
| **Atributo faltante que es Opcional** | Incluir en lista de faltantes pero marcado como "(Opcional)" |
| **Atributo faltante que es Requerido** | Incluir en lista de faltantes, afecta compliance % |

---

## 5. Generación de Reporte

### 5.1 Componente ReportGenerator

```java
public class ReportGenerator {
    
    /**
     * Genera un reporte completo a partir de resultados de comparación
     */
    public AuditReport generateReport(
        ComparisonResult comparisonResult,
        String auditedFileName,
        String catalogFileName
    ) {
        AuditReport report = new AuditReport();
        report.setAuditDate(LocalDateTime.now());
        report.setAuditedFileName(auditedFileName);
        report.setCatalogFileName(catalogFileName);
        
        // 1. Generar Matriz de Compliance
        ComplianceMatrix matrix = generateComplianceMatrix(comparisonResult);
        matrix.sort();  // Ordenar por compliance % ascendente
        report.setComplianceMatrix(matrix);
        
        // 2. Compilar Lista de Atributos Faltantes
        List<MissingAttribute> missingList = 
            compileMissingAttributesList(comparisonResult);
        report.setMissingAttributesList(missingList);
        
        // 3. Calcular Estadísticas Generales
        ComplianceStatistics statistics = 
            calculateStatistics(comparisonResult);
        report.setStatistics(statistics);
        
        return report;
    }
    
    /**
     * Genera matriz de compliance: aplicaciones × atributos requeridos
     */
    private ComplianceMatrix generateComplianceMatrix(
        ComparisonResult comparisonResult
    ) {
        ComplianceMatrix matrix = new ComplianceMatrix();
        
        // Recopilar todos los nombres de atributos únicos
        Set<String> allRequiredAttributes = new HashSet<>();
        for (ApplicationComparisonResult appResult : 
             comparisonResult.getApplicationResults()) {
            if (appResult.isClassified()) {
                allRequiredAttributes.addAll(
                    appResult.getAttributeStatus().keySet()
                );
            }
        }
        
        matrix.setRequiredAttributeNames(
            new ArrayList<>(allRequiredAttributes)
        );
        
        // Llenar matriz con resultados
        for (ApplicationComparisonResult appResult : 
             comparisonResult.getApplicationResults()) {
            if (appResult.isClassified()) {
                List<String> row = new ArrayList<>();
                for (String attrName : allRequiredAttributes) {
                    String status = appResult.getAttributeStatus(attrName);
                    row.add(status != null ? status : "N/A");
                }
                matrix.addRow(appResult.getApplicationName(), row);
                matrix.addCompliancePercentage(
                    appResult.getCompliancePercentage()
                );
            }
        }
        
        return matrix;
    }
    
    /**
     * Compila lista de atributos faltantes agrupados por aplicación
     */
    private List<MissingAttribute> compileMissingAttributesList(
        ComparisonResult comparisonResult
    ) {
        List<MissingAttribute> missingList = 
            comparisonResult.getMissingAttributes();
        
        // Ordenar alfabéticamente por nombre de aplicación, luego atributo
        missingList.sort((a, b) -> {
            int appCmp = a.getApplicationName()
                .compareTo(b.getApplicationName());
            if (appCmp != 0) return appCmp;
            return a.getAttributeName()
                .compareTo(b.getAttributeName());
        });
        
        return missingList;
    }
    
    /**
     * Calcula estadísticas generales de compliance
     */
    private ComplianceStatistics calculateStatistics(
        ComparisonResult comparisonResult
    ) {
        ComplianceStatistics stats = new ComplianceStatistics();
        
        List<ApplicationComparisonResult> results = 
            comparisonResult.getClassifiedApplicationResults();
        
        int total = results.size();
        int fullCompliance = 0;
        int partialCompliance = 0;
        int noCompliance = 0;
        double sumCompliance = 0.0;
        
        for (ApplicationComparisonResult result : results) {
            double compliance = result.getCompliancePercentage();
            sumCompliance += compliance;
            
            if (compliance == 100.0) {
                fullCompliance++;
            } else if (compliance > 0.0) {
                partialCompliance++;
            } else {
                noCompliance++;
            }
        }
        
        stats.setTotalApplicationsAudited(total);
        stats.setApplicationsWithFullCompliance(fullCompliance);
        stats.setApplicationsWithPartialCompliance(partialCompliance);
        stats.setApplicationsWithNoCompliance(noCompliance);
        stats.setUnclassifiedApplicationsCount(
            comparisonResult.getUnclassifiedCount()
        );
        stats.setAverageCompliancePercentage(
            total > 0 ? sumCompliance / total : 0.0
        );
        
        return stats;
    }
}
```

### 5.2 Componente ReportPrinter

```java
public class ReportPrinter {
    
    /**
     * Convierte un AuditReport a filas/columnas Excel formateadas
     */
    public List<List<Cell>> formatReportForExcel(AuditReport report) {
        List<List<Cell>> rows = new ArrayList<>();
        
        // 1. Sección de Título
        rows.addAll(formatTitleSection(report));
        rows.add(new ArrayList<>()); // Línea en blanco
        
        // 2. Sección de Estadísticas Generales
        rows.addAll(formatStatisticsSection(report.getStatistics()));
        rows.add(new ArrayList<>()); // Línea en blanco
        
        // 3. Sección de Matriz de Compliance
        rows.addAll(formatComplianceMatrixSection(
            report.getComplianceMatrix()
        ));
        rows.add(new ArrayList<>()); // Línea en blanco
        
        // 4. Sección de Atributos Faltantes
        rows.addAll(formatMissingAttributesSection(
            report.getMissingAttributesList()
        ));
        
        return rows;
    }
    
    private List<List<Cell>> formatTitleSection(AuditReport report) {
        List<List<Cell>> rows = new ArrayList<>();
        
        // Título
        List<Cell> titleRow = new ArrayList<>();
        titleRow.add(createCell(
            "Reporte de Auditoría ACES",
            bold = true,
            fontSize = 14
        ));
        rows.add(titleRow);
        
        // Fecha de auditoría
        List<Cell> dateRow = new ArrayList<>();
        dateRow.add(createCell("Fecha: " + report.getAuditDate()));
        rows.add(dateRow);
        
        // Nombre de archivo auditado
        List<Cell> fileRow = new ArrayList<>();
        fileRow.add(createCell("Archivo: " + report.getAuditedFileName()));
        rows.add(fileRow);
        
        // Nombre de catálogo
        List<Cell> catalogRow = new ArrayList<>();
        catalogRow.add(createCell("Catálogo: " + report.getCatalogFileName()));
        rows.add(catalogRow);
        
        return rows;
    }
    
    private List<List<Cell>> formatStatisticsSection(
        ComplianceStatistics stats
    ) {
        List<List<Cell>> rows = new ArrayList<>();
        
        // Encabezado de sección
        List<Cell> headerRow = new ArrayList<>();
        headerRow.add(createCell(
            "ESTADÍSTICAS GENERALES",
            bold = true,
            fontSize = 12
        ));
        rows.add(headerRow);
        
        // Métricas
        rows.add(createMetricRow("Total de aplicaciones auditadas", 
            stats.getTotalApplicationsAudited()));
        rows.add(createMetricRow("Aplicaciones con 100% compliance", 
            stats.getApplicationsWithFullCompliance()));
        rows.add(createMetricRow("Aplicaciones con compliance parcial (1-99%)", 
            stats.getApplicationsWithPartialCompliance()));
        rows.add(createMetricRow("Aplicaciones con 0% compliance", 
            stats.getApplicationsWithNoCompliance()));
        rows.add(createMetricRow("Aplicaciones sin clasificar", 
            stats.getUnclassifiedApplicationsCount()));
        rows.add(createMetricRow("Compliance promedio", 
            formatPercentage(stats.getAverageCompliancePercentage())));
        
        // Resumen
        List<Cell> summaryRow = new ArrayList<>();
        summaryRow.add(createCell(
            stats.toSummaryStatement(),
            italic = true
        ));
        rows.add(summaryRow);
        
        return rows;
    }
    
    private List<List<Cell>> formatComplianceMatrixSection(
        ComplianceMatrix matrix
    ) {
        List<List<Cell>> rows = new ArrayList<>();
        
        // Encabezado de sección
        List<Cell> headerRow = new ArrayList<>();
        headerRow.add(createCell(
            "MATRIZ DE COMPLIANCE",
            bold = true,
            fontSize = 12
        ));
        rows.add(headerRow);
        
        // Encabezados de columnas
        List<Cell> columnHeaders = new ArrayList<>();
        columnHeaders.add(createCell("Aplicación", bold = true));
        for (String attrName : matrix.getRequiredAttributeNames()) {
            columnHeaders.add(createCell(attrName, bold = true));
        }
        columnHeaders.add(createCell("Compliance %", bold = true));
        rows.add(columnHeaders);
        
        // Filas de datos
        List<String> applicationNames = matrix.getApplicationNames();
        for (int i = 0; i < applicationNames.size(); i++) {
            List<Cell> dataRow = new ArrayList<>();
            dataRow.add(createCell(applicationNames.get(i)));
            
            Map<String, String> appData = 
                matrix.getRowData(applicationNames.get(i));
            for (String attrName : matrix.getRequiredAttributeNames()) {
                String status = appData.get(attrName);
                Cell cell = createCell(status);
                
                // Colorear según estado
                if ("✓".equals(status)) {
                    cell.setBackgroundColor(Color.GREEN);
                } else if ("✗".equals(status)) {
                    cell.setBackgroundColor(Color.RED);
                }
                dataRow.add(cell);
            }
            
            // Agregar compliance %
            double compliance = matrix.getCompliancePercentage(i);
            dataRow.add(createCell(formatPercentage(compliance)));
            
            rows.add(dataRow);
        }
        
        return rows;
    }
    
    private List<List<Cell>> formatMissingAttributesSection(
        List<MissingAttribute> missingAttributes
    ) {
        List<List<Cell>> rows = new ArrayList<>();
        
        // Encabezado de sección
        List<Cell> headerRow = new ArrayList<>();
        headerRow.add(createCell(
            "ATRIBUTOS FALTANTES",
            bold = true,
            fontSize = 12
        ));
        rows.add(headerRow);
        
        if (missingAttributes.isEmpty()) {
            List<Cell> emptyRow = new ArrayList<>();
            emptyRow.add(createCell(
                "No hay atributos faltantes.",
                italic = true
            ));
            rows.add(emptyRow);
            return rows;
        }
        
        // Encabezados
        List<Cell> columnHeaders = new ArrayList<>();
        columnHeaders.add(createCell("Aplicación", bold = true));
        columnHeaders.add(createCell("Línea de Producto", bold = true));
        columnHeaders.add(createCell("Atributo Faltante", bold = true));
        columnHeaders.add(createCell("Estado", bold = true));
        rows.add(columnHeaders);
        
        // Datos
        String lastAppName = null;
        for (MissingAttribute missing : missingAttributes) {
            List<Cell> dataRow = new ArrayList<>();
            
            // Mostrar nombre de aplicación solo si cambia
            if (!missing.getApplicationName().equals(lastAppName)) {
                dataRow.add(createCell(missing.getApplicationName()));
                lastAppName = missing.getApplicationName();
            } else {
                dataRow.add(createCell(""));
            }
            
            dataRow.add(createCell(missing.getProductLine()));
            dataRow.add(createCell(missing.getAttributeName()));
            
            String statusLabel = 
                missing.getStatus() == AttributeStatus.OPTIONAL 
                    ? missing.getAttributeName() + " (Opcional)"
                    : missing.getAttributeName();
            dataRow.add(createCell(missing.getStatus().toString()));
            
            rows.add(dataRow);
        }
        
        return rows;
    }
}
```

---

## 6. Componentes de Interfaz Gráfica (GUI)

### 6.1 Ventana Principal (MainWindow)

```java
public class MainWindow extends JFrame {
    
    private JButton loadCatalogButton;
    private JButton loadAcesButton;
    private JButton executeAuditButton;
    private JButton exitButton;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JLabel catalogStatusLabel;
    private JLabel acesStatusLabel;
    
    public MainWindow() {
        setTitle("Validador de Atributos ACES");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        
        initializeComponents();
        setupLayout();
        setupListeners();
    }
    
    private void initializeComponents() {
        loadCatalogButton = new JButton("Cargar Catálogo");
        loadAcesButton = new JButton("Cargar ACES");
        executeAuditButton = new JButton("Ejecutar Auditoría");
        executeAuditButton.setEnabled(false);  // Disabled until both files loaded
        exitButton = new JButton("Salir");
        
        statusPanel = new JPanel();
        statusLabel = new JLabel("Estado: Esperando carga de archivos...");
        catalogStatusLabel = new JLabel("Catálogo: No cargado");
        acesStatusLabel = new JLabel("ACES: No cargado");
    }
    
    private void setupLayout() {
        // Panel de botones
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.add(loadCatalogButton);
        buttonPanel.add(loadAcesButton);
        buttonPanel.add(executeAuditButton);
        buttonPanel.add(exitButton);
        
        // Panel de estado
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Estado"));
        statusPanel.add(statusLabel);
        statusPanel.add(Box.createVerticalStrut(5));
        statusPanel.add(catalogStatusLabel);
        statusPanel.add(Box.createVerticalStrut(5));
        statusPanel.add(acesStatusLabel);
        
        // Layout principal
        setLayout(new BorderLayout(10, 10));
        add(buttonPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupListeners() {
        loadCatalogButton.addActionListener(e -> {
            disableAllButtons();
            statusLabel.setText("Estado: Cargando catálogo...");
            // Dispara evento para controlador
        });
        
        loadAcesButton.addActionListener(e -> {
            disableAllButtons();
            statusLabel.setText("Estado: Cargando ACES...");
            // Dispara evento para controlador
        });
        
        executeAuditButton.addActionListener(e -> {
            disableAllButtons();
            statusLabel.setText("Estado: Ejecutando auditoría...");
            // Dispara evento para controlador
        });
        
        exitButton.addActionListener(e -> {
            System.exit(0);
        });
    }
    
    public void enableButtons() {
        loadCatalogButton.setEnabled(true);
        loadAcesButton.setEnabled(true);
        if (isCatalogLoaded() && isAcesLoaded()) {
            executeAuditButton.setEnabled(true);
        }
    }
    
    public void disableAllButtons() {
        loadCatalogButton.setEnabled(false);
        loadAcesButton.setEnabled(false);
        executeAuditButton.setEnabled(false);
    }
    
    public void updateCatalogStatus(int productLineCount, int attributeCount) {
        catalogStatusLabel.setText(
            "Catálogo: Cargado (" + productLineCount + " líneas, " + 
            attributeCount + " atributos)"
        );
    }
    
    public void updateAcesStatus(int applicationCount, int attributeCount) {
        acesStatusLabel.setText(
            "ACES: Cargado (" + applicationCount + " aplicaciones, " +
            attributeCount + " atributos)"
        );
    }
}
```

### 6.2 Flujo de Control de Botones

```
ESTADO INICIAL:
  - Cargar Catálogo: ENABLED
  - Cargar ACES: ENABLED
  - Ejecutar Auditoría: DISABLED
  - Salir: ENABLED

DESPUÉS DE CARGAR CATÁLOGO:
  - Cargar Catálogo: ENABLED
  - Cargar ACES: ENABLED
  - Ejecutar Auditoría: DISABLED (si ACES no cargado) o ENABLED (si ACES cargado)
  - Salir: ENABLED
  - Estado: "Catálogo cargado: X líneas, Y atributos"

DESPUÉS DE CARGAR ACES:
  - Cargar Catálogo: ENABLED
  - Cargar ACES: ENABLED
  - Ejecutar Auditoría: DISABLED (si Catálogo no cargado) o ENABLED (si Catálogo cargado)
  - Salir: ENABLED
  - Estado: "ACES cargado: X aplicaciones, Y atributos"

DURANTE OPERACIÓN (archivo cargando o auditoría ejecutándose):
  - Cargar Catálogo: DISABLED
  - Cargar ACES: DISABLED
  - Ejecutar Auditoría: DISABLED
  - Salir: DISABLED
  - Estado: "Operación en progreso..."

DESPUÉS DE OPERACIÓN EXITOSA:
  - Botones: Restaurados a estado anterior
  - Estado: Mensaje de éxito
```

### 6.3 Diálogos de Error

```java
public class ErrorDialog extends JDialog {
    
    public static void showError(
        Window owner, 
        String title, 
        String message
    ) {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Icono de error
        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
        contentPanel.add(iconLabel, BorderLayout.WEST);
        
        // Mensaje
        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        contentPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        // Botón OK
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(contentPanel);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
```

---

## 7. Lectura/Escritura de Archivos Excel

### 7.1 Estructura Esperada del Catálogo

| Columna | Tipo | Ejemplo | Notas |
|---------|------|---------|-------|
| **Línea de Producto** | String | "Portal" | Requerido, único por fila |
| **Atributo** | String | "Autenticación" | Requerido |
| **Estado** | String | "Requerido" | Requerido: una de ["Requerido", "Opcional", "No necesario"] |

**Validaciones**:
- Columnas requeridas deben existir exactamente como se especifica
- No se permiten filas vacías
- "Línea de Producto" y "Atributo" no deben estar vacíos
- "Estado" debe ser exactamente "Requerido", "Opcional", o "No necesario"

**Ejemplo**:
```
Línea de Producto | Atributo        | Estado
Portal            | Autenticación   | Requerido
Portal            | Encriptación    | Requerido
Portal            | Auditoría       | Opcional
Móvil             | Autenticación   | Requerido
Móvil             | Sincronización  | Requerido
```

### 7.2 Estructura Esperada del Archivo ACES

| Columna | Tipo | Ejemplo | Notas |
|---------|------|---------|-------|
| **Aplicación ID** | String | "APP001" | Identificador único |
| **Aplicación Nombre** | String | "Sistema Portal" | Nombre descriptivo |
| **Línea de Producto** | String | "Portal" | Referencia a catálogo, puede ser NULL/vacío |
| **Atributos Presentes** | String | "Autenticación,Encriptación" | Lista separada por comas o columnas individuales |

**Validaciones**:
- Columnas mínimas requeridas
- "Aplicación ID" no debe estar vacío
- "Aplicación Nombre" no debe estar vacío
- "Línea de Producto" puede estar vacío (será marcada como unclassified)
- Atributos pueden estar en múltiples columnas o como lista separada por comas

**Ejemplo**:
```
Aplicación ID | Aplicación Nombre  | Línea de Producto | Autenticación | Encriptación | Auditoría
APP001        | Sistema Portal     | Portal           | X             | X            | 
APP002        | App Móvil          | Móvil            | X             | X            | X
APP003        | Servicio Backend   |                  | X             |              | X
```

### 7.3 Estructura del Reporte Generado

**Nombre de Hoja**: "Reporte Auditoría ACES" (o "Reporte Auditoría ACES [YYYYMMDD_HHMMSS]" si existe conflicto)

**Secciones** (en orden):
1. **Título**
   - Encabezado: "Reporte de Auditoría ACES" (bold, 14pt)
   - Fecha: "Fecha: [LocalDateTime]"
   - Archivo Auditado: "Archivo: [nombreArchivo]"
   - Catálogo: "Catálogo: [nombreCatálogo]"

2. **Estadísticas Generales**
   - Encabezado: "ESTADÍSTICAS GENERALES" (bold, 12pt)
   - Métricas (cada una en una fila):
     - "Total de aplicaciones auditadas: X"
     - "Aplicaciones con 100% compliance: X"
     - "Aplicaciones con compliance parcial (1-99%): X"
     - "Aplicaciones con 0% compliance: X"
     - "Aplicaciones sin clasificar: X"
     - "Compliance promedio: X.X%"
   - Resumen: "X de Y aplicaciones (Z%) cumplen con todos los atributos requeridos"

3. **Matriz de Compliance**
   - Encabezado: "MATRIZ DE COMPLIANCE" (bold, 12pt)
   - Tabla:
     - Columna 1: "Aplicación"
     - Columnas 2 a N: Nombres de atributos requeridos
     - Columna N+1: "Compliance %"
     - Valores: "✓" (verde), "✗" (rojo), "N/A" (gris)
     - Ordenado por Compliance % ascendente (problemas primero)

4. **Atributos Faltantes**
   - Encabezado: "ATRIBUTOS FALTANTES" (bold, 12pt)
   - Tabla:
     - Columnas: "Aplicación", "Línea de Producto", "Atributo Faltante", "Estado"
     - Agrupado por Aplicación
     - Ordenado alfabéticamente dentro de cada aplicación
     - Atributos Opcionales marcados con "(Opcional)"
   - Si no hay faltantes: "No hay atributos faltantes."

### 7.4 Uso de FastExcel para I/O

```java
public class ExcelFileReader {
    
    /**
     * Lee un archivo XLSX usando FastExcel
     */
    public static List<List<String>> readExcelFile(String filePath) 
        throws IOException {
        List<List<String>> data = new ArrayList<>();
        
        try (ReadableWorkbook workbook = 
             new ReadableWorkbook(new File(filePath))) {
            
            // Leer la primera hoja
            workbook.getFirstSheet().ifPresent(sheet -> {
                for (Row row : sheet.read()) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        rowData.add(cell.asString());
                    }
                    data.add(rowData);
                }
            });
        }
        
        return data;
    }
}

public class ExcelFileWriter {
    
    /**
     * Escribe datos a un archivo XLSX usando FastExcel
     */
    public static void writeExcelFile(
        String filePath, 
        List<List<String>> data
    ) throws IOException {
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {
            
            // Crear nueva hoja
            Sheet newSheet = workbook.createSheet("Reporte");
            
            // Escribir datos
            for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
                Row row = newSheet.createRow(rowIdx);
                for (int colIdx = 0; colIdx < data.get(rowIdx).size(); colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    cell.setCellValue(data.get(rowIdx).get(colIdx));
                }
            }
            
            // Guardar archivo
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }
}
```

---

## 8. Manejo de Errores

### 8.1 Jerarquía de Excepciones

```
Exception
├── IOException
│   └── ExcelFileException (extends IOException)
│       ├── FileNotFoundException ("El archivo no existe")
│       ├── FileAccessException ("No tiene permisos")
│       ├── FileFormatException ("Archivo Excel corrupto o inválido")
│       └── FileWriteException ("Error al guardar archivo")
│
├── ValidationException
│   ├── CatalogValidationException
│   │   ├── MissingColumnException
│   │   ├── InvalidCatalogStructureException
│   │   └── InvalidAttributeStatusException
│   │
│   ├── AcesValidationException
│   │   ├── MissingColumnException
│   │   ├── InvalidAcesStructureException
│   │   └── MissingApplicationDataException
│   │
│   └── GeneralValidationException
│       ├── FilesNotLoadedException
│       ├── CatalogEmptyException
│       └── AcesEmptyException
│
└── ProcessingException
    ├── ComparisonException
    ├── ReportGenerationException
    └── ExportException
```

### 8.2 Puntos de Validación y Errores Posibles

| Punto de Validación | Errores Posibles | Mensaje Usuario |
|-------------------|------------------|-----------------|
| **Selección Archivo** | Archivo no existe | "El archivo no existe. Verifique la ruta: [ruta]" |
| | Sin permisos lectura | "No tiene permisos para leer el archivo. Verifique los permisos." |
| **Validación Formato** | No es .xlsx | "El archivo debe estar en formato .xlsx" |
| | Archivo corrupto | "El archivo Excel está corrupto o no se puede leer" |
| **Validación Estructura Catálogo** | Faltan columnas | "Formato de catálogo inválido: falta columna [nombre]" |
| | Estado inválido | "Formato de catálogo inválido: estado debe ser Requerido/Opcional/No necesario" |
| | Está vacío | "El catálogo no contiene datos" |
| **Validación Estructura ACES** | Faltan columnas | "Formato ACES inválido: falta columna [nombre]" |
| | Está vacío | "Formato ACES inválido o sin datos" |
| | Sin headers | "El archivo no tiene estructura ACES válida" |
| **Validación Antes Auditoría** | Falta Catálogo | "Cargue tanto el Catálogo como el archivo ACES antes de continuar" |
| | Falta ACES | "Cargue tanto el Catálogo como el archivo ACES antes de continuar" |
| | Catálogo vacío | "El catálogo está vacío" |
| | ACES vacío | "El archivo ACES no contiene aplicaciones" |
| **Escritura Reporte** | Error al guardar | "Error al guardar el reporte: [motivo]" |
| | Datos corruptos | "Error: Datos pueden estar corruptos. Auditoría abortada." |
| **Error Inesperado** | Cualquier otra | "Ocurrió un error inesperado. Por favor, contacte al administrador." |

### 8.3 ErrorHandler y Logging

```java
public class ErrorHandler {
    
    private static final Logger logger = 
        Logger.getLogger(ErrorHandler.class.getName());
    
    /**
     * Maneja excepciones y retorna mensaje apropiado para usuario
     */
    public static String getUserFriendlyMessage(Exception e) {
        logger.log(Level.SEVERE, "Error: ", e);
        
        if (e instanceof FileNotFoundException) {
            return "El archivo no existe. Verifique la ruta: " + 
                   extractFilePath(e);
        }
        
        if (e instanceof FileAccessException) {
            return "No tiene permisos para leer el archivo. " +
                   "Verifique los permisos del archivo.";
        }
        
        if (e instanceof FileFormatException) {
            return "El archivo Excel está corrupto o no se puede leer";
        }
        
        if (e instanceof MissingColumnException) {
            MissingColumnException mce = (MissingColumnException) e;
            return "Formato inválido: falta columna " + mce.getColumnName();
        }
        
        if (e instanceof FilesNotLoadedException) {
            return "Cargue tanto el Catálogo como el archivo ACES " +
                   "antes de continuar";
        }
        
        // Por defecto
        return "Ocurrió un error inesperado. " +
               "Por favor, contacte al administrador.";
    }
    
    private static String extractFilePath(Exception e) {
        // Extrae ruta del archivo del mensaje de error
        String message = e.getMessage();
        // Parsing específico según tipo de excepción
        return message != null ? message : "[archivo desconocido]";
    }
}
```

---

## 9. Correctness Properties (Para Testing)

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### 9.1 Round-Trip Parsing Property

**Property 1: Catalog Parse → Print → Parse Round Trip**

*For any* valid catalog file with N product lines and M total attributes, parsing the file, formatting it for Excel, and parsing the output should produce an equivalent catalog structure with the same N product lines and M attributes, preserving exact spelling, capitalization, and attribute statuses.

**Validates: Requirements 13.8**

**Test Implementation**:
- Generate random valid catalogs with varying product lines and attributes
- Parse initial file → produces Catalog₁
- Format Catalog₁ for Excel → produces ExcelData₁
- Parse ExcelData₁ → produces Catalog₂
- Assert: Catalog₁ == Catalog₂ (structurally equivalent)
- Minimum 100 iterations with varying sizes

---

**Property 2: Comparison Results Round Trip**

*For any* valid set of comparison results (applications, attributes, compliance percentages), writing the results to Excel and parsing them back should preserve all data including: application names, attribute presence states (✓/✗/N/A), compliance percentages, and attribute names with case-sensitive matching.

**Validates: Requirements 14.3, 14.6, 14.7**

**Test Implementation**:
- Generate random comparison results with N applications
- Format results for Excel → produces ExcelData
- Parse ExcelData → produces ParsedResults
- Assert: For each application and attribute:
  - Application name matches (case-sensitive)
  - Attribute presence state matches (✓/✗/N/A)
  - Compliance % equals within ±0.1% tolerance
- Minimum 100 iterations

---

### 9.2 Data Preservation Property

**Property 3: Original File Sheets Remain Unchanged After Export**

*For any* valid ACES file with K existing sheets, after loading the file, executing an audit, and exporting the report, all K original sheets should remain completely unchanged (same data, same formatting), and a new sheet "Reporte Auditoría ACES" (or timestamped variant) should be added without modifying any existing content.

**Validates: Requirements 8.3, 8.4, 12.3, 12.5**

**Test Implementation**:
- Load ACES file with K sheets → capture original sheet contents
- Execute audit and export report
- Re-read ACES file
- Assert: All K original sheets exist with identical content
- Assert: New report sheet exists and contains expected content
- Assert: Total sheet count = K + 1 (or K if one was replaced)

---

**Property 4: In-Memory File Data Not Modified During Audit**

*For any* loaded catalog and ACES file, executing the audit comparison should not modify the original file data stored in memory. After audit completion, re-reading the loaded data structures should produce identical results as before the audit.

**Validates: Requirements 12.2**

**Test Implementation**:
- Load catalog and ACES file
- Capture snapshots of both in-memory structures
- Execute audit
- Re-capture snapshots
- Assert: Snapshots are identical (no modifications)
- Minimum 50 iterations with varying file sizes

---

### 9.3 Compliance Calculation Properties

**Property 5: Compliance Percentage Calculation Formula**

*For any* application with T required attributes where P are present, the compliance percentage should equal exactly (P / T) × 100, rounded to one decimal place. If T = 0, compliance should be 0.0%. If P = T, compliance should be 100.0%.

**Validates: Requirements 5.4, 5.5, 5.6, 7.3**

**Test Implementation**:
- Generate random applications with varying required attributes (0 to 100+)
- For each, mark 0 to T attributes as present
- Calculate compliance %
- Assert: compliance = floor((P/T) × 1000) / 10 (for rounding to 1 decimal)
- Edge cases: T=0 → 0%, P=T → 100%, P=0 → 0%
- Minimum 100 iterations

---

**Property 6: Optional Attributes Don't Affect Compliance Percentage**

*For any* application with R required attributes and O optional attributes, the compliance percentage should be calculated using only the R required attributes, regardless of how many optional attributes are present or missing.

**Validates: Requirements 5.7**

**Test Implementation**:
- Generate applications with required and optional attributes
- Calculate compliance twice: once with optionals marked present, once with optionals missing
- Assert: Both calculations produce same compliance %
- Minimum 100 iterations

---

### 9.4 Determinism Property

**Property 7: Same Input Produces Identical Output (Determinism)**

*For any* valid combination of catalog file and ACES file, executing the audit twice with identical inputs should produce identical reports (same compliance matrix, same statistics, same missing attributes list), except for audit timestamp which may differ.

**Validates: All requirements - general correctness**

**Test Implementation**:
- Load catalog and ACES
- Execute audit → Report₁
- Load same files again (or reset state)
- Execute audit → Report₂
- Compare Report₁ and Report₂:
  - Assert: Matrices identical
  - Assert: Statistics identical
  - Assert: Missing attributes lists identical
  - Assert: Compliance percentages identical
  - Allow timestamp difference
- Minimum 50 iterations

---

### 9.5 Sorting and Ordering Properties

**Property 8: Compliance Matrix Sorted by Compliance % Ascending**

*For any* generated compliance matrix, the rows should be strictly ordered by compliance percentage in ascending order, with the lowest compliance applications first.

**Validates: Requirement 5.8**

**Test Implementation**:
- Generate matrix with N applications
- Extract compliance percentages
- Assert: For all i, compliance[i] ≤ compliance[i+1]
- Minimum 100 iterations

---

**Property 9: Missing Attributes List Sorted Alphabetically Within Application**

*For any* generated missing attributes list, for each application, the missing attributes should be sorted alphabetically by attribute name.

**Validates: Requirements 6.4, 6.6**

**Test Implementation**:
- Generate missing attributes list
- Group by application
- For each application group:
  - Extract attribute names
  - Assert: Names are in alphabetical order
- Minimum 50 iterations

---

### 9.6 Edge Case Properties

**Property 10: Application Without Product Line Treated as Unclassified**

*For any* application without a product line assignment (NULL or empty string), the comparison should mark it as unclassified, create a warning entry, and skip attribute validation without crashing or producing false data.

**Validates: Requirements 2.6, 4.6**

**Test Implementation**:
- Generate applications with NULL/empty product lines
- Execute comparison
- Assert: Marked as unclassified
- Assert: Warning entry created
- Assert: No attribute comparisons performed
- Minimum 20 iterations

---

**Property 11: Unknown Product Line Produces Warning**

*For any* application with a product line that does not exist in the catalog, the comparison should create a warning entry, mark as unclassified, and continue processing without crashing.

**Validates: Requirements 4.6**

**Test Implementation**:
- Create catalog with product lines: [A, B, C]
- Create applications with product lines: [A, B, C, D, E (unknown)]
- Execute comparison
- Assert: Applications with D, E marked unclassified
- Assert: Warnings created for D, E
- Assert: Processing continued for all applications
- Minimum 30 iterations

---

**Property 12: Empty File Handling**

*For any* ACES file or catalog with zero applications/attributes respectively, the system should detect this and return an appropriate error before attempting processing.

**Validates: Requirements 3.4, 3.5, 10.5**

**Test Implementation**:
- Create catalog with 0 attributes
- Create ACES with 0 applications
- Attempt to load and audit
- Assert: Validation fails appropriately
- Assert: User-friendly error messages displayed
- Minimum 10 iterations

---

### 9.7 Format Preservation Properties

**Property 13: Percentage Format Consistency**

*For any* compliance percentage value C, when formatted to string and written to Excel, it should use format "XX.X%" (one decimal place), and when parsed back should convert to numeric value within ±0.1% tolerance.

**Validates: Requirements 14.4, 14.5**

**Test Implementation**:
- Generate random percentages: 0.0, 25.5, 50.0, 75.4, 100.0
- Format as string → verify pattern "XX.X%"
- Parse string back to numeric
- Assert: |original - parsed| ≤ 0.1
- Minimum 50 iterations

---

**Property 14: Attribute Name Case Sensitivity Preservation**

*For any* attribute name with mixed case (e.g., "AutoAuthentication", "ENCRYPTION"), when written to Excel and read back, the exact spelling and capitalization should be preserved.

**Validates: Requirements 14.6, 14.7**

**Test Implementation**:
- Generate attribute names with various capitalizations
- Write to Excel
- Read back from Excel
- Assert: String equality (case-sensitive) with original
- Minimum 50 iterations

---

## 10. Testing Strategy

### 10.1 Dual Testing Approach

#### Unit Tests (Example-Based)
- Specific scenarios and concrete examples
- Edge cases and error conditions
- Specific input/output pairs
- Tests for error messages and user interactions

**Coverage areas**:
- FileValidator: specific file format errors
- CatalogParser: parsing of various catalog structures
- ComparisonResult: correctness of comparison output
- ReportGenerator: section generation with known inputs
- ErrorHandler: correct error message mapping

#### Property-Based Tests (Universal Properties)
- Universal properties that hold for all inputs
- Comprehensive input coverage through randomization
- Generators for valid Catalog, ACES, ComparisonResult objects
- Minimum 100 iterations per property test

**Coverage areas** (see Section 9):
- Round-trip parsing (catalog and results)
- Compliance calculation correctness
- Data preservation during processing
- Determinism of audit results
- Sorting and ordering of results
- Format preservation (percentages, names)

#### Integration Tests
- File I/O with fastexcel
- Complete audit workflow from load to export
- Report generation with real Excel files
- File preservation verification

### 10.2 Property Test Implementation Format

Each property-based test must follow this pattern:

```java
@Property(tries = 100)  // Minimum 100 iterations
void testCompliancePercentageFormula(
    @ForAll List<Application> applications,
    @ForAll Catalog catalog
) {
    // Feature: validador-atributos-aces
    // Property 5: Compliance Percentage Calculation Formula
    
    ComparisonResult result = comparator.compare(catalog, acesFile);
    
    for (ApplicationComparisonResult appResult : result.getApplicationResults()) {
        if (!appResult.isClassified()) continue;
        
        int present = appResult.getPresentRequiredAttributes();
        int total = appResult.getRequiredAttributeCount();
        double expected = total > 0 ? 
            Math.round((double) present / total * 1000) / 10.0 : 0.0;
        double actual = appResult.getCompliancePercentage();
        
        Assertions.assertEquals(expected, actual, 0.01);
    }
}
```

### 10.3 Test Execution Commands

```bash
# Ejecutar todos los tests unitarios
mvn test

# Ejecutar solo tests de propiedades
mvn test -Dgroups=property-test

# Ejecutar tests de integración
mvn test -Dgroups=integration-test

# Ejecutar con cobertura de código
mvn test jacoco:report

# Ejecutar tests específicos
mvn test -Dtest=ComplianceCalculatorPropertyTest
```

---

## 11. Estándar de Codificación

### 11.1 Convenciones de Nombres

| Elemento | Convención | Ejemplo |
|----------|-----------|---------|
| **Clases** | PascalCase | `CatalogParser`, `ComplianceCalculator` |
| **Métodos** | camelCase | `getProductLine()`, `calculateCompliance()` |
| **Constantes** | UPPER_SNAKE_CASE | `MAX_APPLICATIONS`, `DEFAULT_SHEET_NAME` |
| **Variables** | camelCase | `applicationCount`, `compliancePercentage` |
| **Paquetes** | lowercase.dot | `com.aces.validator.parser`, `com.aces.validator.ui` |

### 11.2 Estructura de Paquetes

```
com.aces.validator
├── ui                    // Interfaz gráfica
│   ├── MainWindow
│   ├── FileDialogManager
│   ├── StatusPanel
│   └── ErrorDialog
├── controller            // Controladores (orquestación)
│   ├── CatalogController
│   ├── AcesController
│   ├── ValidationController
│   └── ComparisonController
├── model                 // Modelos de datos
│   ├── Catalog
│   ├── ProductLine
│   ├── Application
│   ├── ComparisonResult
│   └── AuditReport
├── logic                 // Lógica de negocio
│   ├── Comparator
│   ├── ComplianceCalculator
│   ├── ReportGenerator
│   └── ReportPrinter
├── parser                // Parsers de archivos
│   ├── CatalogParser
│   ├── AcesParser
│   └── FileValidator
├── io                    // I/O y persistencia
│   ├── ExcelFileReader
│   ├── ExcelFileWriter
│   ├── ReportExporter
│   └── FileHandler
├── error                 // Manejo de errores
│   ├── ErrorHandler
│   ├── ExcelFileException
│   ├── ValidationException
│   └── ProcessingException
└── util                  // Utilidades
    ├── Constants
    ├── StringUtils
    └── DateUtils
```

---

## 12. Próximos Pasos

1. **Implementación de Capas Inferiores**
   - Modelos de datos (Catalog, Application, etc.)
   - Parsers (CatalogParser, AcesParser)
   - Validadores (FileValidator)

2. **Implementación de Lógica de Negocio**
   - Comparator (algoritmo de comparación)
   - ComplianceCalculator
   - ReportGenerator

3. **Implementación de I/O**
   - ExcelFileReader/Writer con fastexcel
   - ReportExporter
   - Preservación de archivos originales

4. **Implementación de GUI**
   - MainWindow con Swing
   - Controladores
   - Manejo de eventos

5. **Testing**
   - Tests unitarios para cada componente
   - Tests de propiedades para correctness
   - Tests de integración end-to-end

---

## 13. Referencias y Notas de Implementación

### 13.1 Dependencias
- **fastexcel-reader**: Para lectura eficiente de archivos XLSX
- **fastexcel**: Para escritura eficiente de archivos XLSX
- **JUnit 5**: Para testing
- **Swing**: Para interfaz gráfica (incluido en JDK)
- **jqwik o QuickCheck**: Para property-based testing

### 13.2 Consideraciones de Performance
- Usar buffering para archivos grandes
- Lazy loading de hojas de Excel cuando sea posible
- Caché de búsquedas de catálogo (Map<String, ProductLine>)
- Thread-safe operations si se implementa multithreading

### 13.3 Consideraciones de Compatibilidad
- Archivos XLSX con múltiples hojas
- Diferentes versiones de Excel
- Caracteres especiales en nombres
- Fechas y números formateados de varias maneras

