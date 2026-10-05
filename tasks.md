# Implementation Plan: Validador de Atributos ACES

## Overview

Plan de implementacion para el Validador de Atributos ACES. Sistema Java de validacion y comparacion de atributos de productos contra catalogos maestros, con interfaz grafica y generacion de reportes en Excel.


## Tasks

### Componente 1: MODELOS DE DATOS

- [x] 1. Crear modelo de datos `Catalog` (TASK-001)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que representa el catalogo maestro de atributos ACES
  - Criterios de Aceptacion:
    - ✅ Clase `Catalog` con propiedades: id, name, version, description, createdDate, productLines
    - ✅ Constructor y getters/setters
    - ✅ Metodo para agregar ProductLine
    - ✅ Validacion de datos basica (no null values criticos)
  - Dependencias: Ninguna
  - Complejidad: 1
  - Prioridad: required

- [x] 2. Crear modelo de datos `ProductLine` (TASK-002)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que representa una linea de producto en el catalogo
  - Criterios de Aceptacion:
    - ✅ Clase `ProductLine` con propiedades: id, name, category, subCategory, description, attributes
    - ✅ Constructor y getters/setters
    - ✅ Metodo para agregar Attribute
    - ✅ Metodo para buscar atributo por nombre
  - Dependencias: TASK-001
  - Complejidad: 1
  - Prioridad: required

- [x] 3. Crear modelo de datos `Attribute` (TASK-003)
  - Estado: COMPLETADO (ajustado a estructura real del catalogo Excel)
  - Descripcion: Implementar clase que representa un atributo individual ACES
  - Criterios de Aceptacion:
    - ✅ Clase `Attribute` con propiedades: id, name, requirement (AttributeRequirement), description
    - ✅ Constructor y getters/setters
    - ✅ Enum AttributeRequirement (REQUIRED, OPTIONAL, NOT_REQUIRED) mapeado a valores reales de celda
    - ✅ Metodo isSatisfiedBy(value) para validar presencia segun requirement
  - Dependencias: TASK-002
  - Complejidad: 2
  - Prioridad: required

- [x] 4. Crear modelo de datos `Application` (TASK-004)
  - Estado: COMPLETADO (extendido con metadata real del ACES)
  - Descripcion: Implementar clase que representa una aplicacion a validar
  - Criterios de Aceptacion:
    - ✅ Clase `Application` con propiedades: make, model, year, product, partNumber, mfrLabel, position, data (Map<String, Object>)
    - ✅ Constructor y getters/setters
    - ✅ Metodo para obtener valor de atributo
    - ✅ Metodo para establecer valor de atributo
    - ✅ Metodo getProductName() como clave de vinculo con el catalogo
  - Dependencias: Ninguna
  - Complejidad: 1
  - Prioridad: required

- [x] 5. Crear modelo de datos `ValidationError` (TASK-005)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que representa un error de validacion
  - Criterios de Aceptacion:
    - ✅ Clase `ValidationError` con propiedades: severity, code, message, attributeName, productLine
    - ✅ Constructor y getters/setters
    - ✅ Enums para severity (ERROR, WARNING, INFO)
    - ✅ Metodo toString() descriptivo
  - Dependencias: Ninguna
  - Complejidad: 1
  - Prioridad: required

- [x] 6. Crear modelo de datos `ComparisonResult` (TASK-006)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que agrupa resultados de comparacion
  - Criterios de Aceptacion:
    - ✅ Clase `ComparisonResult` con propiedades: applicationName, catalogName, totalAttributes, validAttributes, invalidAttributes, errors, warnings, comparisonDate
    - ✅ Constructor y getters/setters
    - ✅ Metodos de estadistica: getCompliancePercentage(), getErrorCount(), getWarningCount()
    - ✅ Metodo para agregar error/warning
  - Dependencias: TASK-005
  - Complejidad: 2
  - Prioridad: required

- [x] 7. Crear modelo de datos `AuditReport` (TASK-007)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que encapsula informacion de auditoria
  - Criterios de Aceptacion:
    - ✅ Clase `AuditReport` con propiedades: timestamp, user, action, details, result
    - ✅ Constructor y getters/setters
    - ✅ Enums para acciones (VALIDATION, COMPARISON, EXPORT, IMPORT)
    - ✅ Metodo para serializar a JSON
  - Dependencias: TASK-006
  - Complejidad: 1
  - Prioridad: optional

---

### Componente 2: PARSERS

- [x] 8. Crear `CatalogParser` - Estructura base (TASK-008)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase base para parsers del catalogo ACES
  - Criterios de Aceptacion:
    - ✅ Clase abstracta `CatalogParser` con metodo `parse(File): Catalog`
    - ✅ Estructura de manejo de excepciones (ParseException checked)
    - ✅ Interfaz para implementaciones especificas (Excel, XML, JSON) via metodo abstracto parse() + parse(String) de conveniencia + validateFileExists() protegido
  - Dependencias: TASK-001, TASK-002, TASK-003
  - Complejidad: 2
  - Prioridad: required

