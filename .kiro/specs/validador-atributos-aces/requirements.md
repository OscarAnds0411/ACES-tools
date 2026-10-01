# Requirements Document: Validador de Atributos ACES

## Introduction

El Validador de Atributos ACES es una herramienta Java con interfaz gráfica que audita archivos Excel ACES comparándolos contra un catálogo de atributos requeridos por línea de producto. La herramienta genera un reporte detallado de compliance en una nueva hoja del archivo ACES sin modificar los originales, permitiendo que auditores no técnicos validen la completitud de atributos en sus aplicaciones de manera automática y confiable.

## Glossary

- **Archivo ACES**: Archivo Excel que contiene un listado de aplicaciones con sus atributos presentes. Estructura estándar con headers definidos y múltiples filas de datos.
- **Catálogo de Atributos**: Archivo Excel que define, por cada línea de producto, qué atributos son Requeridos, Opcionales o No necesarios.
- **Línea de Producto**: Categorización organizacional de aplicaciones (ej: "Portal", "Móvil", "Backend", etc.).
- **Atributo**: Propiedad específica de una aplicación (ej: "Autenticación", "Encriptación", "Auditoría", etc.).
- **Reporte de Compliance**: Hoja nueva en el archivo ACES que contiene análisis de atributos faltantes, matriz de compliance y estadísticas agregadas.
- **Matriz de Compliance**: Tabla que muestra, para cada aplicación, qué atributos requeridos están presentes o faltantes.
- **Aplicación**: Una entidad con atributos asociados listada en el archivo ACES.
- **Interfaz Gráfica (GUI)**: Componentes Swing que permiten al usuario cargar archivos, ejecutar validación y visualizar resultados.
- **Auditor**: Usuario final sin experiencia técnica que utiliza la herramienta.

## Requirements

### Requirement 1: Cargar Catálogo de Atributos

**User Story:** Como auditor, quiero cargar un catálogo de atributos organizados por línea de producto, de modo que pueda establecer la base para validar aplicaciones contra estándares específicos de su línea.

#### Acceptance Criteria

1. WHEN the Auditor selects the "Cargar Catálogo" button in the GUI, THE File_Dialog SHALL display an Excel file chooser dialog
2. WHEN the Auditor selects a valid file named "Atributos ACES por línea de producto.xlsx", THE File_Parser SHALL parse the file successfully
3. IF the selected file does not exist or cannot be read, THEN THE GUI SHALL display an error message stating "No se puede leer el archivo: [nombre del archivo]"
4. WHEN the File_Parser parses the catalog, THE Parser SHALL extract all product lines and their associated attributes
5. WHEN the Parser extracts attributes, THE Parser SHALL classify each attribute as one of: Requerido, Opcional, or No necesario
6. IF the catalog file has an invalid structure or missing required columns, THEN THE Parser SHALL return a descriptive error message stating "Formato de catálogo inválido: falta columna [nombre]"
7. WHEN the catalog is loaded successfully, THE GUI SHALL display a summary showing the number of product lines and total attributes loaded
8. THE Catalog_Loader SHALL store the catalog data in memory for subsequent validation operations

### Requirement 2: Cargar Archivo ACES para Auditar

**User Story:** Como auditor, quiero cargar un archivo ACES con las aplicaciones a auditar, de modo que pueda comparar sus atributos contra el catálogo.

#### Acceptance Criteria

1. WHEN the Auditor selects the "Cargar ACES" button in the GUI, THE File_Dialog SHALL display an Excel file chooser dialog
2. WHEN the Auditor selects a valid Excel file, THE File_Parser SHALL parse the file and extract all applications and their attributes
3. IF the selected file does not exist or cannot be read, THEN THE GUI SHALL display an error message stating "No se puede leer el archivo: [nombre del archivo]"
4. WHEN the File_Parser extracts applications, THE Parser SHALL identify the standard headers and read all application rows
5. IF the ACES file lacks standard headers or is empty, THEN THE Parser SHALL return an error message stating "Formato ACES inválido o sin datos"
6. WHEN an application row lacks a product line assignment, THEN THE Parser SHALL log the application as unclassified and continue processing
7. WHEN the ACES file is loaded successfully, THE GUI SHALL display a summary showing the number of applications and attributes found
8. THE File_Loader SHALL store the ACES data in memory for comparison operations

### Requirement 3: Validar que Archivos Estén Listos para Auditoría

**User Story:** Como auditor, quiero que la herramienta valide que ambos archivos (catálogo y ACES) están cargados correctamente, de modo que pueda proceder con confianza a la auditoría.

#### Acceptance Criteria

