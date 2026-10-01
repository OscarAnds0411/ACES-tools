# Paquete: parsers

## Descripción

Contiene los parsers para leer y procesar archivos Excel (catálogos y aplicaciones).

## Clases

### TASK-008: ExcelParser
- **Descripción**: Parser base para archivos Excel
- **Métodos**: parseFile(), getSheetNames(), getSheetData()
- **Utiliza**: fastexcel-reader para lectura eficiente
- **Estado**: [TODO]

### TASK-009: CatalogExcelParser
- **Descripción**: Parser específico para catálogos de atributos ACES
- **Entrada**: archivo Excel con estructura de catálogo
- **Salida**: objetos Catalog con ProductLine y Attribute
- **Métodos**: parseCatalog(), parseProductLines(), parseAttributes()
- **Estado**: [TODO]

### TASK-010: ApplicationExcelParser
- **Descripción**: Parser específico para aplicaciones a validar
- **Entrada**: archivo Excel con datos de aplicación
- **Salida**: objetos Application con Attribute
- **Métodos**: parseApplication(), parseAttributes()
- **Estado**: [TODO]

### TASK-011: DataValidator (Parsers)
- **Descripción**: Valida la integridad de datos al parsear
- **Métodos**: validateCatalogStructure(), validateApplicationStructure()
- **Estado**: [TODO]

### TASK-012: ParserFactory
- **Descripción**: Factory para crear parsers apropiados
- **Métodos**: createParser(), getParserForFile()
- **Estado**: [TODO]

## Dependencias

- Depende de: models (Catalog, Application, etc.)
- Utiliza: fastexcel-reader-0.20.2.jar

## Librerías Utilizadas

- `fastexcel-reader-0.20.2.jar` - Lectura eficiente de Excel
- `commons-io-2.20.0.jar` - Utilidades de I/O
- `commons-lang3-3.18.0.jar` - Utilidades de String

## Archivos Esperados

```
parsers/
├── ExcelParser.java
├── CatalogExcelParser.java
├── ApplicationExcelParser.java
├── DataValidator.java
├── ParserFactory.java
└── README.md
```

## Formatos Esperados

### Catálogo Excel
- Sheet 1: "ProductLines" (id, name, description)
- Sheets adicionales: Por cada línea de producto con sus atributos
- Columnas: id, name, type, required, validation, allowedValues

### Aplicación Excel
- Sheet 1: "Application" (id, name, version, description)
- Sheet 2: "Attributes" (attributeId, value, productLine)

## Notas

- Usar fastexcel para mejor performance
- Validar estructuras mientras se parsea
- Manejo robusto de excepciones
- Logs detallados de parseo