- [x] 9. Implementar `ExcelCatalogParser` (TASK-009)
  - Estado: COMPLETADO
  - Descripcion: Parser para leer catalogo desde archivos Excel (.xlsx)
  - Criterios de Aceptacion:
    - ✅ Clase `ExcelCatalogParser` que extiende `CatalogParser`
    - ✅ Leer hoja unica "Product Line ACES Attributes" en streaming (openStream): filas = productos, columnas = atributos
    - ✅ Mapeo de columnas 0-3 (Product ID, Product Line, Category, Sub Category) a ProductLine
    - ✅ Mapeo de columnas 4+ (valores Not Required/Optional/Required) a Attribute via AttributeRequirement
    - ✅ Manejo de excepciones de archivo corrupto (IOException y RuntimeException envueltas en ParseException)
    - ✅ Verificado contra archivo real: 38,265 ProductLine, 42 atributos c/u, ~1.5s
  - Dependencias: TASK-008
  - Complejidad: 3
  - Prioridad: required

- [x] 10. Crear `ApplicationParser` - Estructura base (TASK-010)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase base para parsers de aplicaciones ACES
  - Criterios de Aceptacion:
    - ✅ Clase abstracta `ApplicationParser` con metodo `parse(File): List<Application>`
    - ✅ Estructura de manejo de excepciones (ParseException checked, validateFileExists)
    - ✅ Interfaz para implementaciones especificas (Excel, XML, JSON) via metodo abstracto parse() + parse(String) de conveniencia
  - Dependencias: TASK-004
  - Complejidad: 2
  - Prioridad: required

- [x] 11. Implementar `ExcelApplicationParser` (TASK-011)
  - Estado: COMPLETADO
  - Descripcion: Parser para leer datos de aplicacion desde archivos Excel
  - Criterios de Aceptacion:
    - ✅ Clase `ExcelApplicationParser` que extiende `ApplicationParser`
    - ✅ Leer hoja unica "Applications" en streaming (openStream): filas = vehiculos, columnas 0-6 = metadata, 7+ = atributos
    - ✅ Mapeo de columnas 0-6 (Make, Model, Year, Product, PartNumber, MfrLabel, Position) a campos de Application
    - ✅ Mapeo de columnas 7+ al Map de atributos tecnicos (valores vacios preservados como null, relevante para deteccion de faltantes)
    - ✅ Manejo de celdas vacias y excepciones de archivo corrupto (IOException y RuntimeException envueltas en ParseException)
    - ✅ Verificado contra archivo real: 81,886 Applications parseadas, ~0.9s
  - Dependencias: TASK-010
  - Complejidad: 3
  - Prioridad: required

- [x] 12. Crear `ValidationSchema` (TASK-012)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase que encapsula esquema de validacion por producto
  - Criterios de Aceptacion:
    - ✅ Clase `ValidationSchema` con propiedades: productLine, requiredAttributes, optionalAttributes (en com.validador.aces.validation)
    - ✅ Metodo para compilar reglas desde ProductLine.attributes (filtrando por isRequired/isOptional), compilado una sola vez en el constructor
    - ✅ Metodo findMissingRequiredAttributes(Application) e isSatisfiedBy(Application) para validar atributos requeridos
    - ✅ Verificado end-to-end: deteccion de atributo requerido faltante confirmada forzando un valor vacio en EngineLiters de una Application real
  - Dependencias: TASK-003
  - Complejidad: 2
  - Prioridad: required

---

### Componente 3: VALIDACION

- [x] 13. Implementar `Validator` - Estructura base (TASK-013)
  - Estado: COMPLETADO
  - Descripcion: Implementar clase base de validador
  - Criterios de Aceptacion:
    - ✅ Clase abstracta `Validator` con metodo `validate(Application, Catalog): List<ValidationError>`
    - ✅ Estructura para multiples estrategias de validacion
  - Dependencias: TASK-004, TASK-001, TASK-005
  - Complejidad: 2
  - Prioridad: required

- [x] 14. Implementar `AttributeValidator` (TASK-014)
  - Estado: COMPLETADO
  - Descripcion: Validar que todos los atributos requeridos esten presentes
  - Criterios de Aceptacion:
    - ✅ Clase `AttributeValidator` que extiende `Validator`
    - ✅ Validar presencia de atributos con AttributeRequirement.REQUIRED (via ValidationSchema)
    - ✅ Usar Attribute.isSatisfiedBy(value) para la comparacion (delegado a ValidationSchema.findMissingRequiredAttributes)
    - ✅ Generar `ValidationError` para cada atributo requerido faltante (MISSING_REQUIRED_ATTRIBUTE) y WARNING PRODUCT_LINE_NOT_FOUND cuando el producto no existe en el catalogo
    - ✅ Verificado contra archivo real: 0 errores en Application "Keep on Green" valida; WARNING confirmado con producto inexistente
  - Dependencias: TASK-013, TASK-012
  - Complejidad: 3
  - Prioridad: required

- [x] 15. Implementar `RangeValidator` (TASK-015)
  - Estado: COMPLETADO
  - Descripcion: Validar valores numericos dentro de rangos permitidos
  - Criterios de Aceptacion:
    - ✅ Clase `RangeValidator` que extiende `Validator`
    - ✅ Validar rangos para valores numericos cuando aplique (utilidad generica configurable por constructor: attributeName, min, max; valores no numericos o ausentes no generan error)
    - ✅ Generar errores descriptivos (VALUE_OUT_OF_RANGE)
    - ✅ Verificado end-to-end sobre atributo real "EngineCylinder" con rango deliberadamente excluyente
  - Dependencias: TASK-013, TASK-012
  - Complejidad: 2
  - Prioridad: optional

