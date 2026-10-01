# Validador de Atributos ACES - Plan de Implementación

## Overview

Plan de implementación para el Validador de Atributos ACES. Sistema Java de validación y comparación de atributos de productos contra catálogos maestros, con interfaz gráfica y generación de reportes en Excel.

---

## COMPONENTE 1: MODELOS DE DATOS

### TASK-001: Crear modelo de datos `Catalog`
- **Descripción**: Implementar clase que representa el catálogo maestro de atributos ACES
- **Criterios de Aceptación**:
  - [ ] Clase `Catalog` con propiedades: id, name, version, description, createdDate, productLines
  - [ ] Constructor y getters/setters
  - [ ] Método para agregar ProductLine
  - [ ] Validación de datos básica (no null values críticos)
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### TASK-002: Crear modelo de datos `ProductLine`
- **Descripción**: Implementar clase que representa una línea de producto en el catálogo
- **Criterios de Aceptación**:
  - [ ] Clase `ProductLine` con propiedades: id, name, description, attributes
  - [ ] Constructor y getters/setters
  - [ ] Método para agregar Attribute
  - [ ] Método para buscar atributo por nombre
- **Dependencias**: TASK-001
- **Complejidad**: 1
- **Prioridad**: required

### TASK-003: Crear modelo de datos `Attribute`
- **Descripción**: Implementar clase que representa un atributo individual ACES
- **Criterios de Aceptación**:
  - [ ] Clase `Attribute` con propiedades: id, name, type, required, validation, allowedValues, description
  - [ ] Constructor y getters/setters
  - [ ] Enums para tipos de datos (STRING, INTEGER, DECIMAL, BOOLEAN, DATE)
  - [ ] Método para validar que un valor cumple con especificaciones
- **Dependencias**: TASK-002
- **Complejidad**: 2
- **Prioridad**: required

### TASK-004: Crear modelo de datos `Application`
- **Descripción**: Implementar clase que representa una aplicación a validar
- **Criterios de Aceptación**:
  - [ ] Clase `Application` con propiedades: name, version, data (Map<String, Object>)
  - [ ] Constructor y getters/setters
  - [ ] Método para obtener valor de atributo
  - [ ] Método para establecer valor de atributo
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### TASK-005: Crear modelo de datos `ValidationError`
- **Descripción**: Implementar clase que representa un error de validación
- **Criterios de Aceptación**:
  - [ ] Clase `ValidationError` con propiedades: severity, code, message, attributeName, productLine
  - [ ] Constructor y getters/setters
  - [ ] Enums para severity (ERROR, WARNING, INFO)
  - [ ] Método toString() descriptivo
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### TASK-006: Crear modelo de datos `ComparisonResult`
- **Descripción**: Implementar clase que agrupa resultados de comparación
- **Criterios de Aceptación**:
  - [ ] Clase `ComparisonResult` con propiedades: applicationName, catalogName, totalAttributes, validAttributes, invalidAttributes, errors, warnings, comparisonDate
  - [ ] Constructor y getters/setters
  - [ ] Métodos de estadística: getCompliancePercentage(), getErrorCount(), getWarningCount()
  - [ ] Método para agregar error/warning
- **Dependencias**: TASK-005
- **Complejidad**: 2
- **Prioridad**: required

### TASK-007: Crear modelo de datos `AuditReport`
- **Descripción**: Implementar clase que encapsula información de auditoría
- **Criterios de Aceptación**:
  - [ ] Clase `AuditReport` con propiedades: timestamp, user, action, details, result
  - [ ] Constructor y getters/setters
  - [ ] Enums para acciones (VALIDATION, COMPARISON, EXPORT, IMPORT)
  - [ ] Método para serializar a JSON
- **Dependencias**: TASK-006
- **Complejidad**: 1
- **Prioridad**: optional

---

## COMPONENTE 2: PARSERS

### TASK-008: Crear `CatalogParser` - Estructura base
- **Descripción**: Implementar clase base para parsers del catálogo ACES
- **Criterios de Aceptación**:
  - [ ] Clase abstracta `CatalogParser` con método `parse(File): Catalog`
  - [ ] Estructura de manejo de excepciones
  - [ ] Interfaz para implementaciones específicas (Excel, XML, JSON)
- **Dependencias**: TASK-001, TASK-002, TASK-003
- **Complejidad**: 2
- **Prioridad**: required

