# Validador de Atributos ACES - Plan de Implementacion

## Overview

Plan de implementacion para el Validador de Atributos ACES. Sistema Java de validacion y comparacion de atributos de productos contra catalogos maestros, con interfaz grafica y generacion de reportes en Excel.

---

## COMPONENTE 1: MODELOS DE DATOS

### ✅ TASK-001: Crear modelo de datos `Catalog`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que representa el catalogo maestro de atributos ACES
- **Criterios de Aceptacion**:
  - [x] Clase `Catalog` con propiedades: id, name, version, description, createdDate, productLines
  - [x] Constructor y getters/setters
  - [x] Metodo para agregar ProductLine
  - [x] Validacion de datos basica (no null values criticos)
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### ✅ TASK-002: Crear modelo de datos `ProductLine`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que representa una linea de producto en el catalogo
- **Criterios de Aceptacion**:
  - [x] Clase `ProductLine` con propiedades: id, name, category, subCategory, description, attributes
  - [x] Constructor y getters/setters
  - [x] Metodo para agregar Attribute
  - [x] Metodo para buscar atributo por nombre
- **Dependencias**: TASK-001
- **Complejidad**: 1
- **Prioridad**: required

### ✅ TASK-003: Crear modelo de datos `Attribute`
- **Estado**: COMPLETADO (ajustado a estructura real del catalogo Excel)
- **Descripcion**: Implementar clase que representa un atributo individual ACES
- **Criterios de Aceptacion**:
  - [x] Clase `Attribute` con propiedades: id, name, requirement (AttributeRequirement), description
  - [x] Constructor y getters/setters
  - [x] Enum AttributeRequirement (REQUIRED, OPTIONAL, NOT_REQUIRED) mapeado a valores reales de celda
  - [x] Metodo isSatisfiedBy(value) para validar presencia segun requirement
- **Dependencias**: TASK-002
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-004: Crear modelo de datos `Application`
- **Estado**: COMPLETADO (extendido con metadata real del ACES)
- **Descripcion**: Implementar clase que representa una aplicacion a validar
- **Criterios de Aceptacion**:
  - [x] Clase `Application` con propiedades: make, model, year, product, partNumber, mfrLabel, position, data (Map<String, Object>)
  - [x] Constructor y getters/setters
  - [x] Metodo para obtener valor de atributo
  - [x] Metodo para establecer valor de atributo
  - [x] Metodo getProductName() como clave de vinculo con el catalogo
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### ✅ TASK-005: Crear modelo de datos `ValidationError`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que representa un error de validacion
- **Criterios de Aceptacion**:
  - [x] Clase `ValidationError` con propiedades: severity, code, message, attributeName, productLine
  - [x] Constructor y getters/setters
  - [x] Enums para severity (ERROR, WARNING, INFO)
  - [x] Metodo toString() descriptivo
- **Dependencias**: Ninguna
- **Complejidad**: 1
- **Prioridad**: required

### ✅ TASK-006: Crear modelo de datos `ComparisonResult`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que agrupa resultados de comparacion
- **Criterios de Aceptacion**:
  - [x] Clase `ComparisonResult` con propiedades: applicationName, catalogName, totalAttributes, validAttributes, invalidAttributes, errors, warnings, comparisonDate
  - [x] Constructor y getters/setters
  - [x] Metodos de estadistica: getCompliancePercentage(), getErrorCount(), getWarningCount()
  - [x] Metodo para agregar error/warning
- **Dependencias**: TASK-005
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-007: Crear modelo de datos `AuditReport`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que encapsula informacion de auditoria
- **Criterios de Aceptacion**:
  - [x] Clase `AuditReport` con propiedades: timestamp, user, action, details, result
  - [x] Constructor y getters/setters
  - [x] Enums para acciones (VALIDATION, COMPARISON, EXPORT, IMPORT)
  - [x] Metodo para serializar a JSON
- **Dependencias**: TASK-006
- **Complejidad**: 1
- **Prioridad**: optional