- [x] 16. Implementar `EnumValidator` (TASK-016)
  - Estado: COMPLETADO
  - Descripcion: Validar que valores pertenezcan a lista permitida
  - Criterios de Aceptacion:
    - ✅ Clase `EnumValidator` que extiende `Validator`
    - ✅ Validar contra lista de valores permitidos (utilidad generica configurable por constructor: attributeName, allowedValues)
    - ✅ Soporte case-insensitive (configurable)
  - Dependencias: TASK-013, TASK-012
  - Complejidad: 2
  - Prioridad: optional

- [x] 17. Implementar `CompositeValidator` (TASK-017)
  - Estado: COMPLETADO
  - Descripcion: Ejecutar multiples validadores en cadena
  - Criterios de Aceptacion:
    - ✅ Clase `CompositeValidator` que extiende `Validator`
    - ✅ Agregar multiples validadores (addValidator)
    - ✅ Ejecutar todos y recopilar errores
    - ✅ Parar en primer error critico (configurable via setStopOnFirstCriticalError)
    - ✅ Verificado con AttributeValidator + RangeValidator combinados, y con stopOnFirstCriticalError=true confirmando que el segundo validador no se ejecuta tras un ERROR
  - Dependencias: TASK-013, TASK-014, TASK-015, TASK-016
  - Complejidad: 2
  - Prioridad: required

- [x] 18. Implementar `DateFormatValidator` (TASK-018)
  - Estado: COMPLETADO
  - Descripcion: Validar formato de fechas si aplica a algun atributo
  - Criterios de Aceptacion:
    - ✅ Clase `DateFormatValidator` que extiende `Validator`
    - ✅ Validar formato ISO 8601 (YYYY-MM-DD) por defecto
    - ✅ Soporte para multiples formatos (configurable, utilidad generica por constructor)
  - Dependencias: TASK-013, TASK-012
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 4: COMPARACION

- [x] 19. Implementar `Comparator` (TASK-019)
  - Estado: COMPLETADO
  - Descripcion: Comparar datos de aplicacion contra catalogo maestro
  - Criterios de Aceptacion:
    - ✅ Clase `Comparator` con metodo `compare(Application, Catalog): ComparisonResult`
    - ✅ Buscar ProductLine via Catalog.findProductByName(application.getProductName())
    - ✅ Ejecutar validadores apropiados
    - ✅ Calcular estadisticas de compliance
    - ✅ Capturar informacion de auditoria
    - ✅ Metodo `compareAll(List<Application>, Catalog): List<ComparisonResult>` con indice interno de ProductLine (evita busqueda lineal repetida); verificado contra archivo real: 20 comparaciones individuales con `compare()` + lote completo de 81,886 aplicaciones con `compareAll()` en ~9.5s (74,515 con 100% compliance, 0 sin clasificar, 0 con atributos faltantes en esta corrida)
  - Dependencias: TASK-017, TASK-006
  - Complejidad: 3
  - Prioridad: required

- [x] 20. Implementar `ComplianceCalculator` (TASK-020)
  - Estado: COMPLETADO
  - Descripcion: Calcular porcentaje de compliance detallado
  - Criterios de Aceptacion:
    - ✅ Clase `ComplianceCalculator` con metodo `calculate(ComparisonResult): ComplianceMetrics`
    - ✅ Calcular compliance global (porcentaje valido)
    - ✅ Calcular compliance por ProductLine
    - ✅ Identificar atributos requeridos incumplidos
    - ✅ Verificado con smoke test: `calculate()` sobre resultado individual y `calculateByProductLine()` agrupando manualmente 3 resultados de "A/C Condenser", retornando metricas agregadas correctas
  - Dependencias: TASK-006
  - Complejidad: 2
  - Prioridad: required

- [x] 21. Implementar `ComparisonCache` (TASK-021)
  - Estado: COMPLETADO
  - Descripcion: Cache para resultados de comparacion previos
  - Criterios de Aceptacion:
    - ✅ Clase `ComparisonCache` con almacenamiento en memoria
    - ✅ Metodo para guardar resultado con clave (aplicacion + catalogo + timestamp)
    - ✅ Metodo para recuperar resultado
    - ✅ Limite de tamano configurable
    - ✅ Verificado con smoke test: put/get/contains sobre 3 entradas (size=3), y `evictOlderThan(Duration.ZERO)` elimino todas las entradas (size=0)
  - Dependencias: TASK-006
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 5: REPORTE