### TASK-009: Implementar `ExcelCatalogParser`
- **Descripción**: Parser para leer catálogo desde archivos Excel (.xlsx)
- **Criterios de Aceptación**:
  - [ ] Clase `ExcelCatalogParser` que extiende `CatalogParser`
  - [ ] Leer estructura de hojas: Sheet 1 = Product Lines, Sheet 2+ = Attributes por línea
  - [ ] Mapeo de columnas de Excel a propiedades de Attribute
  - [ ] Manejo de validaciones de formato
  - [ ] Manejo de excepciones de archivo corrupto
- **Dependencias**: TASK-008
- **Complejidad**: 3
- **Prioridad**: required

### TASK-010: Crear `ApplicationParser` - Estructura base
- **Descripción**: Implementar clase base para parsers de aplicaciones ACES
- **Criterios de Aceptación**:
  - [ ] Clase abstracta `ApplicationParser` con método `parse(File): Application`
  - [ ] Estructura de manejo de excepciones
  - [ ] Interfaz para implementaciones específicas (Excel, XML, JSON)
- **Dependencias**: TASK-004
- **Complejidad**: 2
- **Prioridad**: required

### TASK-011: Implementar `ExcelApplicationParser`
- **Descripción**: Parser para leer datos de aplicación desde archivos Excel
- **Criterios de Aceptación**:
  - [ ] Clase `ExcelApplicationParser` que extiende `ApplicationParser`
  - [ ] Leer estructura de hojas con datos de atributos
  - [ ] Mapeo de valores Excel a tipos correctos (String, Integer, Decimal, Boolean, Date)
  - [ ] Manejo de celdas vacías y valores por defecto
  - [ ] Manejo de excepciones de tipo incorrecto
- **Dependencias**: TASK-010
- **Complejidad**: 3
- **Prioridad**: required

### TASK-012: Crear `ValidationSchema`
- **Descripción**: Implementar clase que encapsula esquema de validación
- **Criterios de Aceptación**:
  - [ ] Clase `ValidationSchema` con propiedades: productLine, attributeRules, strictMode
  - [ ] Método para compilar reglas de validación desde Attribute
  - [ ] Método para validar valor contra regla
- **Dependencias**: TASK-003
- **Complejidad**: 2
- **Prioridad**: required

---

## COMPONENTE 3: VALIDACIÓN

### TASK-013: Implementar `Validator` - Estructura base
- **Descripción**: Implementar clase base de validador
- **Criterios de Aceptación**:
  - [ ] Clase abstracta `Validator` con método `validate(Application, Catalog): List<ValidationError>`
  - [ ] Estructura para múltiples estrategias de validación
- **Dependencias**: TASK-004, TASK-001, TASK-005
- **Complejidad**: 2
- **Prioridad**: required

### TASK-014: Implementar `AttributeValidator`
- **Descripción**: Validar que todos los atributos requeridos estén presentes y sean válidos
- **Criterios de Aceptación**:
  - [ ] Clase `AttributeValidator` que extiende `Validator`
  - [ ] Validar presencia de atributos requeridos
  - [ ] Validar tipo de dato correcto
  - [ ] Validar formato según especificación (regex, rango, etc)
  - [ ] Generar `ValidationError` para cada fallo
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 3
- **Prioridad**: required

### TASK-015: Implementar `RangeValidator`
- **Descripción**: Validar valores numéricos dentro de rangos permitidos
- **Criterios de Aceptación**:
  - [ ] Clase `RangeValidator` que extiende `Validator`
  - [ ] Validar rangos para INTEGER y DECIMAL
  - [ ] Validar valores mínimo/máximo
  - [ ] Generar errores descriptivos
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: required

### TASK-016: Implementar `EnumValidator`
- **Descripción**: Validar que valores pertenezcan a lista permitida
- **Criterios de Aceptación**:
  - [ ] Clase `EnumValidator` que extiende `Validator`
  - [ ] Validar contra lista de valores permitidos (allowedValues)
  - [ ] Soporte case-insensitive (configurable)
  - [ ] Generar errores con sugerencias de valores válidos
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: required

### TASK-017: Implementar `CompositeValidator`
- **Descripción**: Ejecutar múltiples validadores en cadena
- **Criterios de Aceptación**:
  - [ ] Clase `CompositeValidator` que extiende `Validator`
  - [ ] Agregar múltiples validadores
  - [ ] Ejecutar todos y recopilar errores
  - [ ] Parar en primer error crítico (configurable)
- **Dependencias**: TASK-013, TASK-014, TASK-015, TASK-016
- **Complejidad**: 2
- **Prioridad**: required