---

## COMPONENTE 2: PARSERS

### ✅ TASK-008: Crear `CatalogParser` - Estructura base
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase base para parsers del catalogo ACES
- **Criterios de Aceptacion**:
  - [x] Clase abstracta `CatalogParser` con metodo `parse(File): Catalog`
  - [x] Estructura de manejo de excepciones (ParseException checked)
  - [x] Interfaz para implementaciones especificas (Excel, XML, JSON) via metodo abstracto parse() + parse(String) de conveniencia + validateFileExists() protegido
- **Dependencias**: TASK-001, TASK-002, TASK-003
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-009: Implementar `ExcelCatalogParser`
- **Estado**: COMPLETADO
- **Descripcion**: Parser para leer catalogo desde archivos Excel (.xlsx)
- **Criterios de Aceptacion**:
  - [x] Clase `ExcelCatalogParser` que extiende `CatalogParser`
  - [x] Leer hoja unica "Product Line ACES Attributes" en streaming (openStream): filas = productos, columnas = atributos
  - [x] Mapeo de columnas 0-3 (Product ID, Product Line, Category, Sub Category) a ProductLine
  - [x] Mapeo de columnas 4+ (valores Not Required/Optional/Required) a Attribute via AttributeRequirement
  - [x] Manejo de excepciones de archivo corrupto (IOException y RuntimeException envueltas en ParseException)
  - [x] Verificado contra archivo real: 38,265 ProductLine, 42 atributos c/u, ~1.5s
- **Dependencias**: TASK-008
- **Complejidad**: 3
- **Prioridad**: required

### ✅ TASK-010: Crear `ApplicationParser` - Estructura base
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase base para parsers de aplicaciones ACES
- **Criterios de Aceptacion**:
  - [x] Clase abstracta `ApplicationParser` con metodo `parse(File): List<Application>`
  - [x] Estructura de manejo de excepciones (ParseException checked, validateFileExists)
  - [x] Interfaz para implementaciones especificas (Excel, XML, JSON) via metodo abstracto parse() + parse(String) de conveniencia
- **Dependencias**: TASK-004
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-011: Implementar `ExcelApplicationParser`
- **Estado**: COMPLETADO
- **Descripcion**: Parser para leer datos de aplicacion desde archivos Excel
- **Criterios de Aceptacion**:
  - [x] Clase `ExcelApplicationParser` que extiende `ApplicationParser`
  - [x] Leer hoja unica "Applications" en streaming (openStream): filas = vehiculos, columnas 0-6 = metadata, 7+ = atributos
  - [x] Mapeo de columnas 0-6 (Make, Model, Year, Product, PartNumber, MfrLabel, Position) a campos de Application
  - [x] Mapeo de columnas 7+ al Map de atributos tecnicos (valores vacios preservados como null, relevante para deteccion de faltantes)
  - [x] Manejo de celdas vacias y excepciones de archivo corrupto (IOException y RuntimeException envueltas en ParseException)
  - [x] Verificado contra archivo real: 81,886 Applications parseadas, ~0.9s
- **Dependencias**: TASK-010
- **Complejidad**: 3
- **Prioridad**: required

### ✅ TASK-012: Crear `ValidationSchema`
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase que encapsula esquema de validacion por producto
- **Criterios de Aceptacion**:
  - [x] Clase `ValidationSchema` con propiedades: productLine, requiredAttributes, optionalAttributes (en com.validador.aces.validation)
  - [x] Metodo para compilar reglas desde ProductLine.attributes (filtrando por isRequired/isOptional), compilado una sola vez en el constructor
  - [x] Metodo findMissingRequiredAttributes(Application) e isSatisfiedBy(Application) para validar atributos requeridos
  - [x] Verificado end-to-end: deteccion de atributo requerido faltante confirmada forzando un valor vacio en EngineLiters de una Application real
- **Dependencias**: TASK-003
- **Complejidad**: 2
- **Prioridad**: required

---

## COMPONENTE 3: VALIDACION