- [x] 22. Crear `ReportGenerator` - Estructura base (TASK-022)
  - Estado: COMPLETADO
  - Descripcion: Clase base para generadores de reporte
  - Criterios de Aceptacion:
    - ✅ Clase abstracta `ReportGenerator` con metodo `generate(ComparisonResult): Report`
    - ✅ Estructura para multiples formatos (Excel, PDF, HTML) via metodo abstracto getFormatName() + generate(), mas utilidades protegidas buildSummarySection() y buildErrorsSection() reutilizables por cualquier subclase concreta
    - ✅ Clases de soporte creadas: Report (contenedor de secciones con fecha de generacion) y ReportSection (tabla generica con headers y filas, agnostica de formato de salida)
    - ✅ Verificado end-to-end con subclase de prueba contra archivos reales: aplicacion valida (Isuzu Amigo 1998, producto Accessory Drive Belt) mostro 100% compliance y fila "Completo..."; mismo caso con atributo requerido EngineLiters forzado a vacio mostro correctamente el ERROR MISSING_REQUIRED_ATTRIBUTE en la seccion de errores
  - Dependencias: TASK-006
  - Complejidad: 2
  - Prioridad: required

- [x] 23. Implementar `ExcelReportGenerator` (TASK-023)
  - Estado: COMPLETADO
  - Descripcion: Generar reporte de validacion en archivo Excel (hoja "Reporte Auditoria ACES")
  - Criterios de Aceptacion:
    - ✅ Clase `ExcelReportGenerator` que extiende `ReportGenerator` (getFormatName="EXCEL", generate(ComparisonResult) individual + generateBatchReport(Map) por lote)
    - ✅ Seccion resumen ejecutivo (totales agregados: apps auditadas, 100% compliant, con faltantes, sin clasificar, compliance promedio)
    - ✅ Seccion errores: listado "Atributos Faltantes por Aplicacion" con Aplicacion, Producto y Atributo faltante
    - ✅ Seccion estadisticas por ProductLine (delegada a ComplianceCalculator.calculateByProductLine)
    - ✅ Formateo visual con fastexcel: titulos de seccion (relleno TEAL/NAVY, fuente blanca, negrita), encabezados (relleno GRAY3, bordes THIN), filas de error resaltadas (DARK_RED), anchos de columna
    - ✅ No modificar hojas originales del archivo ACES: fastexcel escribe un libro nuevo desde cero; el reporte se escribe en archivo SEPARADO (helper defaultReportFileFor() genera ruta hermana con sufijo _Reporte_Auditoria.xlsx), nunca se abre el ACES en escritura
    - ✅ Verificado end-to-end: muestra de 500 apps reales (1 faltante forzado detectado), reporte escrito y re-leido con fastexcel-reader (hoja presente, 3 secciones, 19 filas); ACES original intacto (7,214,981 bytes sin cambios)
  - Dependencias: TASK-022
  - Complejidad: 4
  - Prioridad: required

- [x] 24. Implementar `ReportPrinter` (TASK-024)
  - Estado: COMPLETADO
  - Descripcion: Generar representacion textual de reporte para consola
  - Criterios de Aceptacion:
    - ✅ Clase `ReportPrinter` con metodo `print(ComparisonResult): String` (cabecera, resumen ejecutivo, tabla ASCII de errores/advertencias)
    - ✅ Resumen ejecutivo con compliance %, atributos totales/satisfechos/incumplidos, conteo de errores y advertencias
    - ✅ Listado de errores con tabla ASCII formateada (Severidad | Atributo | Linea de Producto | Mensaje); fila "Sin problemas detectados" si no hay errores
    - ✅ Metodo adicional printBatch(List<ComparisonResult>) para resumen de lote (apps, compliance promedio, 100% compliant, con errores)
    - ✅ Verificado: texto generado correctamente con 1 error forzado real; printBatch OK (longitud=409)
  - Dependencias: TASK-006
  - Complejidad: 2
  - Prioridad: optional

- [x] 25. Implementar `ExcelExporter` (TASK-025)
  - Estado: COMPLETADO
  - Descripcion: Exportar resultados a archivo Excel independiente por aplicacion
  - Criterios de Aceptacion:
    - ✅ Clase `ExcelExporter` con metodo `export(Application, ComparisonResult, File): void` throws IOException
    - ✅ Incluir metadata: aplicacion, vehiculo (Make/Model/Year), catalogo, fecha de exportacion, compliance %
    - ✅ Manejo de sobrescritura: el archivo se crea o sobrescribe (comportamiento documentado en JavaDoc)
    - ✅ Formato visual fastexcel: titulo NAVY_BLUE, etiquetas de metadata en negrita GRAY2, tabla de problemas con encabezados GRAY3, filas ERROR en DARK_RED
    - ✅ Verificado: archivo exportado 4465 bytes, lectura confirmada con fastexcel-reader
  - Dependencias: TASK-006
  - Complejidad: 3
  - Prioridad: optional

- [x] 26. Implementar `ReportFilter` (TASK-026)
  - Estado: COMPLETADO
  - Descripcion: Filtrar resultados de comparacion segun criterios, devolviendo nueva instancia
  - Criterios de Aceptacion:
    - ✅ Clase `ReportFilter` con metodos filterBySeverity, filterByProductLine, filterByAttributeName (todos retornan nueva instancia de ComparisonResult, no modifican el original)
    - ✅ Metodo applyFilters(ComparisonResult, List<Predicate<ValidationError>>) para filtros combinados (AND logico)
    - ✅ Contadores de atributos (total/valido/invalido) recalculados en cada instancia filtrada segun errores MISSING_REQUIRED_ATTRIBUTE resultantes
    - ✅ Verificado: filterBySeverity(ERROR)=1, filterByProductLine=1, filterByAttributeName=1, applyFilters(AND)=1, filterBySeverity(INFO)=0
  - Dependencias: TASK-006
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 6: INTERFAZ GRAFICA (GUI)

