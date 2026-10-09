# Lógica del programa — Validador de Atributos ACES

## 1. Qué hace

El programa audita qué tan completos están los datos de **aplicaciones de autopartes** (formato ACES) frente a un **catálogo maestro** que define, por tipo de producto, qué atributos técnicos son obligatorios.

Entradas (ambas `.xlsx`):

| Entrada | Contenido | Hoja por defecto |
|---|---|---|
| **Catálogo** | Una fila por *línea de producto* y una columna por atributo, con el valor `Required`, `Optional` o `Not Required`. | `Product Line ACES Attributes` |
| **Archivo ACES** | Una fila por aplicación (Make, Model, Year, Product, PartNumber…) más columnas de atributos técnicos con sus valores. | `Applications` (o autodetectada) |

Salida: resultados en pantalla (métricas, estadísticas, historial de auditoría) y un **reporte Excel** con el detalle de faltantes.

**Idea central:** para cada aplicación se busca su línea de producto en el catálogo; los atributos `Required` de esa línea deben tener valor no vacío en la aplicación. El porcentaje de *compliance* es `requeridos satisfechos / requeridos totales`.

Tecnología: Java 11+, Swing (GUI), `fastexcel` / `fastexcel-reader` para leer y escribir Excel (JARs en `lib/`), build con Ant (`build.xml`, `build.bat`). Configuración: el diálogo de Configuración de la GUI (`~/.validador_aces_config.properties`).

---

## 2. Arquitectura por paquetes

```
Launcher ──► gui ──► parsers ──► models
                 └─► comparison ──► validation ──► models
                 └─► reporting  ──► comparison / models
```

| Paquete | Responsabilidad |
|---|---|
| `Launcher` | Punto de entrada. Activa el look & feel Nimbus y, en el hilo de Swing (EDT), crea `MainWindow` y registra los paneles. |
| `models` | Datos puros: `Catalog`, `ProductLine`, `Attribute`, `AttributeRequirement`, `Application`, `ValidationError`, `ComparisonResult`, `AuditReport`, enums. |
| `parsers` | Leen los Excel y construyen los modelos (`ExcelCatalogParser`, `ExcelApplicationParser`). |
| `validation` | Reglas de validación (`Validator` y subclases) y `ValidationSchema`. |
| `comparison` | Orquesta la comparación (`Comparator`) y calcula métricas (`ComplianceCalculator`, `ComplianceMetrics`, `ProductLineSummary`). |
| `reporting` | Convierte resultados en `Report`/`ReportSection` y los escribe a Excel. |
| `gui` | Ventana principal y paneles (carga de catálogo, carga de ACES, validación, estadísticas, auditoría, configuración). |
| `tests` | Pruebas propias con `TestRunner` y `Assert` (sin JUnit). |

---

## 3. Modelo de datos

- **`Catalog`** — contiene una lista de `ProductLine`. `findProductByName()` busca por nombre **ignorando mayúsculas y espacios en los extremos**. `getAllAttributeNames()` devuelve la unión de todos los atributos de todas las líneas.
- **`ProductLine`** — una línea de producto (id, nombre, categoría, subcategoría) con su lista de `Attribute`.
- **`Attribute`** — nombre + `AttributeRequirement`. `isSatisfiedBy(valor)`:
  - si el atributo **no** es `Required` → siempre `true`;
  - si es `Required` → `false` cuando el valor es `null` o una cadena vacía/solo espacios.