### ✅ TASK-013: Implementar `Validator` - Estructura base
- **Estado**: COMPLETADO
- **Descripcion**: Implementar clase base de validador
- **Criterios de Aceptacion**:
  - [x] Clase abstracta `Validator` con metodo `validate(Application, Catalog): List<ValidationError>`
  - [x] Estructura para multiples estrategias de validacion
- **Dependencias**: TASK-004, TASK-001, TASK-005
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-014: Implementar `AttributeValidator`
- **Estado**: COMPLETADO
- **Descripcion**: Validar que todos los atributos requeridos esten presentes
- **Criterios de Aceptacion**:
  - [x] Clase `AttributeValidator` que extiende `Validator`
  - [x] Validar presencia de atributos con AttributeRequirement.REQUIRED (via ValidationSchema)
  - [x] Usar Attribute.isSatisfiedBy(value) para la comparacion (delegado a ValidationSchema.findMissingRequiredAttributes)
  - [x] Generar `ValidationError` para cada atributo requerido faltante (MISSING_REQUIRED_ATTRIBUTE) y WARNING PRODUCT_LINE_NOT_FOUND cuando el producto no existe en el catalogo
  - [x] Verificado contra archivo real: 0 errores en Application "Keep on Green" valida; WARNING confirmado con producto inexistente
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 3
- **Prioridad**: required

### ✅ TASK-015: Implementar `RangeValidator`
- **Estado**: COMPLETADO
- **Descripcion**: Validar valores numericos dentro de rangos permitidos
- **Criterios de Aceptacion**:
  - [x] Clase `RangeValidator` que extiende `Validator`
  - [x] Validar rangos para valores numericos cuando aplique (utilidad generica configurable por constructor: attributeName, min, max; valores no numericos o ausentes no generan error)
  - [x] Generar errores descriptivos (VALUE_OUT_OF_RANGE)
  - [x] Verificado end-to-end sobre atributo real "EngineCylinder" con rango deliberadamente excluyente
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: optional

### ✅ TASK-016: Implementar `EnumValidator`
- **Estado**: COMPLETADO
- **Descripcion**: Validar que valores pertenezcan a lista permitida
- **Criterios de Aceptacion**:
  - [x] Clase `EnumValidator` que extiende `Validator`
  - [x] Validar contra lista de valores permitidos (utilidad generica configurable por constructor: attributeName, allowedValues)
  - [x] Soporte case-insensitive (configurable)
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: optional

### ✅ TASK-017: Implementar `CompositeValidator`
- **Estado**: COMPLETADO
- **Descripcion**: Ejecutar multiples validadores en cadena
- **Criterios de Aceptacion**:
  - [x] Clase `CompositeValidator` que extiende `Validator`
  - [x] Agregar multiples validadores (addValidator)
  - [x] Ejecutar todos y recopilar errores
  - [x] Parar en primer error critico (configurable via setStopOnFirstCriticalError)
  - [x] Verificado con AttributeValidator + RangeValidator combinados, y con stopOnFirstCriticalError=true confirmando que el segundo validador no se ejecuta tras un ERROR
- **Dependencias**: TASK-013, TASK-014, TASK-015, TASK-016
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-018: Implementar `DateFormatValidator`
- **Estado**: COMPLETADO
- **Descripcion**: Validar formato de fechas si aplica a algun atributo
- **Criterios de Aceptacion**:
  - [x] Clase `DateFormatValidator` que extiende `Validator`
  - [x] Validar formato ISO 8601 (YYYY-MM-DD) por defecto
  - [x] Soporte para multiples formatos (configurable, utilidad generica por constructor)
- **Dependencias**: TASK-013, TASK-012
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 4: COMPARACION