### TASK-018: Implementar `DateFormatValidator`
- **Descripción**: Validar formato de fechas
- **Criterios de Aceptación**:
  - [ ] Clase `DateFormatValidator` que extiende `Validator`
  - [ ] Validar formato ISO 8601 (YYYY-MM-DD)
  - [ ] Soporte para múltiples formatos (configurable)
  - [ ] Validar que la fecha sea válida
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 4: COMPARACIÓN

### TASK-019: Implementar `Comparator`
- **Descripción**: Comparar datos de aplicación contra catálogo maestro
- **Criterios de Aceptación**:
  - [ ] Clase `Comparator` con método `compare(Application, Catalog): ComparisonResult`
  - [ ] Ejecutar validadores apropiados
  - [ ] Agrupar errores por ProductLine
  - [ ] Calcular estadísticas de compliance
  - [ ] Capturar información de auditoría
- **Dependencias**: TASK-017, TASK-006
- **Complejidad**: 3
- **Prioridad**: required

### TASK-020: Implementar `ComplianceCalculator`
- **Descripción**: Calcular porcentaje de compliance detallado
- **Criterios de Aceptación**:
  - [ ] Clase `ComplianceCalculator` con método `calculate(ComparisonResult): ComplianceMetrics`
  - [ ] Calcular compliance global (porcentaje válido)
  - [ ] Calcular compliance por ProductLine
  - [ ] Calcular compliance por tipo de error
  - [ ] Identificar atributos críticos incumplidos
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: required

### TASK-021: Implementar `ComparisonCache`
- **Descripción**: Cache para resultados de comparación previos
- **Criterios de Aceptación**:
  - [ ] Clase `ComparisonCache` con almacenamiento en memoria
  - [ ] Método para guardar resultado con clave (aplicación + catálogo + timestamp)
  - [ ] Método para recuperar resultado
  - [ ] Métodos de limpieza y expiración
  - [ ] Límite de tamaño configurable
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 5: REPORTE

### TASK-022: Crear `ReportGenerator` - Estructura base
- **Descripción**: Clase base para generadores de reporte
- **Criterios de Aceptación**:
  - [ ] Clase abstracta `ReportGenerator` con método `generate(ComparisonResult): Report`
  - [ ] Estructura para múltiples formatos (Excel, PDF, HTML)
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: required

### TASK-023: Implementar `ExcelReportGenerator`
- **Descripción**: Generar reporte de validación en formato Excel
- **Criterios de Aceptación**:
  - [ ] Clase `ExcelReportGenerator` que extiende `ReportGenerator`
  - [ ] Sheet 1: Resumen ejecutivo (compliance, estadísticas generales)
  - [ ] Sheet 2: Errores y warnings (listado detallado con ProductLine, Atributo, Valor, Error)
  - [ ] Sheet 3: Estadísticas por ProductLine
  - [ ] Sheet 4: Atributos válidos (si aplica)
  - [ ] Formateo visual (colores, headers, borders)
- **Dependencias**: TASK-022
- **Complejidad**: 4
- **Prioridad**: required

### TASK-024: Implementar `ReportPrinter`
- **Descripción**: Generar representación textual de reporte para consola
- **Criterios de Aceptación**:
  - [ ] Clase `ReportPrinter` con método `print(ComparisonResult): String`
  - [ ] Resumen ejecutivo con compliance porcentual
  - [ ] Listado de errores con formateo
  - [ ] Estadísticas por ProductLine
  - [ ] Tabla ascii para mejor visualización
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-025: Implementar `ExcelExporter`
- **Descripción**: Exportar resultados válidos a archivo Excel
- **Criterios de Aceptación**:
  - [ ] Clase `ExcelExporter` con método `export(Application, ComparisonResult, File): void`
  - [ ] Exportar solo atributos válidos
  - [ ] Mantener estructura de ProductLines
  - [ ] Incluir metadata (fecha, versión catálogo, compliance)
  - [ ] Manejo de sobrescritura de archivo
- **Dependencias**: TASK-006
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-026: Implementar `ReportFilter`
- **Descripción**: Filtrar resultados de comparación según criterios
- **Criterios de Aceptación**:
  - [ ] Clase `ReportFilter` con métodos para filtrar por: severity, productLine, attributeName
  - [ ] Método para aplicar múltiples filtros
  - [ ] Retornar nueva instancia de `ComparisonResult` filtrada
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 6: INTERFAZ GRÁFICA (GUI)