- **`AttributeRequirement`** — `REQUIRED`, `OPTIONAL`, `NOT_REQUIRED`. `fromCellValue()` interpreta el texto de la celda **sin distinguir mayúsculas ni espacios en los extremos** (`required`, ` Optional `); un valor desconocido (por ejemplo `Req`) o `null` cuenta como `NOT_REQUIRED`. `isRecognized()` permite detectar esos valores desconocidos para avisar al usuario.
- **`Application`** — metadatos (`make`, `model`, `year`, `product`, `partNumber`, `mfrLabel`, `position`) y un mapa `data` con los atributos técnicos (nombre de columna → valor). `getProductName()` devuelve `product`, que es el vínculo con el catálogo.
- **`ValidationError`** — severidad (`ErrorSeverity`: `ERROR` / `WARNING`), código, mensaje, nombre de atributo y línea de producto.
- **`ComparisonResult`** — resultado por aplicación: errores, advertencias, `totalAttributes`, `validAttributes`, `invalidAttributes`, `partNumber`, atributos opcionales y opcionales faltantes.
- **`AuditReport` / `AuditAction`** — registro de cada comparación (usuario, acción, detalle, resumen, duración).

---

## 4. Flujo completo de ejecución

```
1. Cargar catálogo ──► Catalog
2. Cargar ACES     ──► List<Application>   (solo columnas que existan en el catálogo)
3. Ejecutar auditoría ──► List<ComparisonResult>
4. Agrupar por producto ──► métricas y estadísticas
5. Exportar reporte Excel
```

La ventana principal (`MainWindow`) usa un `CardLayout` con las tarjetas `WELCOME`, `CATALOG`, `ACES`, `RESULTS`, `STATISTICS` y `AUDIT_HISTORY`. Guarda el estado compartido (catálogo cargado, aplicaciones cargadas, último archivo ACES, últimos resultados, historial de auditoría). El botón **Ejecutar** solo se habilita cuando catálogo y aplicaciones están cargados (`setRunEnabled`).

Todas las operaciones pesadas (leer Excel, comparar, exportar) corren en un `SwingWorker` para no congelar la interfaz.

### 4.1 Carga del catálogo (`ExcelCatalogParser`)

1. Se listan las hojas del archivo (`readSheetNames`) para que el usuario elija; por defecto `Product Line ACES Attributes`.
2. Fila 1 = encabezados. Las columnas **0–3** son fijas (ID, nombre, y dos campos de categoría); desde la columna **4** cada encabezado es un atributo. Un encabezado vacío se omite con un aviso, **sin desplazar** las columnas siguientes (cada atributo conserva su columna original).
3. Cada fila siguiente genera una `ProductLine`:
   - sin ID → se ignora la fila en silencio;
   - sin nombre → se omite con aviso;
   - por cada atributo se crea un `Attribute` con el texto de la celda (celda vacía = `"Not Required"`); un valor que no es `Required`/`Optional`/`Not Required` genera un aviso.
4. El nombre del `Catalog` es el nombre del archivo sin extensión.
5. Los avisos de la lectura (columnas sin encabezado, filas omitidas, valores no reconocidos) quedan en `ExcelCatalogParser.getWarnings()` (máximo 50 detallados) y `CatalogLoadPanel` los muestra en pantalla.

### 4.2 Carga del archivo ACES (`ExcelApplicationParser`)

1. Si no se indica hoja, se **autodetecta**: la primera hoja cuyo encabezado contenga `Make`, `Model`, `Year` y `Product`.
2. En el encabezado, las columnas `Make/Model/Year/Product` (obligatorias) y `PartNumber/MfrLabel/Position` (opcionales) son **núcleo** (insensibles a mayúsculas). **Cualquier otra columna es un atributo técnico.** Si falta una obligatoria se lanza `ParseException`.
3. **Optimización clave:** el panel le pasa al parser `catalog.getAllAttributeNames()` como `headersOfInterest`. Solo se leen las columnas de atributos que el catálogo conoce, y se guardan con el **nombre canónico del catálogo** (la coincidencia de nombres ignora mayúsculas). Esto reduce mucho la memoria en archivos de hasta 94 columnas × miles de filas.
4. Por fila: si `Make` está vacío se omite y se cuenta (`getSkippedRowCount()`); `ApplicationLoadPanel` muestra ese número, porque esas aplicaciones no se auditan ni aparecen en el reporte. Las celdas vacías se guardan como `null`.