### ✅ TASK-019: Implementar `Comparator`
- **Estado**: COMPLETADO
- **Descripcion**: Comparar datos de aplicacion contra catalogo maestro
- **Criterios de Aceptacion**:
  - [x] Clase `Comparator` con metodo `compare(Application, Catalog): ComparisonResult`
  - [x] Buscar ProductLine via Catalog.findProductByName(application.getProductName())
  - [x] Ejecutar validadores apropiados
  - [x] Calcular estadisticas de compliance
  - [x] Capturar informacion de auditoria
  - [x] Metodo `compareAll(List<Application>, Catalog): List<ComparisonResult>` con indice interno de ProductLine (evita busqueda lineal repetida); verificado contra archivo real: 20 comparaciones individuales con `compare()` + lote completo de 81,886 aplicaciones con `compareAll()` en ~9.5s (74,515 con 100% compliance, 0 sin clasificar, 0 con atributos faltantes en esta corrida)
- **Dependencias**: TASK-017, TASK-006
- **Complejidad**: 3
- **Prioridad**: required

### ✅ TASK-020: Implementar `ComplianceCalculator`
- **Estado**: COMPLETADO
- **Descripcion**: Calcular porcentaje de compliance detallado
- **Criterios de Aceptacion**:
  - [x] Clase `ComplianceCalculator` con metodo `calculate(ComparisonResult): ComplianceMetrics`
  - [x] Calcular compliance global (porcentaje valido)
  - [x] Calcular compliance por ProductLine
  - [x] Identificar atributos requeridos incumplidos
  - [x] Verificado con smoke test: `calculate()` sobre resultado individual y `calculateByProductLine()` agrupando manualmente 3 resultados de "A/C Condenser", retornando metricas agregadas correctas
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: required

### ✅ TASK-021: Implementar `ComparisonCache`
- **Estado**: COMPLETADO
- **Descripcion**: Cache para resultados de comparacion previos
- **Criterios de Aceptacion**:
  - [x] Clase `ComparisonCache` con almacenamiento en memoria
  - [x] Metodo para guardar resultado con clave (aplicacion + catalogo + timestamp)
  - [x] Metodo para recuperar resultado
  - [x] Limite de tamano configurable
  - [x] Verificado con smoke test: put/get/contains sobre 3 entradas (size=3), y `evictOlderThan(Duration.ZERO)` elimino todas las entradas (size=0)
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 5: REPORTE

### TASK-022: Crear `ReportGenerator` - Estructura base
- **Descripcion**: Clase base para generadores de reporte
- **Criterios de Aceptacion**:
  - [ ] Clase abstracta `ReportGenerator` con metodo `generate(ComparisonResult): Report`
  - [ ] Estructura para multiples formatos (Excel, PDF, HTML)
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: required

### TASK-023: Implementar `ExcelReportGenerator`
- **Descripcion**: Generar reporte de validacion en nueva hoja del archivo ACES
- **Criterios de Aceptacion**:
  - [ ] Clase `ExcelReportGenerator` que extiende `ReportGenerator`
  - [ ] Seccion resumen ejecutivo (compliance, estadisticas generales)
  - [ ] Seccion errores (listado con Aplicacion, Producto, Atributo faltante)
  - [ ] Seccion estadisticas por ProductLine
  - [ ] Formateo visual (colores, headers, borders)
  - [ ] No modificar hojas originales del archivo ACES
- **Dependencias**: TASK-022
- **Complejidad**: 4
- **Prioridad**: required

### TASK-024: Implementar `ReportPrinter`
- **Descripcion**: Generar representacion textual de reporte para consola
- **Criterios de Aceptacion**:
  - [ ] Clase `ReportPrinter` con metodo `print(ComparisonResult): String`
  - [ ] Resumen ejecutivo con compliance porcentual
  - [ ] Listado de errores con formateo
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-025: Implementar `ExcelExporter`
- **Descripcion**: Exportar resultados a archivo Excel independiente
- **Criterios de Aceptacion**:
  - [ ] Clase `ExcelExporter` con metodo `export(Application, ComparisonResult, File): void`
  - [ ] Incluir metadata (fecha, version catalogo, compliance)
  - [ ] Manejo de sobrescritura de archivo