### TASK-027: Implementar `MainWindow` - Marco principal
- **Descripción**: Ventana principal de la aplicación
- **Criterios de Aceptación**:
  - [ ] JFrame con layout principal
  - [ ] Menú: File (Open Catalog, Open Application, Exit), Tools (Settings), Help (About)
  - [ ] Panel de estado con información de estado
  - [ ] Área central para panels intercambiables
  - [ ] Tamaño inicial 1024x768, resizable
- **Dependencias**: Ninguna
- **Complejidad**: 2
- **Prioridad**: required

### TASK-028: Implementar `CatalogLoadPanel`
- **Descripción**: Panel para cargar catálogo
- **Criterios de Aceptación**:
  - [ ] Botón "Browse" para seleccionar archivo Excel
  - [ ] Campo de texto mostrando ruta del archivo
  - [ ] Botón "Load" para cargar catálogo
  - [ ] Progress bar durante carga
  - [ ] Mostrar información del catálogo cargado (versión, líneas de producto)
  - [ ] Manejo de errores con diálogos
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: required

### TASK-029: Implementar `ApplicationLoadPanel`
- **Descripción**: Panel para cargar aplicación a validar
- **Criterios de Aceptación**:
  - [ ] Botón "Browse" para seleccionar archivo Excel
  - [ ] Campo de texto mostrando ruta del archivo
  - [ ] Botón "Load" para cargar aplicación
  - [ ] Progress bar durante carga
  - [ ] Mostrar información de la aplicación (nombre, versión)
  - [ ] Manejo de errores con diálogos
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: required

### TASK-030: Implementar `ValidationPanel`
- **Descripción**: Panel para ejecutar validación y mostrar resultados
- **Criterios de Aceptación**:
  - [ ] Botón "Validate" para iniciar validación
  - [ ] Progress bar durante validación
  - [ ] Tabla con resultados (ProductLine, Atributo, Valor, Estado, Mensaje)
  - [ ] Filtros (por severity, por ProductLine)
  - [ ] Resumen de compliance (porcentaje)
  - [ ] Botón para exportar reporte
- **Dependencias**: TASK-028, TASK-029
- **Complejidad**: 3
- **Prioridad**: required

### TASK-031: Implementar `StatisticsPanel`
- **Descripción**: Panel mostrando estadísticas de validación
- **Criterios de Aceptación**:
  - [ ] Gráfico de barras: compliance por ProductLine
  - [ ] Gráfico de pie: distribución de errores/warnings
  - [ ] Tabla: estadísticas detalladas
  - [ ] Actualizar automáticamente tras validación
- **Dependencias**: TASK-030
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-032: Implementar diálogos de Archivo
- **Descripción**: Diálogos de selección de archivos
- **Criterios de Aceptación**:
  - [ ] JFileChooser para catálogos (filtro .xlsx)
  - [ ] JFileChooser para aplicaciones (filtro .xlsx)
  - [ ] Recordar último directorio usado
  - [ ] Filtro de archivos apropiado para cada tipo
- **Dependencias**: TASK-027
- **Complejidad**: 1
- **Prioridad**: required

### TASK-033: Implementar diálogos de Configuración
- **Descripción**: Diálogos para configurar opciones de validación
- **Criterios de Aceptación**:
  - [ ] Dialog para seleccionar validadores activos
  - [ ] Opciones de strictness (lenient, normal, strict)
  - [ ] Opción para case-sensitivity en enums
  - [ ] Guardar configuración en archivo propiedades
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-034: Implementar `AuditPanel`
- **Descripción**: Panel mostrando historial de auditoría
- **Criterios de Aceptación**:
  - [ ] Tabla con historial: timestamp, acción, aplicación, catálogo, resultado
  - [ ] Filtros por acción y fecha
  - [ ] Botón para exportar historial
  - [ ] Botón para limpiar historial
- **Dependencias**: TASK-007
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 7: TESTING - PARSERS

### TASK-035: Tests para `CatalogParser`*
- **Descripción**: Unit tests para parser de catálogo
- **Criterios de Aceptación**:
  - [ ] Test: parsear archivo Excel válido
  - [ ] Test: lanzar excepción con archivo inválido
  - [ ] Test: manejar hojas vacías
  - [ ] Test: validar estructura de datos después de parsear
- **Dependencias**: TASK-009
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-036: Tests para `ApplicationParser`*
- **Descripción**: Unit tests para parser de aplicación
- **Criterios de Aceptación**:
  - [ ] Test: parsear archivo Excel válido
  - [ ] Test: lanzar excepción con archivo inválido
  - [ ] Test: manejar tipos de datos mixtos
  - [ ] Test: validar conversión de tipos