- [x] 27. Implementar `MainWindow` - Marco principal (TASK-027)
  - Estado: COMPLETADO
  - Descripcion: Ventana principal de la aplicacion
  - Criterios de Aceptacion:
    - ✅ setUndecorated(true) + barra de titulo propia (arrastrable, boton cierre con hover RED)
    - ✅ Botones toolbar: Cargar catalogo (SCI_BLUE), Cargar ACES (SCI_BLUE), Ejecutar auditoria (FLUSH_ORANGE, disabled hasta que ambos archivos esten cargados), Salir
    - ✅ Panel de estado en la parte inferior con setStatus(String) y setStatus(String, Color)
    - ✅ Tamano inicial proporcional a la pantalla (84% x 88% del area disponible), minimo px(780)xpx(520), resizable
    - ✅ CardLayout central para intercambiar paneles (registerPanel/showCard)
    - ✅ Factor de escala DPI (S = getScaleX()) aplicado a px() y font() para HiDPI
    - ✅ Gestion de estado: setLoadedCatalog/setLoadedApplications con checkCanRun()
    - ✅ Paleta completa aplicada: MINE_SHAFT, FLUSH_ORANGE, SCI_BLUE, SILVER, RED, CHELSEA_GEM, MALIBU
  - Dependencias: Ninguna
  - Complejidad: 2
  - Prioridad: required

- [x] 28. Implementar `CatalogLoadPanel` (TASK-028)
  - Estado: COMPLETADO
  - Descripcion: Panel para cargar catalogo
  - Criterios de Aceptacion:
    - ✅ Boton "Examinar..." (outline SCI_BLUE) que abre JFileChooser con filtro .xlsx; recuerda el ultimo directorio
    - ✅ Campo de texto readonly mostrando ruta del archivo seleccionado
    - ✅ Boton "Cargar catalogo" (solido FLUSH_ORANGE) deshabilitado hasta seleccionar archivo; carga en SwingWorker (no bloquea EDT)
    - ✅ Card de resultados (SCI_BLUE accent) con: contador de lineas de producto y atributos por linea (formateado con separador de miles), boton "Continuar: Cargar ACES"
    - ✅ Manejo de errores con JOptionPane y actualizacion del status bar en rojo (C_ERROR)
    - ✅ Misma paleta y estilo de card (borde acento izquierdo) que el resto de la app
    - ✅ Auto-registra en MainWindow y conecta el boton del toolbar
  - Dependencias: TASK-027
  - Complejidad: 2
  - Prioridad: required

- [ ] 29. Implementar `ApplicationLoadPanel` (TASK-029)
  - Descripcion: Panel para cargar archivo ACES a validar
  - Criterios de Aceptacion:
    - ⬜ Boton "Browse" para seleccionar archivo Excel
    - ⬜ Campo de texto mostrando ruta del archivo
    - ⬜ Boton "Load" para cargar aplicaciones
    - ⬜ Mostrar informacion de aplicaciones cargadas (cantidad)
    - ⬜ Manejo de errores con dialogos
  - Dependencias: TASK-027
  - Complejidad: 2
  - Prioridad: required

- [ ] 30. Implementar `ValidationPanel` (TASK-030)
  - Descripcion: Panel para ejecutar validacion y mostrar resultados
  - Criterios de Aceptacion:
    - ⬜ Boton "Validate" para iniciar validacion
    - ⬜ Tabla con resultados (Aplicacion, Producto, Atributo, Estado)
    - ⬜ Resumen de compliance (porcentaje)
    - ⬜ Boton para exportar reporte
  - Dependencias: TASK-028, TASK-029
  - Complejidad: 3
  - Prioridad: required

- [ ] 31. Implementar `StatisticsPanel` (TASK-031)
  - Descripcion: Panel mostrando estadisticas de validacion
  - Criterios de Aceptacion:
    - ⬜ Tabla con estadisticas detalladas
    - ⬜ Actualizar automaticamente tras validacion
  - Dependencias: TASK-030
  - Complejidad: 3
  - Prioridad: optional

- [ ] 32. Implementar dialogos de Archivo (TASK-032)
  - Descripcion: Dialogos de seleccion de archivos
  - Criterios de Aceptacion:
    - ⬜ JFileChooser con filtro .xlsx
    - ⬜ Recordar ultimo directorio usado
  - Dependencias: TASK-027
  - Complejidad: 1
  - Prioridad: required

- [ ] 33. Implementar dialogos de Configuracion (TASK-033)
  - Descripcion: Dialogos para configurar opciones de validacion
  - Criterios de Aceptacion:
    - ⬜ Dialog para opciones basicas
    - ⬜ Guardar configuracion en archivo propiedades
  - Dependencias: TASK-027
  - Complejidad: 2
  - Prioridad: optional

