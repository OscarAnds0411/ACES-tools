# Plan 002: La tabla y el reporte muestran "Fila Excel" y "Celda" de cada faltante

> **Instrucciones para el ejecutor**: sigue el plan paso a paso. Ejecuta cada
> comando de verificación y confirma el resultado esperado antes de seguir.
> Si ocurre algo de "Condiciones de PARADA", detente y escribe un handback; no
> improvises. Al terminar, actualiza la fila de este plan en `README.md` del
> esfuerzo. No hagas commit ni push salvo que el usuario lo pida.
>
> **Comprobación de deriva (ejecutar primero)**:
> `git diff --stat 1ad2131 -- src/com/validador/aces/gui src/com/validador/aces/reporting src/com/validador/aces/tests`
> y confirma que el plan 001 está `DONE` en `README.md` (existen
> `Application.getSourceRow`, `ComparisonResult.getSourceRow`,
> `ExcelApplicationParser.getAttributeColumns` y `ExcelColumns.cellAddress`).
> Si hay cambios en esos archivos, compara los extractos de "Estado actual" con
> el código vivo; si no coinciden, es una condición de PARADA.

## Estado

- **Esfuerzo**: M
- **Riesgo**: BAJO
- **Depende de**: 001-source-row-model-parser.md
- **Planificado en**: revisión `1ad2131` (rama `main`), 2026-10-12

## Por qué importa

Con la fila ya disponible (plan 001), quien corrige el ACES necesita verla donde
trabaja: en la tabla de resultados de la GUI y en el reporte Excel. Con la
columna "Fila Excel" y la "Celda" (p. ej. `H1532`) puede saltar directo a cada
hueco en vez de buscarlo entre decenas de miles de filas. Esto también cumple
parte de lo que prometían los requisitos 5 y 8 de `requirements.md`.

## Estado actual

- `src/com/validador/aces/reporting/ExcelReportGenerator.java`
  - `generateBatchReport(Map<String,List<ComparisonResult>>)` (≈línea 122) arma
    4 secciones; `generateAndWriteBatch(results, File)` (≈línea 376) la llama.
  - `buildBatchMissingAttributesSection` (≈línea 189) tiene 3 encabezados:
    `"Aplicacion"`, `"Linea de Producto"`, `"Atributo Faltante"`, y por cada
    error con código `AttributeValidator.MISSING_REQUIRED_ATTRIBUTE` añade
    `row(r.getApplicationName(), productLine, attr)`; si no hay faltantes añade
    una fila `row("-", "-", "Sin atributos requeridos faltantes en el lote")`.
  - `applyColumnWidths(Worksheet)` (≈línea 477) fija anchos de columnas 0–4
    (compartidos por todas las secciones, que se escriben una bajo otra en la
    misma hoja).
  - Constantes públicas de títulos: `MISSING_ATTRIBUTES_SECTION`,
    `PART_NUMBER_GAPS_SECTION`.
- `src/com/validador/aces/gui/ValidationPanel.java`
  - Constructor (≈línea 105): `new DefaultTableModel(new String[]{"Aplicación", "Línea de Producto", "Atributo Faltante", "Estado"}, 0)`.
  - `populateUI(results, apps)` (≈línea 373) arma `List<Object[]> rows` con
    **tres** sitios de `new Object[]{ … }` de 4 elementos (sin clasificar, faltante
    y, en el medio, el `continue` para N/A): ≈líneas 391 y 408. Hay un
    `buildTable()` con renderizadores/anchos por índice de columna: revisa que
    no queden desalineados al insertar una columna.
  - El export llama `new ExcelReportGenerator().generateAndWriteBatch(snapshot, finalOut)` (≈línea 502).
- `src/com/validador/aces/gui/MainWindow.java` — guarda `loadedApplications`
  (`setLoadedApplications` ≈línea 605, que llama a `invalidateResults()` si la
  lista cambia) y `loadedAcesFile`.
- `src/com/validador/aces/gui/ApplicationLoadPanel.java` ≈líneas 460-490 — crea
  el `ExcelApplicationParser`, y en `done()` llama `window.setLoadedApplications(apps)`.
- Tests que asumen el reporte actual: `ExcelReportGeneratorTest`
  (`testGenerateBatch_hasThreSections` espera 4 secciones, no mira columnas),
  `ReportCompletenessTest` (secciones del reporte **individual**, no cambian) y
  `EndToEndTest` (genera y relee un reporte real). Exemplar para abrir un
  `.xlsx` generado y leer celdas: `EndToEndTest` ≈línea 95 (`ReadableWorkbook`).

Decisión ya tomada (de `mejora.md` §3.1): se muestra la **fila física** y la
**dirección de celda completa**. El mapa atributo→columna es por archivo y debe
llegar a ambos destinos sin guardarse dentro de cada `ComparisonResult`.

## Comandos que necesitarás