- **Dependencias**: TASK-011
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-037: Tests Property-Based: Round-trip Catalog*
- **Descripción**: Test que verifica que parsear y re-serializar catálogo es idéntico
- **Criterios de Aceptación**:
  - [ ] Generar catálogos aleatorios
  - [ ] Serializar a Excel
  - [ ] Parsear de nuevo
  - [ ] Verificar que Catalog.equals() es true
- **Dependencias**: TASK-009
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-038: Tests Property-Based: Round-trip Application*
- **Descripción**: Test que verifica que parsear y re-serializar aplicación es idéntico
- **Criterios de Aceptación**:
  - [ ] Generar aplicaciones aleatorias
  - [ ] Serializar a Excel
  - [ ] Parsear de nuevo
  - [ ] Verificar que Application.equals() es true
- **Dependencias**: TASK-011
- **Complejidad**: 3
- **Prioridad**: optional

---

## COMPONENTE 8: TESTING - VALIDADORES

### TASK-039: Tests para `AttributeValidator`*
- **Descripción**: Unit tests para validador de atributos
- **Criterios de Aceptación**:
  - [ ] Test: atributo válido pasa validación
  - [ ] Test: atributo faltante genera error
  - [ ] Test: tipo incorrecto genera error
  - [ ] Test: valor fuera de rango genera error
- **Dependencias**: TASK-014
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-040: Tests para `RangeValidator`*
- **Descripción**: Unit tests para validador de rangos
- **Criterios de Aceptación**:
  - [ ] Test: valor dentro de rango pasa
  - [ ] Test: valor bajo genera error
  - [ ] Test: valor alto genera error
  - [ ] Test: límites exactos funcionan
- **Dependencias**: TASK-015
- **Complejidad**: 1
- **Prioridad**: optional

### TASK-041: Tests para `EnumValidator`*
- **Descripción**: Unit tests para validador de enumeraciones
- **Criterios de Aceptación**:
  - [ ] Test: valor en lista permitida pasa
  - [ ] Test: valor no en lista genera error
  - [ ] Test: case-insensitive si habilitado
  - [ ] Test: sugerencias de valores válidos
- **Dependencias**: TASK-016
- **Complejidad**: 1
- **Prioridad**: optional

### TASK-042: Tests para `CompositeValidator`*
- **Descripción**: Unit tests para validador compuesto
- **Criterios de Aceptación**:
  - [ ] Test: ejecutar múltiples validadores
  - [ ] Test: recopilar todos los errores
  - [ ] Test: parar en primer error si configurado
- **Dependencies**: TASK-017
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 9: TESTING - COMPARACIÓN E INTEGRACIÓN

### TASK-043: Tests para `Comparator`*
- **Descripción**: Unit tests para comparador
- **Criterios de Aceptación**:
  - [ ] Test: comparar aplicación válida contra catálogo
  - [ ] Test: detectar atributos faltantes
  - [ ] Test: generar ComparisonResult correcto
  - [ ] Test: calcular compliance percentage
- **Dependencias**: TASK-019
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-044: Tests Integration: End-to-end*
- **Descripción**: Test de integración que valida flujo completo
- **Criterios de Aceptación**:
  - [ ] Cargar catálogo desde archivo
  - [ ] Cargar aplicación desde archivo
  - [ ] Ejecutar validación completa
  - [ ] Generar reporte
  - [ ] Verificar salida correcta
- **Dependencias**: TASK-009, TASK-011, TASK-019, TASK-023
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-045: Tests Property-Based: Validation Idempotence*
- **Descripción**: Verificar que validar dos veces produce mismo resultado
- **Criterios de Aceptación**:
  - [ ] Generar aplicación aleatoria
  - [ ] Validar contra catálogo
  - [ ] Validar nuevamente
  - [ ] Verificar que resultados son idénticos
- **Dependencias**: TASK-019
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 10: TESTING - REPORTES

### TASK-046: Tests para `ExcelReportGenerator`*
- **Descripción**: Unit tests para generador de reportes Excel
- **Criterios de Aceptación**:
  - [ ] Test: generar reporte para validación sin errores
  - [ ] Test: generar reporte para validación con errores
  - [ ] Test: verificar que todas las sheets se crean
  - [ ] Test: verificar formato correcto