- **Dependencias**: TASK-006
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-026: Implementar `ReportFilter`
- **Descripcion**: Filtrar resultados de comparacion segun criterios
- **Criterios de Aceptacion**:
  - [ ] Clase `ReportFilter` con metodos para filtrar por: severity, productLine, attributeName
  - [ ] Metodo para aplicar multiples filtros
  - [ ] Retornar nueva instancia de `ComparisonResult` filtrada
- **Dependencias**: TASK-006
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 6: INTERFAZ GRAFICA (GUI)

### TASK-027: Implementar `MainWindow` - Marco principal
- **Descripcion**: Ventana principal de la aplicacion
- **Criterios de Aceptacion**:
  - [ ] JFrame con layout principal
  - [ ] Botones: Cargar Catalogo, Cargar ACES, Ejecutar Auditoria, Salir
  - [ ] Panel de estado con informacion de estado
  - [ ] Tamano inicial 1024x768, resizable
- **Dependencias**: Ninguna
- **Complejidad**: 2
- **Prioridad**: required

### TASK-028: Implementar `CatalogLoadPanel`
- **Descripcion**: Panel para cargar catalogo
- **Criterios de Aceptacion**:
  - [ ] Boton "Browse" para seleccionar archivo Excel
  - [ ] Campo de texto mostrando ruta del archivo
  - [ ] Boton "Load" para cargar catalogo
  - [ ] Mostrar informacion del catalogo cargado (cantidad de productos)
  - [ ] Manejo de errores con dialogos
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: required

### TASK-029: Implementar `ApplicationLoadPanel`
- **Descripcion**: Panel para cargar archivo ACES a validar
- **Criterios de Aceptacion**:
  - [ ] Boton "Browse" para seleccionar archivo Excel
  - [ ] Campo de texto mostrando ruta del archivo
  - [ ] Boton "Load" para cargar aplicaciones
  - [ ] Mostrar informacion de aplicaciones cargadas (cantidad)
  - [ ] Manejo de errores con dialogos
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: required

### TASK-030: Implementar `ValidationPanel`
- **Descripcion**: Panel para ejecutar validacion y mostrar resultados
- **Criterios de Aceptacion**:
  - [ ] Boton "Validate" para iniciar validacion
  - [ ] Tabla con resultados (Aplicacion, Producto, Atributo, Estado)
  - [ ] Resumen de compliance (porcentaje)
  - [ ] Boton para exportar reporte
- **Dependencias**: TASK-028, TASK-029
- **Complejidad**: 3
- **Prioridad**: required

### TASK-031: Implementar `StatisticsPanel`
- **Descripcion**: Panel mostrando estadisticas de validacion
- **Criterios de Aceptacion**:
  - [ ] Tabla con estadisticas detalladas
  - [ ] Actualizar automaticamente tras validacion
- **Dependencias**: TASK-030
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-032: Implementar dialogos de Archivo
- **Descripcion**: Dialogos de seleccion de archivos
- **Criterios de Aceptacion**:
  - [ ] JFileChooser con filtro .xlsx
  - [ ] Recordar ultimo directorio usado
- **Dependencias**: TASK-027
- **Complejidad**: 1
- **Prioridad**: required

### TASK-033: Implementar dialogos de Configuracion
- **Descripcion**: Dialogos para configurar opciones de validacion
- **Criterios de Aceptacion**:
  - [ ] Dialog para opciones basicas
  - [ ] Guardar configuracion en archivo propiedades
- **Dependencias**: TASK-027
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-034: Implementar `AuditPanel`
- **Descripcion**: Panel mostrando historial de auditoria
- **Criterios de Aceptacion**:
  - [ ] Tabla con historial: timestamp, accion, resultado
  - [ ] Boton para exportar historial
- **Dependencias**: TASK-007
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 7: TESTING - PARSERS

