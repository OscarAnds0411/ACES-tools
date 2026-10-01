# Paquete: reporting

## Descripción

Contiene la lógica para generar reportes de validación y comparación.

## Clases

### TASK-022: ReportGenerator
- **Descripción**: Generador base de reportes
- **Métodos**: generate(), export(), format()
- **Formatos soportados**: HTML, PDF, Excel, JSON, XML
- **Estado**: [TODO]

### TASK-023: ValidationReportBuilder
- **Descripción**: Constructor de reportes de validación
- **Métodos**: buildReport(), addSection(), formatResults()
- **Contenido**: Resumen, errores, advertencias, estadísticas
- **Estado**: [TODO]

### TASK-024: ComparisonReportBuilder
- **Descripción**: Constructor de reportes de comparación
- **Métodos**: buildReport(), addDifference(), formatComparison()
- **Contenido**: Cambios, impacto, sugerencias de migración
- **Estado**: [TODO]

### TASK-025: HTMLReportFormatter
- **Descripción**: Formateador de reportes en HTML
- **Métodos**: format(), addStyles(), generateTable()
- **Salida**: HTML con estilos y tablas interactivas
- **Estado**: [TODO]

### TASK-026: ExcelReportExporter
- **Descripción**: Exportador de reportes a Excel
- **Métodos**: export(), createSheet(), formatCell()
- **Utiliza**: fastexcel para escritura eficiente
- **Estado**: [TODO]

## Dependencias

- Depende de: models (ValidationResult, ComparisonResult)
- Depende de: validation y comparison
- Utiliza: fastexcel para exportar a Excel
- Utiliza: commons-io para manejo de archivos

## Librerías Utilizadas

- `fastexcel-0.20.2.jar` - Escritura eficiente a Excel
- `commons-io-2.20.0.jar` - Utilidades de I/O
- `commons-compress-1.28.0.jar` - Compresión (para ZIP)

## Archivos Esperados

```
reporting/
├── ReportGenerator.java
├── ValidationReportBuilder.java
├── ComparisonReportBuilder.java
├── HTMLReportFormatter.java
├── ExcelReportExporter.java
├── resources/
│   └── report-template.html
└── README.md
```

## Formatos de Salida

### HTML
- Estilos CSS incluidos
- Tablas interactivas
- Gráficos (si es posible)

### Excel
- Múltiples sheets por sección
- Formatos y colores
- Fórmulas de suma/estadísticas

### JSON
- Estructura anidada
- Fácil procesamiento
- API-compatible

### PDF
- Opcional (puede usar HTML2PDF)
- Imprimible

## Contenido de Reportes

### Reporte de Validación
1. Resumen (total validado, errores, advertencias)
2. Listado de errores detallados
3. Listado de advertencias
4. Estadísticas por tipo de error
5. Tiempo de procesamiento

### Reporte de Comparación
1. Resumen de cambios
2. Nuevos atributos
3. Atributos eliminados
4. Atributos modificados
5. Análisis de impacto
6. Recomendaciones

## Notas

- Reportes legibles y profesionales
- Incluir logos/marcas si aplica
- Paginación para reportes grandes
- Optimizar para pantalla e impresión
- Exportar en múltiples formatos