- [ ] 34. Implementar `AuditPanel` (TASK-034)
  - Descripcion: Panel mostrando historial de auditoria
  - Criterios de Aceptacion:
    - ⬜ Tabla con historial: timestamp, accion, resultado
    - ⬜ Boton para exportar historial
  - Dependencias: TASK-007
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 7: TESTING - PARSERS

- [ ] 35. Tests para `CatalogParser` (TASK-035)
  - Descripcion: Unit tests para parser de catalogo
  - Criterios de Aceptacion:
    - ⬜ Test: parsear archivo Excel valido
    - ⬜ Test: lanzar excepcion con archivo invalido
    - ⬜ Test: validar estructura de datos despues de parsear
  - Dependencias: TASK-009
  - Complejidad: 2
  - Prioridad: optional

- [ ] 36. Tests para `ApplicationParser` (TASK-036)
  - Descripcion: Unit tests para parser de aplicacion
  - Criterios de Aceptacion:
    - ⬜ Test: parsear archivo Excel valido
    - ⬜ Test: lanzar excepcion con archivo invalido
    - ⬜ Test: manejar celdas vacias
  - Dependencias: TASK-011
  - Complejidad: 2
  - Prioridad: optional

- [ ] 37. Tests Property-Based: Round-trip Catalog (TASK-037)
  - Descripcion: Test que verifica que parsear y re-serializar catalogo es identico
  - Criterios de Aceptacion:
    - ⬜ Generar catalogos aleatorios
    - ⬜ Serializar a Excel
    - ⬜ Parsear de nuevo
    - ⬜ Verificar equivalencia de estructura
  - Dependencias: TASK-009
  - Complejidad: 3
  - Prioridad: optional

- [ ] 38. Tests Property-Based: Round-trip Application (TASK-038)
  - Descripcion: Test que verifica que parsear y re-serializar aplicacion es identico
  - Criterios de Aceptacion:
    - ⬜ Generar aplicaciones aleatorias
    - ⬜ Serializar a Excel
    - ⬜ Parsear de nuevo
    - ⬜ Verificar equivalencia de estructura
  - Dependencias: TASK-011
  - Complejidad: 3
  - Prioridad: optional

---

### Componente 8: TESTING - VALIDADORES

- [ ] 39. Tests para `AttributeValidator` (TASK-039)
  - Descripcion: Unit tests para validador de atributos
  - Criterios de Aceptacion:
    - ⬜ Test: atributo requerido presente pasa validacion
    - ⬜ Test: atributo requerido faltante genera error
    - ⬜ Test: atributo opcional faltante no genera error
  - Dependencias: TASK-014
  - Complejidad: 2
  - Prioridad: optional

- [ ] 40. Tests para `RangeValidator` (TASK-040)
  - Descripcion: Unit tests para validador de rangos
  - Criterios de Aceptacion:
    - ⬜ Test: valor dentro de rango pasa
    - ⬜ Test: valor fuera de rango genera error
  - Dependencias: TASK-015
  - Complejidad: 1
  - Prioridad: optional

- [ ] 41. Tests para `EnumValidator` (TASK-041)
  - Descripcion: Unit tests para validador de enumeraciones
  - Criterios de Aceptacion:
    - ⬜ Test: valor en lista permitida pasa
    - ⬜ Test: valor no en lista genera error
  - Dependencias: TASK-016
  - Complejidad: 1
  - Prioridad: optional

- [ ] 42. Tests para `CompositeValidator` (TASK-042)
  - Descripcion: Unit tests para validador compuesto
  - Criterios de Aceptacion:
    - ⬜ Test: ejecutar multiples validadores
    - ⬜ Test: recopilar todos los errores
  - Dependencias: TASK-017
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 9: TESTING - COMPARACION E INTEGRACION

- [ ] 43. Tests para `Comparator` (TASK-043)
  - Descripcion: Unit tests para comparador
  - Criterios de Aceptacion:
    - ⬜ Test: comparar aplicacion valida contra catalogo
    - ⬜ Test: detectar atributos faltantes
    - ⬜ Test: calcular compliance percentage
  - Dependencias: TASK-019
  - Complejidad: 2
  - Prioridad: optional

- [ ] 44. Tests Integration: End-to-end (TASK-044)
  - Descripcion: Test de integracion que valida flujo completo
  - Criterios de Aceptacion:
    - ⬜ Cargar catalogo desde archivo real
    - ⬜ Cargar ACES desde archivo real
    - ⬜ Ejecutar validacion completa
    - ⬜ Generar reporte
  - Dependencias: TASK-009, TASK-011, TASK-019, TASK-023
  - Complejidad: 3
  - Prioridad: optional

- [ ] 45. Tests Property-Based: Validation Idempotence (TASK-045)
  - Descripcion: Verificar que validar dos veces produce mismo resultado
  - Criterios de Aceptacion:
    - ⬜ Validar misma aplicacion dos veces
    - ⬜ Verificar que resultados son identicos
  - Dependencias: TASK-019
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 10: TESTING - REPORTES

- [ ] 46. Tests para `ExcelReportGenerator` (TASK-046)
  - Descripcion: Unit tests para generador de reportes Excel
  - Criterios de Aceptacion:
    - ⬜ Test: generar reporte sin errores
    - ⬜ Test: generar reporte con errores
    - ⬜ Test: verificar que hojas originales no se modifican
  - Dependencias: TASK-023
  - Complejidad: 2
  - Prioridad: optional