> Por eso el catálogo debe cargarse **antes** que el archivo ACES.

### 4.3 Validación (`Comparator` → `AttributeValidator`)

`ValidationPanel.startValidation()` llama a `comparator.compareAll(apps, catalog)` en segundo plano. Para un lote, `Comparator`:

1. Construye un **índice** `nombre-de-línea-normalizado → ProductLine` y un **caché de `ValidationSchema`** por línea (se calculan una sola vez, no por aplicación).
2. Por cada aplicación resuelve **una sola vez** su línea y su esquema en esos índices (`product`, en minúsculas y sin espacios en los extremos) y llama a `AttributeValidator.validateResolved(app, línea, esquema)`, sin volver a buscar en el catálogo. Si el catálogo tiene varias líneas con el mismo nombre, **gana la primera** (igual que `Catalog.findProductByName`, que usa `compare` para una sola aplicación).
3. `AttributeValidator`:
   - **Línea no encontrada** → un `WARNING` con código `PRODUCT_LINE_NOT_FOUND` (la aplicación queda "sin clasificar") y termina.
   - Si se encuentra → `ValidationSchema` separa los atributos en requeridos y opcionales y se revisa cada requerido con `Attribute.isSatisfiedBy`. Cada faltante genera un `ERROR` con código `MISSING_REQUIRED_ATTRIBUTE`.
4. Se arma el `ComparisonResult`:
   - `total` = nº de atributos requeridos de la línea;
   - `invalid` = nº de errores `MISSING_REQUIRED_ATTRIBUTE` (de la misma línea, así que `0 ≤ invalid ≤ total`);
   - `valid` = `total - invalid`;
   - lista de opcionales de la línea y cuáles faltan en la aplicación.
   - Sin línea → total/valid/invalid = 0.
5. Se registra un `AuditReport` de tipo `COMPARISON` con duración y totales; el panel lo añade al historial.

`CompositeValidator` ejecuta una lista de `Validator` y acumula errores; con `stopOnFirstCriticalError` se detiene en el primer `ERROR`. Hoy el `Comparator` ya no lo usa (solo hay un validador), por lo que `Comparator.setStopOnFirstCriticalError` se conserva por compatibilidad pero no tiene efecto.

**Validadores disponibles pero no conectados al flujo actual** (el `Comparator` solo usa `AttributeValidator`): `EnumValidator` (valor dentro de una lista), `RangeValidator` (rango numérico; ignora valores no numéricos) y `DateFormatValidator`. Existen y tienen pruebas (salvo `DateFormatValidator`), y son el punto de extensión para validar valores, no solo presencia. Conectarlos exige decidir dónde se declaran las reglas (el catálogo solo dice Required/Optional) y cambiaría la semántica del compliance.

### 4.4 Métricas (`ComplianceCalculator`)

`ValidationPanel.buildResultsMap` agrupa los resultados por **nombre de producto** de cada aplicación (vacío → `"(sin producto)"`). Luego:

- **`ComplianceMetrics`** — `compliance % = satisfechos / totales × 100`. Si no hay atributos requeridos (`total = 0`) la métrica es **N/A** (no 0 %), para no mezclarla con aplicaciones realmente incumplidas.
- `calculateByProductLine` — suma requeridos totales y satisfechos de todas las aplicaciones de la línea (el porcentaje es ponderado por atributos, no un promedio de promedios).
- **`ProductLineSummary`** — por línea: nº de aplicaciones, requeridos totales/satisfechos, atributos opcionales y, por **número de parte**, qué requeridos y qué opcionales faltan (`PartNumberGap`). Aplicaciones sin número de parte se agrupan bajo `"(sin numero de parte)"`.
- `identifyUnmetRequiredAttributes` obtiene los faltantes leyendo los errores `MISSING_REQUIRED_ATTRIBUTE`.