Igual que el plan 001 (PowerShell desde la raíz del repo): compilar con `javac --release 11 …` y correr
`java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` → todas pasan.
Para GUI: el patrón de verificación sin clics está en `mejora.md` §1 (programa
temporal **fuera del repo** que instancia `MainWindow` + paneles sin mostrarlos,
dispara `getBtnRun().doClick()` y lee campos privados por reflexión; usa
`-Duser.home=<carpeta temporal>` para no tocar `~/.validador_aces_config.properties`).

## Alcance

**Dentro**:
- `src/com/validador/aces/reporting/ExcelReportGenerator.java`
- `src/com/validador/aces/gui/MainWindow.java`
- `src/com/validador/aces/gui/ApplicationLoadPanel.java`
- `src/com/validador/aces/gui/ValidationPanel.java`
- tests: `ExcelReportGeneratorTest.java` y/o un nuevo `SourceRowReportTest.java` (registrado en `TestRunner.java`); ajustes mínimos a `EndToEndTest.java` solo si algo se rompe

**Fuera**:
- `models/*`, `parsers/*`, `comparison/*` (plan 001; si falta algo ahí → PARADA).
- `reporting/ReportGenerator.java` (reporte individual, otra estructura).
- La copia del ACES con celdas resaltadas (M1b) y cualquier cambio de cifras de compliance.
- `ProductLineSummary.PartNumberGap` (opcional, ver Notas).

## Pasos

### Paso 1: el generador acepta el mapa de columnas

En `ExcelReportGenerator` añade sobrecargas que reciban el mapa
`Map<String,Integer> attributeColumns` (nombre canónico → índice 0-based; puede ser
`null`):
`generateBatchReport(results, attributeColumns)` y
`generateAndWriteBatch(results, attributeColumns, File)`. Las firmas actuales
**se conservan** y delegan con `null` (no rompen a quienes ya las llaman).
El mapa se pasa por parámetro hasta `buildBatchMissingAttributesSection`; no lo
guardes en un campo del generador (es una instancia que se crea por exportación,
pero no dejes estado oculto).

**Verifica**: compila; suite en verde sin cambiar ningún test (aún no se usa).

### Paso 2: la sección de faltantes gana "Fila Excel" y "Celda"

Cambia `buildBatchMissingAttributesSection` a 5 encabezados, en este orden:
`Aplicacion`, `Linea de Producto`, `Atributo Faltante`, `Fila Excel`, `Celda`.
Reglas:
- `Fila Excel`: `String.valueOf(r.getSourceRow())`, o `"-"` si es 0.
- `Celda`: `ExcelColumns.cellAddress(col, r.getSourceRow())` cuando el mapa
  tiene el atributo **y** la fila es > 0; `"-"` en cualquier otro caso (mapa
  `null`, atributo ausente del mapa, fila desconocida). Nunca lanzar excepción
  por un mapa incompleto.
- La fila "Sin atributos requeridos faltantes en el lote" debe seguir teniendo
  el mismo número de celdas que los encabezados (rellena con `"-"`).
- Revisa `applyColumnWidths`: las dos columnas nuevas caen en los índices 3 y 4
  (hoy anchos 22 y 55, pensados para otras secciones). Elige anchos que no
  deformen las demás secciones; es una decisión de presentación, no cambies los
  anchos de las columnas 0–2.

**Verifica**: compila; los tests existentes siguen pasando (`testGenerateBatch_hasThreSections`
cuenta secciones, no columnas).

### Paso 3: la GUI guarda el mapa por archivo y lo pasa al exportar

- `MainWindow`: añade `Map<String,Integer> loadedAttributeColumns` con
  getter/setter (o amplía `setLoadedApplications`; decide lo más simple). Debe
  **limpiarse junto con las aplicaciones** cuando la carga falla o se descarta
  (`apps == null`), igual que `loadedAcesFile`. No debe disparar
  `invalidateResults()` por sí solo si las aplicaciones no cambiaron.
- `ApplicationLoadPanel.done()`: tras cargar, entrega a la ventana
  `parser.getAttributeColumns()` (el parser ya está en el scope; se usa para
  `getSkippedRowCount()`).
- `ValidationPanel` (export, ≈línea 502): llama a la sobrecarga nueva con
  `window.getLoadedAttributeColumns()`.
- Importante: si el usuario cambia de ACES o de catálogo se vuelve a parsear
  (los atributos de interés dependen del catálogo), así que el mapa siempre
  corresponde al último parseo; el patrón de invalidación del plan 006 de
  `aces-hardening` (resultados vacíos al cambiar de ACES) debe seguir intacto.

**Verifica**: compila; suite en verde.

### Paso 4: la tabla de la GUI muestra "Fila Excel"

En `ValidationPanel`:
- Añade la columna **"Fila Excel"** al modelo, justo después de "Aplicación"
  (o la posición que mejor lea; deja el orden documentado en el Javadoc de la
  clase). Cada `new Object[]{…}` de `populateUI` debe tener el mismo número de
  elementos que el encabezado: para filas sin clasificar usa la fila de la
  aplicación; para faltantes usa `r.getSourceRow()` (o `"-"` si 0).