- [ ] 47. Tests Property-Based: Report Completeness (TASK-047)
  - Descripcion: Verificar que reporte contiene todos los errores
  - Criterios de Aceptacion:
    - ⬜ Generar ComparisonResult aleatorio
    - ⬜ Verificar que todos los errores aparecen en reporte
  - Dependencias: TASK-023
  - Complejidad: 2
  - Prioridad: optional

---

### Componente 11: DOCUMENTACION

- [ ] 48. Documentacion de API (TASK-048)
  - Descripcion: Generar documentacion JavaDoc de todas las clases publicas
  - Criterios de Aceptacion:
    - ⬜ JavaDoc comments para todas las clases
    - ⬜ Generar HTML con javadoc tool
  - Dependencias: Todas las implementaciones
  - Complejidad: 2
  - Prioridad: optional

- [ ] 49. Guia de Usuario (TASK-049)
  - Descripcion: Crear guia de usuario para GUI
  - Criterios de Aceptacion:
    - ⬜ Instrucciones paso a paso
    - ⬜ Solucion de problemas comunes
  - Dependencias: TASK-027 al TASK-034
  - Complejidad: 2
  - Prioridad: optional

- [ ] 50. Guia de Configuracion (TASK-050)
  - Descripcion: Crear documentacion de configuracion
  - Criterios de Aceptacion:
    - ⬜ Parametros configurables documentados
    - ⬜ Ejemplos de configuracion
  - Dependencias: TASK-033
  - Complejidad: 1
  - Prioridad: optional

---

### Componente 12: EMPAQUETADO

- [ ] 51. Configurar build.xml (Ant) (TASK-051)
  - Descripcion: Configurar script de compilacion con Apache Ant
  - Criterios de Aceptacion:
    - ⬜ Target para compilar (compile)
    - ⬜ Target para limpiar (clean)
    - ⬜ Target para empaquetar JAR (jar)
  - Dependencias: Ninguna
  - Complejidad: 2
  - Prioridad: required

- [ ] 52. Crear archivo de Manifest (TASK-052)
  - Descripcion: Crear MANIFEST.MF para JAR ejecutable
  - Criterios de Aceptacion:
    - ⬜ Main-Class apuntando a clase launcher
    - ⬜ Class-Path con todas las dependencias
  - Dependencias: TASK-051
  - Complejidad: 1
  - Prioridad: required

- [ ] 53. Crear Launcher (TASK-053)
  - Descripcion: Crear clase Launcher para iniciar aplicacion
  - Criterios de Aceptacion:
    - ⬜ Clase `Launcher` con main() method
    - ⬜ Inicializar MainWindow
  - Dependencias: TASK-027
  - Complejidad: 1
  - Prioridad: required

- [ ] 54. Empaquetar JAR ejecutable (TASK-054)
  - Descripcion: Compilar y empaquetar aplicacion en JAR
  - Criterios de Aceptacion:
    - ⬜ Ejecutar: java -jar validador-aces.jar
    - ⬜ Incluir todas las dependencias en lib/
  - Dependencias: TASK-051, TASK-052, TASK-053
  - Complejidad: 1
  - Prioridad: required

---

## Task Dependency Graph