### 4.5 Resultados en pantalla

`ValidationPanel` alterna entre tres estados (`IDLE` → `RUNNING` → `RESULTS`) y muestra totales: aplicaciones, con 100 % de compliance, con errores y sin clasificar. `StatisticsPanel` muestra las estadísticas por línea de producto, y `AuditPanel` el historial de auditorías de la sesión.

### 4.6 Reporte Excel (`reporting`)

`ExcelReportGenerator.generateBatchReport` crea un `Report` con cuatro `ReportSection`:

1. **Resumen Ejecutivo** — total auditado, con 100 %, con faltantes, sin clasificar, sin requeridos (N/A), líneas auditadas y compliance promedio (solo clasificadas y con requisitos).
2. **Atributos faltantes** — aplicación / línea / atributo, o una fila indicando que no hay faltantes.
3. **Estadísticas por línea de producto** — aplicaciones, requeridos satisfechos/totales, compliance %, opcionales y números de parte con faltantes.
4. **Faltantes por número de parte** — requeridos y opcionales que faltan por cada parte.

`writeReportToFile` lo escribe con `fastexcel`; `defaultReportFileFor(acesFile)` propone el nombre de salida a partir del archivo ACES. El export también corre en un `SwingWorker`.

La escritura es **atómica**: se escribe a un `.tmp` del mismo directorio y luego reemplaza al destino, de modo que un fallo (o el archivo abierto en Excel) deja intacto un reporte existente. Antes de exportar, `ValidationPanel` rechaza elegir el archivo ACES cargado (`ExcelReportGenerator.isSameFile`) y pide confirmación si el destino ya existe.

### 4.7 Coherencia del estado en la GUI

Al cargar otro catálogo u otro ACES (o si la carga falla), `MainWindow` descarta los resultados de la auditoría anterior y avisa a sus oyentes (`addResultsInvalidatedListener`): `ValidationPanel` vuelve al estado inicial y `StatisticsPanel` muestra "Sin resultados". Así no se puede exportar un reporte que no corresponde a los archivos cargados.

---

## 5. Decisiones de diseño relevantes

- **Coincidencia por nombre, sin distinguir mayúsculas**: producto de la aplicación ↔ nombre de la línea, y columnas ACES ↔ atributos del catálogo.
- **Producto desconocido = advertencia, no error**: no se penaliza el compliance, se reporta aparte como "sin clasificar".
- **Solo los `Required` cuentan** para el porcentaje; los `Optional` solo se reportan como información.
- **Un valor en blanco equivale a ausente**.
- **Rendimiento**: índices y esquemas precalculados por lote, lectura parcial de columnas, hilos de fondo en la GUI.
- **Extensibilidad**: nuevas reglas = subclase de `Validator` agregada al `CompositeValidator`; nuevos formatos = subclase de `CatalogParser` / `ApplicationParser` / `ReportGenerator`.

---

## 6. Configuración y pruebas

- **Configuración**: la única fuente es el diálogo de Configuración (`ConfigDialog`), que guarda `~/.validador_aces_config.properties` con dos opciones: máximo de filas de la tabla de resultados (`maxTableRows`, por defecto 10 000) y directorio de exportación por defecto. No existe otro archivo de configuración.
- Pruebas en `tests/` (parsers, validadores, comparador y su equivalencia `compare`/`compareAll`, idempotencia, completitud y escritura segura del reporte, extremo a extremo, ida y vuelta de Excel), ejecutadas con `TestRunner`. Las que usan los archivos reales (`catalogoDb/`, `ACES/`, no versionados) se **omiten** si faltan. Ver `src/com/validador/aces/tests/README.md`.
- Compilar, probar y ejecutar: comandos verificados en `CLAUDE.md` (PowerShell y `ant compile / test / jar / run`; `build.bat <target>` también sirve).