### TASK-035: Tests para `CatalogParser`
- **Descripcion**: Unit tests para parser de catalogo
- **Criterios de Aceptacion**:
  - [ ] Test: parsear archivo Excel valido
  - [ ] Test: lanzar excepcion con archivo invalido
  - [ ] Test: validar estructura de datos despues de parsear
- **Dependencias**: TASK-009
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-036: Tests para `ApplicationParser`
- **Descripcion**: Unit tests para parser de aplicacion
- **Criterios de Aceptacion**:
  - [ ] Test: parsear archivo Excel valido
  - [ ] Test: lanzar excepcion con archivo invalido
  - [ ] Test: manejar celdas vacias
- **Dependencias**: TASK-011
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-037: Tests Property-Based: Round-trip Catalog
- **Descripcion**: Test que verifica que parsear y re-serializar catalogo es identico
- **Criterios de Aceptacion**:
  - [ ] Generar catalogos aleatorios
  - [ ] Serializar a Excel
  - [ ] Parsear de nuevo
  - [ ] Verificar equivalencia de estructura
- **Dependencias**: TASK-009
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-038: Tests Property-Based: Round-trip Application
- **Descripcion**: Test que verifica que parsear y re-serializar aplicacion es identico
- **Criterios de Aceptacion**:
  - [ ] Generar aplicaciones aleatorias
  - [ ] Serializar a Excel
  - [ ] Parsear de nuevo
  - [ ] Verificar equivalencia de estructura
- **Dependencias**: TASK-011
- **Complejidad**: 3
- **Prioridad**: optional

---

## COMPONENTE 8: TESTING - VALIDADORES

### TASK-039: Tests para `AttributeValidator`
- **Descripcion**: Unit tests para validador de atributos
- **Criterios de Aceptacion**:
  - [ ] Test: atributo requerido presente pasa validacion
  - [ ] Test: atributo requerido faltante genera error
  - [ ] Test: atributo opcional faltante no genera error
- **Dependencias**: TASK-014
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-040: Tests para `RangeValidator`
- **Descripcion**: Unit tests para validador de rangos
- **Criterios de Aceptacion**:
  - [ ] Test: valor dentro de rango pasa
  - [ ] Test: valor fuera de rango genera error
- **Dependencias**: TASK-015
- **Complejidad**: 1
- **Prioridad**: optional

### TASK-041: Tests para `EnumValidator`
- **Descripcion**: Unit tests para validador de enumeraciones
- **Criterios de Aceptacion**:
  - [ ] Test: valor en lista permitida pasa
  - [ ] Test: valor no en lista genera error
- **Dependencias**: TASK-016
- **Complejidad**: 1
- **Prioridad**: optional

### TASK-042: Tests para `CompositeValidator`
- **Descripcion**: Unit tests para validador compuesto
- **Criterios de Aceptacion**:
  - [ ] Test: ejecutar multiples validadores
  - [ ] Test: recopilar todos los errores
- **Dependencias**: TASK-017
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 9: TESTING - COMPARACION E INTEGRACION

### TASK-043: Tests para `Comparator`
- **Descripcion**: Unit tests para comparador
- **Criterios de Aceptacion**:
  - [ ] Test: comparar aplicacion valida contra catalogo
  - [ ] Test: detectar atributos faltantes
  - [ ] Test: calcular compliance percentage
- **Dependencias**: TASK-019
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-044: Tests Integration: End-to-end
- **Descripcion**: Test de integracion que valida flujo completo
- **Criterios de Aceptacion**:
  - [ ] Cargar catalogo desde archivo real
  - [ ] Cargar ACES desde archivo real
  - [ ] Ejecutar validacion completa
  - [ ] Generar reporte
- **Dependencias**: TASK-009, TASK-011, TASK-019, TASK-023
- **Complejidad**: 3
- **Prioridad**: optional

### TASK-045: Tests Property-Based: Validation Idempotence
- **Descripcion**: Verificar que validar dos veces produce mismo resultado
- **Criterios de Aceptacion**:
  - [ ] Validar misma aplicacion dos veces
  - [ ] Verificar que resultados son identicos