```
- TASK-001 (Crear modelo de datos `Catalog`) depende de: Ninguna
- TASK-002 (Crear modelo de datos `ProductLine`) depende de: TASK-001
- TASK-003 (Crear modelo de datos `Attribute`) depende de: TASK-002
- TASK-004 (Crear modelo de datos `Application`) depende de: Ninguna
- TASK-005 (Crear modelo de datos `ValidationError`) depende de: Ninguna
- TASK-006 (Crear modelo de datos `ComparisonResult`) depende de: TASK-005
- TASK-007 (Crear modelo de datos `AuditReport`) depende de: TASK-006
- TASK-008 (Crear `CatalogParser` - Estructura base) depende de: TASK-001, TASK-002, TASK-003
- TASK-009 (Implementar `ExcelCatalogParser`) depende de: TASK-008
- TASK-010 (Crear `ApplicationParser` - Estructura base) depende de: TASK-004
- TASK-011 (Implementar `ExcelApplicationParser`) depende de: TASK-010
- TASK-012 (Crear `ValidationSchema`) depende de: TASK-003
- TASK-013 (Implementar `Validator` - Estructura base) depende de: TASK-004, TASK-001, TASK-005
- TASK-014 (Implementar `AttributeValidator`) depende de: TASK-013, TASK-012
- TASK-015 (Implementar `RangeValidator`) depende de: TASK-013, TASK-012
- TASK-016 (Implementar `EnumValidator`) depende de: TASK-013, TASK-012
- TASK-017 (Implementar `CompositeValidator`) depende de: TASK-013, TASK-014, TASK-015, TASK-016
- TASK-018 (Implementar `DateFormatValidator`) depende de: TASK-013, TASK-012
- TASK-019 (Implementar `Comparator`) depende de: TASK-017, TASK-006
- TASK-020 (Implementar `ComplianceCalculator`) depende de: TASK-006
- TASK-021 (Implementar `ComparisonCache`) depende de: TASK-006
- TASK-022 (Crear `ReportGenerator` - Estructura base) depende de: TASK-006
- TASK-023 (Implementar `ExcelReportGenerator`) depende de: TASK-022
- TASK-024 (Implementar `ReportPrinter`) depende de: TASK-006
- TASK-025 (Implementar `ExcelExporter`) depende de: TASK-006
- TASK-026 (Implementar `ReportFilter`) depende de: TASK-006
- TASK-027 (Implementar `MainWindow` - Marco principal) depende de: Ninguna
- TASK-028 (Implementar `CatalogLoadPanel`) depende de: TASK-027
- TASK-029 (Implementar `ApplicationLoadPanel`) depende de: TASK-027
- TASK-030 (Implementar `ValidationPanel`) depende de: TASK-028, TASK-029
- TASK-031 (Implementar `StatisticsPanel`) depende de: TASK-030
- TASK-032 (Implementar dialogos de Archivo) depende de: TASK-027
- TASK-033 (Implementar dialogos de Configuracion) depende de: TASK-027
- TASK-034 (Implementar `AuditPanel`) depende de: TASK-007
- TASK-035 (Tests para `CatalogParser`) depende de: TASK-009
- TASK-036 (Tests para `ApplicationParser`) depende de: TASK-011
- TASK-037 (Tests Property-Based: Round-trip Catalog) depende de: TASK-009
- TASK-038 (Tests Property-Based: Round-trip Application) depende de: TASK-011
- TASK-039 (Tests para `AttributeValidator`) depende de: TASK-014
- TASK-040 (Tests para `RangeValidator`) depende de: TASK-015
- TASK-041 (Tests para `EnumValidator`) depende de: TASK-016
- TASK-042 (Tests para `CompositeValidator`) depende de: TASK-017
- TASK-043 (Tests para `Comparator`) depende de: TASK-019
- TASK-044 (Tests Integration: End-to-end) depende de: TASK-009, TASK-011, TASK-019, TASK-023
- TASK-045 (Tests Property-Based: Validation Idempotence) depende de: TASK-019
- TASK-046 (Tests para `ExcelReportGenerator`) depende de: TASK-023
- TASK-047 (Tests Property-Based: Report Completeness) depende de: TASK-023
- TASK-048 (Documentacion de API) depende de: Todas las implementaciones
- TASK-049 (Guia de Usuario) depende de: TASK-027 al TASK-034
- TASK-050 (Guia de Configuracion) depende de: TASK-033
- TASK-051 (Configurar build.xml (Ant)) depende de: Ninguna
- TASK-052 (Crear archivo de Manifest) depende de: TASK-051
- TASK-053 (Crear Launcher) depende de: TASK-027
- TASK-054 (Empaquetar JAR ejecutable) depende de: TASK-051, TASK-052, TASK-053
```

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": [
        "TASK-001",
        "TASK-004",
        "TASK-005",
        "TASK-027",
        "TASK-048",
        "TASK-051"
      ]
    },
    {
      "wave": 2,
      "tasks": [
        "TASK-002",
        "TASK-006",
        "TASK-010",
        "TASK-013",
        "TASK-028",
        "TASK-029",
        "TASK-032",
        "TASK-033",
        "TASK-052",
        "TASK-053"
      ]
    },
    {
      "wave": 3,
      "tasks": [
        "TASK-003",
        "TASK-007",
        "TASK-011",
        "TASK-020",
        "TASK-021",
        "TASK-022",
        "TASK-024",
        "TASK-025",
        "TASK-026",
        "TASK-030",
        "TASK-050",
        "TASK-054"
      ]
    },
    {
      "wave": 4,
      "tasks": [
        "TASK-008",
        "TASK-012",
        "TASK-023",
        "TASK-031",
        "TASK-034",
        "TASK-036",
        "TASK-038"
      ]
    },
    {
      "wave": 5,
      "tasks": [
        "TASK-009",
        "TASK-014",
        "TASK-015",
        "TASK-016",
        "TASK-018",
        "TASK-046",
        "TASK-047",
        "TASK-049"
      ]
    },
    {
      "wave": 6,
      "tasks": [
        "TASK-017",
        "TASK-035",
        "TASK-037",
        "TASK-039",
        "TASK-040",
        "TASK-041"
      ]
    },
    {
      "wave": 7,
      "tasks": [
        "TASK-019",
        "TASK-042"
      ]
    },
    {
      "wave": 8,
      "tasks": [
        "TASK-043",
        "TASK-044",
        "TASK-045"
      ]
    }
  ]
}
```

## Notes

- **Orden de ejecucion recomendado**: Seguir el orden 1 a 54 respetando dependencias
- **Ajuste post Componente 1**: Los modelos fueron ajustados a la estructura real de los archivos Excel (ver XLSX_STRUCTURE_ANALYSIS.md)
- **Testing**: Tasks 35-47 pueden ejecutarse en paralelo con componentes correspondientes
- **Build**: Tasks 51-54 solo despues de que todo compila sin errores


**Version**: 1.0
**Estado**: En Progreso (28/54 tareas completadas - 52%)