1. WHEN the Auditor clicks the "Ejecutar Auditoría" button, THE Validator SHALL check if both the catalog and ACES file are loaded in memory
2. IF either the catalog or ACES file is missing, THEN THE GUI SHALL display an error message stating "Cargue tanto el Catálogo como el archivo ACES antes de continuar"
3. WHEN both files are loaded, THE Validator SHALL verify that the catalog contains at least one product line with attributes
4. WHEN both files are loaded, THE Validator SHALL verify that the ACES file contains at least one application
5. IF the catalog is empty or the ACES file contains no applications, THEN THE Validator SHALL display an error message indicating the specific issue
6. WHEN all validations pass, THE Validator SHALL proceed to comparison operations

### Requirement 4: Comparar Atributos Requeridos vs. Presentes

**User Story:** Como auditor, quiero que se comparen automáticamente los atributos requeridos contra los presentes en cada aplicación, de modo que identifique brechas de compliance.

#### Acceptance Criteria

1. WHEN the Validator begins comparison, THE Comparator SHALL iterate through each application in the ACES file
2. FOR each application, THE Comparator SHALL identify its assigned product line from the ACES data
3. WHEN an application's product line is found in the catalog, THE Comparator SHALL extract the list of required attributes for that line
4. WHEN the Comparator has the required attributes, THE Comparator SHALL compare them against the attributes present in the application row
5. FOR each required attribute, THE Comparator SHALL mark it as Present if it exists in the application, or Missing if absent
6. IF an application's product line is not found in the catalog, THEN THE Comparator SHALL create a warning entry and skip attribute validation for that application
7. WHILE processing comparisons, THE Comparator SHALL maintain a detailed log of all present and missing attributes per application
8. WHEN comparison completes, THE Comparator SHALL return a results data structure with all findings

### Requirement 5: Generar Matriz de Compliance

**User Story:** Como auditor, quiero visualizar una matriz clara de compliance que muestre por cada aplicación qué atributos requeridos están presentes o faltantes, de modo que identifique rápidamente problemas de completitud.

#### Acceptance Criteria

1. WHEN the Report_Generator receives comparison results, THE Generator SHALL create a compliance matrix with applications as rows and required attributes as columns
2. FOR each cell in the matrix, THE Generator SHALL display: "✓" if the attribute is present, "✗" if missing, or "N/A" if not applicable for that product line
3. WHEN the matrix is generated, THE Generator SHALL add a "Compliance %" column showing the percentage of required attributes present for each application
4. FOR each application, THE Compliance_Calculator SHALL compute: (Present Required Attributes / Total Required Attributes) × 100
5. IF an application has all required attributes present, THE Compliance_Calculator SHALL show 100%
6. IF an application is missing one or more required attributes, THE Compliance_Calculator SHALL show a percentage less than 100%
7. WHEN optional attributes exist for a product line, THE Generator SHALL include them in the matrix as informational columns (not affecting compliance percentage)
8. THE Generator SHALL sort the matrix by Compliance % in ascending order to highlight problem applications first

### Requirement 6: Listar Atributos Faltantes por Aplicación

**User Story:** Como auditor, quiero una lista clara de atributos faltantes para cada aplicación, de modo que sepa exactamente qué necesita ser agregado.

#### Acceptance Criteria

1. WHEN the Report_Generator generates the report, THE Generator SHALL create a "Atributos Faltantes" section in the report sheet
2. FOR each application with missing required attributes, THE Generator SHALL list all missing attributes with the following information:
   - Application name
   - Product line
   - Missing attribute name
   - Attribute status (Requerido or Opcional)
3. IF an application has no missing required attributes, THE Generator SHALL indicate "Completo - Todos los atributos requeridos presentes"
4. WHEN listing missing attributes, THE Generator SHALL group them by application for clarity
5. WHEN an optional attribute is missing, THE Generator SHALL list it separately with a note "(Opcional)" to distinguish it from required attributes
6. THE Missing_Attributes_Formatter SHALL ensure the list is sorted alphabetically within each application

### Requirement 7: Generar Estadísticas Generales de Compliance

**User Story:** Como auditor, quiero ver estadísticas agregadas de compliance, de modo que entienda el estado general de la auditoría.

#### Acceptance Criteria

1. WHEN the Report_Generator generates the report, THE Generator SHALL create a "Estadísticas Generales" section
2. THE Statistics_Calculator SHALL compute and display the following metrics:
   - Total applications audited
   - Applications with 100% compliance
   - Applications with partial compliance (1-99%)
   - Applications with 0% compliance
   - Average compliance percentage across all applications