- Para que la tabla **ordene numéricamente** la columna, el valor debe ser un
  `Integer` y el modelo debe devolver `Integer.class` para esa columna
  (`getColumnClass`), o el ordenador del `TableRowSorter`; si ya hay un sorter,
  respétalo. Revisa `buildTable()` por anchos/renderizadores indexados.
- No cambies el tope `ConfigDialog.getMaxTableRows()` ni la lógica de conteo.

**Verifica**: con el patrón de GUI sin clics (arriba) y un `.xlsx` sintético
con faltantes conocidos en las filas 3 y 7: tras `getBtnRun().doClick()`, el
`tableModel` tiene las filas esperadas y la columna "Fila Excel" contiene 3 y 7.
**Control negativo**: compilar `git archive HEAD src` en otra carpeta
(sin `tests/`) y comprobar que ese mismo programa falla ahí.

### Paso 5: pruebas de salida

Ver Plan de pruebas. Registra la suite nueva en `TestRunner.java` y normaliza a
CRLF los archivos nuevos (`[regex]::Replace($t,'(?<!\r)\n',"`r`n")`).

## Plan de pruebas

En un nuevo `SourceRowReportTest` (patrón: `EndToEndTest` para generar y reabrir
el `.xlsx`; datos sintéticos con fastexcel, como `ApplicationRoundTripTest.writeTmpAces`):

1. **Cabecera y valores**: parsear un ACES sintético (encabezado en fila 1,
   datos desde la 2, **filas sin Make intercaladas** para forzar numeración
   física no consecutiva) con un catálogo sintético
   (`CatalogRoundTripTest.writeTmpCatalog`), comparar, generar el reporte con el
   mapa del parser, abrirlo con `ReadableWorkbook` y comprobar: la fila de
   encabezados de la sección "Atributos Faltantes por Aplicacion" contiene
   `Fila Excel` y `Celda`; el faltante de la aplicación en la fila física 5 trae
   `5` y, p. ej., `F5` si su atributo está en la columna F.
2. **Mapa nulo**: `generateBatchReport(results)` (firma antigua) produce `-`
   en `Celda` y no lanza excepción.
3. **Atributo fuera del mapa** → `Celda = "-"`, `Fila Excel` sigue viniendo.
4. **Sin faltantes**: la fila informativa tiene tantas celdas como encabezados.
5. **Compatibilidad**: los tests existentes de `ExcelReportGeneratorTest`,
   `ReportCompletenessTest` y `EndToEndTest` pasan sin cambios (si alguno asumía
   3 columnas, ajústalo mínimamente y anótalo en el cierre).

Prueba con el ACES real (si existe; **no modificarlo**, trabajar con una copia
en `%TEMP%` si hace falta escribir): `ACES Keep on Green` cumple al 100 %, así
que no tiene faltantes; para ejercitar casos reales usa `ACES Radec 24.09.2026 - Copy.xlsx`
si el usuario ya lo verificó (pendiente P3 de `mejora.md`), o un `.xlsx`
sintético con faltantes. En el reporte generado, abre una fila con faltante en
Excel y confirma que la celda indicada en "Celda" está efectivamente vacía.

**Verifica**: comando de tests → todos pasan.

## Criterios de terminado

- [ ] Compilación y suite completa en verde (línea base 120 + plan 001 + nuevos).
- [ ] La sección "Atributos Faltantes por Aplicacion" tiene 5 columnas y una prueba lo comprueba abriendo el `.xlsx`.
- [ ] La tabla de `ValidationPanel` muestra "Fila Excel" (comprobado con el programa de GUI sin clics y su control negativo).
- [ ] `grep -rn "generateAndWriteBatch(snapshot, finalOut)" src` → sin resultados (la GUI usa la sobrecarga con mapa).
- [ ] Ningún archivo fuera del alcance modificado (`git status`).
- [ ] Archivos nuevos en CRLF.

## Condiciones de PARADA

Detente si:

- El plan 001 no está `DONE` o falta alguno de sus símbolos.
- El código de `populateUI`/`buildTable` no coincide con "Estado actual" o
  insertar la columna rompe el ordenamiento/anchos de forma que exija
  reescribir la tabla.
- Para llevar el mapa hasta el export hay que tocar estado compartido de
  `MainWindow` más allá de un campo (p. ej. se necesita `AuditSession`, ver M5-7).
- Un paso falla dos veces tras un arreglo razonable.
- Hace falta modificar un archivo fuera del alcance.
- El hallazgo de que la celda indicada no corresponde con el Excel real
  (p. ej. columna desplazada por encabezados vacíos): handback con un ejemplo.

Al detenerte, escribe un **handback**: estado actual, resultado deseado y dudas.
Descriptivo, no prescriptivo.

## Notas de mantenimiento

- Quien revise: que `Celda` use el índice de columna del **parseo actual** (si el
  usuario recarga el ACES con otro catálogo, el mapa cambia con él).
- Diferido: columna de fila en `PartNumberGap` / hoja "Celdas a corregir"
  (alternativa de bajo riesgo a la copia resaltada, M1b, ver `mejora.md` §3.3);
  se planifican después de usar la mejora un tiempo.