- **Dependencias**: TASK-023
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-047: Tests Property-Based: Report Completeness*
- **Descripción**: Verificar que reporte contiene todos los errores y warnings
- **Criterios de Aceptación**:
  - [ ] Generar ComparisonResult aleatorio
  - [ ] Generar reporte
  - [ ] Verificar que todos los errores aparecen en reporte
  - [ ] Verificar que compliance percentage es correcto
- **Dependencias**: TASK-023
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 11: DOCUMENTACIÓN

### TASK-048: Documentación de API
- **Descripción**: Generar documentación JavaDoc de todas las clases públicas
- **Criterios de Aceptación**:
  - [ ] JavaDoc comments para todas las clases
  - [ ] JavaDoc para todos los métodos públicos
  - [ ] Ejemplos de uso en comentarios
  - [ ] Generar HTML con javadoc tool
- **Dependencias**: Todas las implementaciones
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-049: Guía de Usuario
- **Descripción**: Crear guía de usuario para GUI
- **Criterios de Aceptación**:
  - [ ] Screenshots anotados de cada panel
  - [ ] Instrucciones paso a paso
  - [ ] Ejemplos de uso común
  - [ ] Solución de problemas
- **Dependencias**: TASK-027 al TASK-034
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-050: Guía de Configuración
- **Descripción**: Crear documentación de configuración
- **Criterios de Aceptación**:
  - [ ] Estructura de archivo propiedades
  - [ ] Parámetros configurables
  - [ ] Valores por defecto
  - [ ] Ejemplos de configuración
- **Dependencias**: TASK-033
- **Complejidad**: 1
- **Prioridad**: optional

---

## COMPONENTE 12: EMPAQUETADO

### TASK-051: Configurar build.xml (Ant)
- **Descripción**: Configurar script de compilación con Apache Ant
- **Criterios de Aceptación**:
  - [ ] Target para compilar (compile)
  - [ ] Target para limpiar (clean)
  - [ ] Target para tests (test)
  - [ ] Target para empaquetar JAR (jar)
  - [ ] Incluir todas las dependencias
- **Dependencias**: Ninguna
- **Complejidad**: 2
- **Prioridad**: required

### TASK-052: Crear archivo de Manifest
- **Descripción**: Crear MANIFEST.MF para JAR ejecutable
- **Criterios de Aceptación**:
  - [ ] Main-Class apuntando a clase launcher
  - [ ] Class-Path con todas las dependencias
  - [ ] Specification-Version
  - [ ] Implementation-Version
- **Dependencias**: TASK-051
- **Complejidad**: 1
- **Prioridad**: required

### TASK-053: Crear Launcher
- **Descripción**: Crear clase Launcher para iniciar aplicación
- **Criterios de Aceptación**:
  - [ ] Clase `Launcher` con main() method
  - [ ] Cargar configuración inicial
  - [ ] Inicializar MainWindow
  - [ ] Manejo de argumentos CLI (opcional)
- **Dependencies**: TASK-027
- **Complejidad**: 1
- **Prioridad**: required

### TASK-054: Empaquetar JAR ejecutable
- **Descripción**: Compilar y empaquetar aplicación en JAR
- **Criterios de Aceptación**:
  - [ ] Ejecutar: java -jar validador-aces.jar
  - [ ] Incluir todas las clases compiladas
  - [ ] Incluir todas las dependencias en lib/
  - [ ] JAR debe ser ejecutable desde línea de comandos
- **Dependencias**: TASK-051, TASK-052, TASK-053
- **Complejidad**: 1
- **Prioridad**: required

---

## NOTAS GENERALES

- **Orden de ejecución recomendado**: Seguir el orden 1→54 respetando dependencias
- **Tareas opcionales (*)**: Pueden saltarse para MVP, pero recomendadas para versión 1.0
- **Testing**: Tasks 35-47 pueden ejecutarse en paralelo con componentes correspondientes
- **Documentación**: Tasks 48-50 pueden hacerse al final
- **Build**: Tasks 51-54 sólo después de que todo compila sin errores

## DEPENDENCIAS CRÍTICAS

```
TASK-001 → TASK-002 → TASK-003
   ↓         ↓         ↓
TASK-004  TASK-005  TASK-006 → TASK-007
   ↓         ↓         ↓
TASK-008  TASK-013  TASK-022
   ↓         ↓         ↓
TASK-009  TASK-017  TASK-023
   ↓         ↓         
TASK-010  TASK-019 → TASK-020
   ↓         ↓
TASK-011  TASK-027 → TASK-028
         ↓         ↓
       TASK-029 → TASK-030
```

---

**Generado**: 2024
**Versión**: 1.0
**Estado**: Listo para implementación