3. THE Statistics_Calculator SHALL compute: Average Compliance = (Sum of all Compliance %) / Total Applications
4. WHEN computing statistics, THE Calculator SHALL exclude unclassified applications (those without a product line in the catalog) from compliance metrics
5. THE Statistics_Calculator SHALL display unclassified application counts separately
6. WHEN compliance statistics are computed, THE Formatter SHALL display percentages with one decimal place (e.g., "78.5%")
7. THE Statistics_Section SHALL include a summary statement such as: "X de Y aplicaciones (Z%) cumplen con todos los atributos requeridos"

### Requirement 8: Generar Reporte en Hoja Nueva del Archivo ACES

**User Story:** Como auditor, quiero que el reporte se genere en una nueva hoja del archivo ACES sin modificar el contenido original, de modo que pueda compartir el archivo con el reporte incluido.

#### Acceptance Criteria

1. WHEN the Auditor executes the audit and chooses to export, THE Report_Exporter SHALL create a new sheet named "Reporte Auditoría ACES" in the original ACES file
2. IF a sheet with the same name already exists, THEN THE Exporter SHALL rename the new sheet to "Reporte Auditoría ACES [timestamp]" where timestamp is YYYYMMDD_HHMMSS
3. WHEN the new sheet is created, THE Exporter SHALL preserve all data in existing sheets of the original ACES file unchanged
4. WHEN the report is written to the new sheet, THE Exporter SHALL write the report in the following order:
   - Title section with audit date and file information
   - Estadísticas Generales section
   - Matriz de Compliance section
   - Atributos Faltantes section
5. WHEN the export completes successfully, THE Exporter SHALL save the file to the same directory as the original ACES file with the same filename
6. IF a file write error occurs, THEN THE GUI SHALL display an error message stating "Error al guardar el reporte: [motivo]"
7. WHEN the export is successful, THE GUI SHALL display a confirmation message: "Reporte generado exitosamente: [ruta del archivo]"
8. THE Exporter SHALL use fastexcel library for Excel writing to ensure compatibility and performance

### Requirement 9: Interfaz Gráfica para Carga y Ejecución

**User Story:** Como auditor sin experiencia técnica, quiero una interfaz simple que me guíe paso a paso para cargar archivos y ejecutar la auditoría, de modo que pueda usar la herramienta sin asistencia técnica.

#### Acceptance Criteria

1. WHEN the application starts, THE GUI SHALL display a main window titled "Validador de Atributos ACES"
2. WHEN the main window is displayed, THE GUI SHALL show the following elements:
   - "Cargar Catálogo" button
   - "Cargar ACES" button
   - "Ejecutar Auditoría" button
   - "Salir" button
   - Status panel showing current state (files loaded, audit status)
3. WHEN the Auditor hovers over buttons or loads files, THE GUI SHALL display status messages in a status bar indicating current operation
4. WHEN a button is clicked and an operation is in progress, THE GUI SHALL disable the buttons to prevent concurrent operations
5. WHEN an operation completes, THE GUI SHALL re-enable buttons and update the status panel
6. IF an error occurs during any operation, THE GUI SHALL display an error dialog with a clear, user-friendly message in Spanish
7. WHEN the Auditor clicks "Salir", THE GUI SHALL close the application gracefully
8. THE GUI SHALL use Swing for rendering and SHALL NOT require external UI frameworks

### Requirement 10: Validar Formatos Excel y Estructuras de Archivos

**User Story:** Como auditor, quiero que la herramienta valide automáticamente que los archivos tengan el formato y estructura correctos, de modo que reciba mensajes claros si algo está mal.

#### Acceptance Criteria

1. WHEN the File_Parser attempts to parse an Excel file, THE Parser SHALL verify the file is in .xlsx format
2. IF the file is not in .xlsx format, THEN THE Parser SHALL return an error message: "El archivo debe estar en formato .xlsx"
3. WHEN parsing the catalog file, THE Parser SHALL verify that required columns exist: (Línea de Producto, Atributo, Estado)
4. WHEN parsing the ACES file, THE Parser SHALL verify that required columns exist for applications and attributes
5. IF required columns are missing, THEN THE Parser SHALL return a descriptive error message listing the missing columns
6. WHEN the Parser encounters corrupt or unreadable Excel data, THEN THE Parser SHALL return an error message: "El archivo Excel está corrupto o no se puede leer"
7. WHEN the Parser detects empty rows or cells with unexpected data types, THEN THE Parser SHALL either skip them gracefully or report them clearly in the error message
8. THE Parser SHALL provide validation messages in Spanish suitable for non-technical users

### Requirement 11: Mensajes de Error Descriptivos para Usuarios No Técnicos

**User Story:** Como auditor sin experiencia técnica, quiero recibir mensajes de error claros que expliquen qué salió mal y cómo resolverlo, de modo que pueda corregir problemas sin asistencia técnica.

#### Acceptance Criteria