- **Dependencias**: TASK-019
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 10: TESTING - REPORTES

### TASK-046: Tests para `ExcelReportGenerator`
- **Descripcion**: Unit tests para generador de reportes Excel
- **Criterios de Aceptacion**:
  - [ ] Test: generar reporte sin errores
  - [ ] Test: generar reporte con errores
  - [ ] Test: verificar que hojas originales no se modifican
- **Dependencias**: TASK-023
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-047: Tests Property-Based: Report Completeness
- **Descripcion**: Verificar que reporte contiene todos los errores
- **Criterios de Aceptacion**:
  - [ ] Generar ComparisonResult aleatorio
  - [ ] Verificar que todos los errores aparecen en reporte
- **Dependencias**: TASK-023
- **Complejidad**: 2
- **Prioridad**: optional

---

## COMPONENTE 11: DOCUMENTACION

### TASK-048: Documentacion de API
- **Descripcion**: Generar documentacion JavaDoc de todas las clases publicas
- **Criterios de Aceptacion**:
  - [ ] JavaDoc comments para todas las clases
  - [ ] Generar HTML con javadoc tool
- **Dependencias**: Todas las implementaciones
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-049: Guia de Usuario
- **Descripcion**: Crear guia de usuario para GUI
- **Criterios de Aceptacion**:
  - [ ] Instrucciones paso a paso
  - [ ] Solucion de problemas comunes
- **Dependencias**: TASK-027 al TASK-034
- **Complejidad**: 2
- **Prioridad**: optional

### TASK-050: Guia de Configuracion
- **Descripcion**: Crear documentacion de configuracion
- **Criterios de Aceptacion**:
  - [ ] Parametros configurables documentados
  - [ ] Ejemplos de configuracion
- **Dependencias**: TASK-033
- **Complejidad**: 1
- **Prioridad**: optional

---

## COMPONENTE 12: EMPAQUETADO

### TASK-051: Configurar build.xml (Ant)
- **Descripcion**: Configurar script de compilacion con Apache Ant
- **Criterios de Aceptacion**:
  - [ ] Target para compilar (compile)
  - [ ] Target para limpiar (clean)
  - [ ] Target para empaquetar JAR (jar)
- **Dependencias**: Ninguna
- **Complejidad**: 2
- **Prioridad**: required

### TASK-052: Crear archivo de Manifest
- **Descripcion**: Crear MANIFEST.MF para JAR ejecutable
- **Criterios de Aceptacion**:
  - [ ] Main-Class apuntando a clase launcher
  - [ ] Class-Path con todas las dependencias
- **Dependencias**: TASK-051
- **Complejidad**: 1
- **Prioridad**: required

### TASK-053: Crear Launcher
- **Descripcion**: Crear clase Launcher para iniciar aplicacion
- **Criterios de Aceptacion**:
  - [ ] Clase `Launcher` con main() method
  - [ ] Inicializar MainWindow
- **Dependencias**: TASK-027
- **Complejidad**: 1
- **Prioridad**: required

### TASK-054: Empaquetar JAR ejecutable
- **Descripcion**: Compilar y empaquetar aplicacion en JAR
- **Criterios de Aceptacion**:
  - [ ] Ejecutar: java -jar validador-aces.jar
  - [ ] Incluir todas las dependencias en lib/
- **Dependencias**: TASK-051, TASK-052, TASK-053
- **Complejidad**: 1
- **Prioridad**: required

---

## NOTAS GENERALES

- **Orden de ejecucion recomendado**: Seguir el orden 1 a 54 respetando dependencias
- **Ajuste post Componente 1**: Los modelos fueron ajustados a la estructura real de los archivos Excel (ver XLSX_STRUCTURE_ANALYSIS.md)
- **Testing**: Tasks 35-47 pueden ejecutarse en paralelo con componentes correspondientes
- **Build**: Tasks 51-54 solo despues de que todo compila sin errores

---

**Version**: 1.0
**Estado**: En Progreso (21/54 tareas completadas)