1. WHEN an error occurs during file loading, THE Error_Handler SHALL generate a message describing: what went wrong, what file was affected, and a suggested action
2. IF a file does not exist, THE Error_Message SHALL state: "El archivo no existe. Verifique la ruta: [ruta proporcionada]"
3. IF a file cannot be read due to permissions, THE Error_Message SHALL state: "No tiene permisos para leer el archivo. Verifique los permisos del archivo."
4. IF the Excel structure is invalid, THE Error_Message SHALL state: "Formato inválido. Esperado: [estructura esperada]. Encontrado: [estructura actual]"
5. IF comparison cannot proceed due to missing data, THE Error_Message SHALL state: "No se puede ejecutar auditoría. Verifique que ambos archivos estén cargados correctamente."
6. WHEN an unexpected internal error occurs, THE Error_Handler SHALL log the full stack trace and display a user-friendly message: "Ocurrió un error inesperado. Por favor, contacte al administrador."
7. ALL error messages SHALL be displayed in user-friendly language in Spanish, avoiding technical jargon
8. THE Error_Handler SHALL ensure messages are concise but informative (1-3 sentences)

### Requirement 12: Preservar Archivos Originales Sin Modificaciones

**User Story:** Como auditor, quiero que los archivos originales nunca se modifiquen durante la auditoría, de modo que pueda tener confianza en la integridad de los datos.

#### Acceptance Criteria

1. WHEN files are loaded for processing, THE File_Loader SHALL read them in read-only mode
2. WHEN the audit executes, THE Comparator SHALL NOT modify the loaded file data in memory
3. WHEN the report is generated, THE Report_Exporter SHALL NOT overwrite the original ACES file; instead, it SHALL add a new sheet
4. IF the Auditor requests to save the report, THE Exporter SHALL save it to the same directory with the same filename, only adding a new sheet
5. WHEN the file is saved, THE Exporter SHALL verify that all original sheets remain intact and unchanged
6. IF a save operation would corrupt the original data, THEN THE Exporter SHALL abort and display an error message
7. WHEN the application closes, THE Application_State SHALL not persist any file modifications to disk
8. THE File_Handler SHALL use file locking or read-only handles to ensure original files cannot be accidentally modified

### Requirement 13: Parser para Catálogo de Atributos con Pretty Printer

**User Story:** Como desarrollador, quiero un parser robusto que lea la estructura del catálogo de atributos y un pretty printer que genere reportes formateados, de modo que la validación sea confiable y los reportes sean legibles.

#### Acceptance Criteria

1. WHEN the Catalog_Parser reads the catalog file, THE Parser SHALL extract all rows with product lines and attribute definitions
2. WHEN the Parser extracts data, THE Parser SHALL parse each attribute record including: product line name, attribute name, and attribute status (Requerido/Opcional/No necesario)
3. WHEN an attribute record is invalid or incomplete, THEN THE Parser SHALL log an error and continue parsing other records
4. WHEN the Parser completes, THE Parser SHALL return a structured Catalog object containing all valid records
5. WHEN the Report_Printer formats the report, THE Printer SHALL convert all compliance data into Excel cells with appropriate formatting
6. THE Report_Printer SHALL apply consistent styling: headers in bold, compliance percentages in percentage format, missing attributes highlighted
7. WHEN the Pretty_Printer generates the report sheet, THE Printer SHALL format columns with appropriate widths for readability
8. FOR ALL valid Catalog objects, parsing then printing then parsing SHALL produce an equivalent catalog structure (round-trip property)

### Requirement 14: Round-Trip Parsing for Report Consistency

**User Story:** Como auditor, quiero tener confianza en que los datos de atributos se preservan exactamente cuando se escriben y leen nuevamente, de modo que el reporte sea una fuente confiable de verdad.

#### Acceptance Criteria

1. WHEN the Report_Generator writes comparison results to Excel, THE Writer SHALL format data in structured columns
2. WHEN another tool or process reads the generated report sheet, THE Reader SHALL be able to re-parse the data without loss of information
3. FOR ALL comparison result sets, writing to Excel then reading back SHALL produce data structures with identical: application names, attribute states, compliance percentages
4. WHEN compliance percentages are written, THE Writer SHALL use consistent decimal formatting (e.g., "78.5%")
5. WHEN the report is read back, THE Reader SHALL correctly parse percentages back to numeric values within ±0.1% tolerance
6. WHEN attribute names are written to the report, THE Writer SHALL preserve exact spelling and capitalization
7. WHEN attribute names are read back, THE Reader SHALL match them exactly (case-sensitive) to original names
8. IF formatting or rounding causes data loss, THEN THE Log SHALL record the discrepancy and the application SHALL alert the Auditor

