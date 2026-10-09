# Mejoras pendientes — Validador de Atributos ACES

Documento para **retomar el trabajo en otra sesión** sin el contexto de esta. Contiene el estado de partida, lo que quedó pendiente del esfuerzo anterior, y cinco mejoras planificadas (con evidencia en el código, decisiones abiertas con recomendación, pasos y verificación).

> Creado el 2026-10-09 sobre el commit `da151fc` (rama `fix/UI-UX`, árbol limpio). Líneas de código citadas verificadas ese día; si el código cambió, vuelve a comprobarlas.

## Cómo retomar

1. Lee las secciones 1 y 2 (5 minutos).
2. Elige una mejora de la sección 3 (el orden recomendado está en 3.0).
3. Pide, en la nueva sesión, por ejemplo:
   - *"Lee `mejora.md` y convierte la mejora M1a en un plan ejecutable con `writing-plans`"* → crea `plans/mejoras-producto/NNN-….md` con su índice.
   - *"Ejecuta el plan 001 de `plans/mejoras-producto`"* → lo ejecuta y registra el cierre en el índice.
4. Las skills `improve`, `writing-plans` y `skill-security-auditor` están en `.claude/skills/` (versionadas); hay que reiniciar Claude Code para que aparezcan.

El esfuerzo anterior (9 planes, ya cerrados) está en `plans/aces-hardening/README.md`; su registro de cierre explica cada decisión.

---

## 1. Estado de partida

**Proyecto**: app de escritorio Java 11 + Swing que valida un ACES (`.xlsx`) contra un catálogo maestro y genera un reporte Excel. Lógica completa en `LOGICA_DEL_PROGRAMA.md`; comandos y convenciones en `CLAUDE.md`.

**Verificación** (desde la raíz del repo, PowerShell; Git Bash parte el `;` del classpath):

```powershell
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName
java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner    # 120 tests, todos pasan
```
`ant test` (Ant 1.10.18 en `C:\opt\ant`, `ANT_HOME` ya configurado) hace lo mismo y **falla el build** si algo falla.

**Datos reales** (no versionados; **no modificarlos nunca**, trabajar con copias en `%TEMP%`):
- `catalogoDb/Atributos ACES por línea de producto.xlsx`: 38 265 líneas de producto, 42 atributos cada una, solo 12 984 atributos `Required` en total (≈0,34 por línea). Sin nombres de línea duplicados.
- `ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx`: 81 886 aplicaciones; **cumple al 100 %** (0 faltantes, 0 sin clasificar), así que no ejercita ningún caso de falla.
- `ACES/ACES Radec 24.09.2026 - Copy.xlsx`: existe y **nunca se ha ejecutado**; ver pendiente P3.

**Convenciones y trampas aprendidas** (útiles para cualquier ejecutor):
- Todo test nuevo se **registra en `TestRunner.java`**; los que usan datos reales empiezan con `Assert.assumeFileExists(...)` (se omiten, no fallan, si faltan). Ver `src/com/validador/aces/tests/README.md`.
- Los `.java` y `.md` del repo usan **CRLF** (`core.autocrlf=true`). La herramienta Edit lo respeta; los archivos nuevos creados con Write salen en LF: normalizarlos (`[regex]::Replace($t,'(?<!\r)\n',"`r`n")`) antes de cerrar un plan.
- **No hacer commit ni push** salvo que el usuario lo pida (él mismo hace los commits).
- La configuración de la GUI vive en `~/.validador_aces_config.properties`: para probar `ConfigDialog` usar `java -Duser.home=<carpeta temporal>` y **no tocar el archivo real**.
- Patrón de verificación de GUI que funcionó (planes 006 y 008): un programa temporal **fuera del repo** que instancia `MainWindow` + paneles sin mostrarlos, dispara cargas/auditorías con `getBtnRun().doClick()` y lee campos privados por reflexión; más un **control negativo** compilando `git archive HEAD src` (sin la carpeta `tests/`) en otra carpeta y comprobando que el mismo programa falla ahí.
- Patrón de verificación de rendimiento/fidelidad (plan 004): comparar HEAD vs árbol de trabajo con el mismo programa y un checksum CRC32 de todos los resultados.
- Un validador de comandos de la herramienta bloqueó scripts con `Remove-Item` por falso positivo: escribir los scripts auxiliares con Write en la carpeta scratchpad y ejecutarlos con `powershell -File`.
- `ant clean` borra `bin/`, `dist/` y `docs/` (todo generado). No guardar documentación en `docs/`.

---

## 2. Pendientes del esfuerzo anterior (hacer antes o junto con las mejoras)

| # | Pendiente | Qué hacer |
|---|---|---|
| P1 | **Prueba manual de la GUI** (nunca se hicieron clics reales) | Con `catalogo_smoke.xlsx` y `aces_smoke.xlsx` (se regeneran con un programa de 40 líneas con fastexcel; los usados estaban en `%TEMP%\aces-smoke\`): ver avisos al cargar el catálogo (columna F vacía, valor `Req`), "2 filas omitidas" al cargar el ACES, exportar: (a) elegir el ACES cargado → rechaza; (b) elegir un reporte existente → confirma, Cancelar lo deja intacto; (c) nombre nuevo → exporta; cambiar de ACES → Resultados y Estadísticas vuelven a vacío; abrir Configuración y bajar el máximo de filas |
| P2 | **Licencia de `fastexcel` y `fastexcel-reader` sin verificar** (los JAR no la declaran) | Confirmar en `github.com/dhatim/fastexcel` y actualizar `lib/README.md`; imprescindible si se redistribuye |
| P3 | **Contrastar con un ACES que sí tenga faltantes** | Ejecutar la app (o un programa de comparación HEAD vs nuevo, como en el plan 004) contra `ACES Radec…` y revisar a mano 3–5 aplicaciones con faltantes contra el Excel |
| P4 | Documentación vigente incompleta | `tasks.md` tiene TASK-048/049/050 sin hacer (API, guía de usuario, guía de configuración); `.kiro/specs/validador-atributos-aces/design.md` aún menciona clases eliminadas |

---

## 3. Mejoras

### 3.0 Resumen y orden recomendado

| ID | Mejora | Valor | Esfuerzo | Depende de |
|---|---|---|---|---|
| **M1a** | Fila y celda de Excel en cada hallazgo | Quien corrige ~80 000 filas hoy busca cada hueco a mano | S–M | — |
| **M2** | Persistir el historial de auditoría | El historial se pierde al cerrar una herramienta de "auditoría" | S | — |
| **M1b** | Copia del ACES con las celdas resaltadas (**spike primero**) | Corregir directamente sobre el archivo | M | M1a |
| **M4** | Modo sin interfaz (CLI) | Procesar varios archivos / automatizar | M | — (mejor tras M1a) |
| **M3** | Validación de valores (rangos, listas, fechas) | Hoy solo se revisa que el campo no esté vacío | M–L | decisiones del dueño |
| **M5** | Deuda técnica menor | ver lista | S c/u | — |

Orden sugerido: **M1a → M2 → M1b (spike) → M4 → M3**. M3 cambia la semántica del compliance, así que no conviene empezar sin las decisiones de 3.4. Cada mejora cabe en 1–3 planes PR-sized; la descomposición propuesta está dentro de cada una.

### 3.1 M1a — Fila y celda de Excel en cada hallazgo

**Por qué**: el reporte y la tabla dicen *aplicación / línea / atributo*, pero no *dónde está* en el Excel. `Application` no guarda su fila de origen. `requirements.md` (Req 5 y 8) además promete una matriz de cumplimiento y un reporte en el archivo original que nunca se entregaron.

**Evidencia**:
- `Application` (`models/Application.java:23-44`) tiene `make/model/year/product/partNumber/mfrLabel/position` y `data`; **sin fila**.
- `ExcelApplicationParser.processRows` (`parsers/ExcelApplicationParser.java:203-222`) itera filas (`while (rows.hasNext())`, línea 218) y las pasa a `buildApplication` (línea 243) sin conservar su número.
- `fastexcel-reader 0.20.2` expone `Row.getRowNum()` (verificado con `javap`); falta comprobar si es 1-based.
- El reporte "Atributos Faltantes por Aplicacion" tiene solo 3 columnas: `Aplicacion`, `Linea de Producto`, `Atributo Faltante` (`reporting/ExcelReportGenerator.java:~190-197`). La tabla de la GUI tiene `Aplicación`, `Línea de Producto`, `Atributo Faltante`, `Estado` (`gui/ValidationPanel.java`, constructor).

**Decisiones abiertas**:
1. *¿Fila física o índice de datos?* **Recomendado**: la **fila física del Excel** (la que el usuario ve en el margen, contando la fila de encabezado), porque es lo que necesita para ir a corregir.
2. *¿Dirección completa de celda (ej. `H1532`) o solo fila?* **Recomendado**: ambas. La columna del atributo ya se conoce al parsear (`HeaderColumns.attributes: nombre → índice`, `ExcelApplicationParser`), pero hoy no sale del parser; hay que exponer un mapa **por archivo** (no por aplicación): `atributo canónico → índice de columna`. Dónde guardarlo (p. ej. un objeto `AcesSource {archivo, hoja, mapa}` en `MainWindow`) es decisión de diseño del plan.

**Plan propuesto** (2 unidades):
- **001 — Modelo y parser**: `Application.sourceRow` (int; 0 = desconocida) asignado en `buildApplication` con `row.getRowNum()` (verificar la base con una prueba); `ComparisonResult.sourceRow` copiado en `Comparator.buildResult` (así viaja sin cambiar `Map<línea, resultados>`); el parser expone el mapa atributo→columna y una utilidad `columnLetter` (ya existe una privada en `ExcelCatalogParser`; extraerla a un lugar común en vez de duplicarla).
- **002 — Salidas**: columna **"Fila Excel"** en la tabla de `ValidationPanel` (cuidado: `populateUI` arma `Object[]` de 4 columnas, línea ~373+) y columnas **"Fila Excel"** y **"Celda"** en la sección de faltantes del reporte; opcional: fila en `PartNumberGap`.

**Verificación**: tests con `.xlsx` sintéticos (encabezado en fila 1, datos desde la 2, **filas sin Make intercaladas** para probar que la numeración sigue siendo la física); test del reporte que abre el `.xlsx` generado y comprueba cabecera y valores; prueba con el ACES real: la fila de la primera aplicación debe ser 2 y la última 81 887 (comparar con el contador de filas de Excel).

**STOP si**: `getRowNum()` resulta ser 0-based o salta filas vacías de forma inconsistente (devolver un handback con un ejemplo); o si cambiar `Application` rompe la serialización de otro componente (hoy nada la serializa, comprobar con `grep`).

**Riesgo**: bajo; las pruebas de `ExcelReportGeneratorTest` y `ReportCompletenessTest` asumen 3 columnas, hay que actualizarlas.

### 3.2 M2 — Persistir el historial de auditoría

**Por qué**: `MainWindow` guarda `auditHistory` en memoria (`gui/MainWindow.java:578`) y `AuditPanel` solo exporta CSV a demanda; al cerrar se pierde todo.

**Evidencia**: `AuditReport.toJson()` (`models/AuditReport.java:63`) y `toLogString()` ya existen y nadie los usa; **no hay lector** (falta `fromJson`). `addAuditReport` (`MainWindow.java:658`) es el único punto de entrada de nuevas entradas; `clearAuditHistory` (línea ~637) lo usa el botón "Limpiar historial" de `AuditPanel` (línea ~121).

**Decisiones abiertas**:
1. *Formato*: **recomendado** JSON Lines (`~/.validador_aces_audit.jsonl`, una línea por entrada, UTF-8) reutilizando `toJson()` y escribiendo un lector mínimo para ese esquema plano (6 campos) en vez de añadir una dependencia JSON.
2. *Tope*: **recomendado** conservar las últimas 1 000 entradas (reescribir el archivo al pasar el tope).
3. *"Limpiar historial"* debe vaciar también el archivo (pedir confirmación como hoy).

**Plan propuesto** (1 unidad): `AuditHistoryStore` (cargar/añadir/vaciar/recortar, ruta inyectable para pruebas), `MainWindow` carga al iniciar y añade al registrar, `AuditPanel` limpia el archivo. Tolerar un archivo corrupto o con líneas inválidas (saltarlas y seguir; no impedir el arranque).

**Verificación**: tests de ida y vuelta con caracteres especiales (comillas, saltos de línea, acentos, `\t`), recorte al tope, línea corrupta, archivo inexistente; prueba con `-Duser.home` temporal (no tocar el archivo real).

**STOP si**: el historial contiene datos que el dueño no quiere en disco (el campo `details` incluye nombres de aplicación): preguntar antes de persistir.

### 3.3 M1b — Copia del ACES con las celdas resaltadas (spike primero)

**Por qué**: es la entrega que prometía `requirements.md` Req 8 (reporte sobre el ACES), en versión segura: nunca modificar el original (garantía de `tasks.md` y del Req 12).

**Riesgo principal (por eso es un spike)**: `fastexcel` no edita libros; habría que **releer la hoja completa** (todas las columnas; hoy el parser solo lee las del catálogo) y **reescribirla** con `fastexcel` marcando las celdas faltantes. Se perderían formatos, fórmulas, otras hojas y posiblemente los tipos de celda (fechas/números). ACES real: 81 886 filas × ~94 columnas ≈ 7,7 millones de celdas.

**Plan propuesto**:
- **001 (spike, sin código de producto)**: programa temporal fuera del repo que lee la hoja con `ReadableWorkbook` en streaming y escribe una copia con `fastexcel` resaltando N celdas. Medir tiempo y memoria con `-Xmx512m` sobre una **copia** del ACES real; comprobar fidelidad de tipos (texto/número/fecha), que la copia se relee con `fastexcel-reader` y que cada celda resaltada corresponde a un faltante real. Entregar una memo `memo-copia-resaltada.md` con veredicto.
- **002 (según veredicto)**: si el spike pasa → "Exportar copia del ACES con faltantes resaltados" (nunca sobre el original: reutilizar `ExcelReportGenerator.isSameFile` y la escritura atómica del plan 005). Si no pasa → **alternativa recomendada de bajo riesgo**: una hoja **"Celdas a corregir"** en el reporte actual, una fila por celda faltante (`Fila`, `Celda`, `Atributo`, `Make`, `Model`, `Year`, `PartNumber`) ordenada por fila, que se puede filtrar en Excel. Eso es solo una extensión de M1a y no necesita releer el ACES.

**STOP si**: la copia no preserva tipos de celda o supera la memoria razonable → pasar a la alternativa; no insistir.

### 3.4 M3 — Validación de valores (rangos, listas, fechas)

**Por qué**: hoy solo se comprueba que el campo `Required` no esté vacío. Valores como un `EngineLiters` de `-3` o un `VehicleType` inexistente pasan como "cumple".

**Evidencia**: `RangeValidator(String attr, double min, double max)` (`validation/RangeValidator.java:44`), `EnumValidator(String attr, Collection<String> allowed, boolean caseInsensitive)` (`EnumValidator.java:49`) y `DateFormatValidator(String attr[, formatos])` (`DateFormatValidator.java:47,60`) existen; los dos primeros tienen pruebas, el tercero **no**. El `Comparator` solo usa `AttributeValidator` (la aplicación no puede decirles qué atributo validar ni con qué regla: **el catálogo solo dice Required/Optional**). `AttributeType` existe y no lo usa nadie (`grep` fuera de su archivo: ninguno).

**Decisiones abiertas (del dueño; el plan no debe asumirlas)**:
1. *¿Dónde se declaran las reglas?* **Recomendado**: un archivo **opcional y aparte** que se carga como el catálogo (hoja con `Atributo | Tipo | Mín | Máx | Valores permitidos | Formato de fecha`), porque el catálogo es el archivo externo del estándar y no conviene alterarlo. Alternativas: hoja extra dentro del catálogo; archivo `.properties`/JSON junto a la app.
2. *¿Un valor inválido cuenta en el compliance?* **Recomendado**: **no** al principio; reportarlo como `WARNING` con código propio (p. ej. `VALUE_OUT_OF_RANGE`) y columna aparte, para no cambiar los números actuales. Pasar a contarlo solo si el dueño lo pide (cambia la semántica de `LOGICA_DEL_PROGRAMA.md` §4.4).
3. *¿Qué atributos reales tienen reglas?* Requiere datos: ver el primer paso.

**Plan propuesto**:
- **001 (spike)**: perfilar el ACES real (`ACES Keep on Green` y `Radec`): por cada una de las columnas de atributos, tipo dominante, mín/máx numérico, valores distintos (si son pocos) y formatos de fecha. Entregar una propuesta de reglas para que el dueño la corrija. Solo lectura.
- **002**: formato del archivo de reglas + lector + modelo (`ValueRule`), con pruebas.
- **003**: cablear en `Comparator`/`AttributeValidator.validateResolved` (el hook ya existe desde el plan 004: valida con línea y esquema ya resueltos), con un `CompositeValidator` por línea construido **una vez por lote** (no por aplicación) para no perder el rendimiento ganado.
- **004**: GUI (cargar reglas, mostrar/filtrar advertencias de valor) y reporte (columna/sección).

**Verificación**: pruebas de equivalencia con el patrón de `ComparatorEquivalenceTest` (sin reglas el resultado es idéntico al actual, bit a bit); benchmark HEAD vs nuevo con el checksum del plan 004 (no debe degradarse `compareAll`: 0,18 s con los datos reales).

**STOP si**: el dueño decide que los valores inválidos deben contar en el compliance (cambia las cifras históricas: acordar cómo comparar con reportes viejos antes de implementarlo).

### 3.5 M4 — Modo sin interfaz (CLI)

**Por qué**: el motor ya no depende de Swing (`EndToEndTest` lo ejecuta), pero el flujo completo vive en la GUI: la agrupación por línea está en `ValidationPanel.buildResultsMap` (`gui/ValidationPanel.java:363`) y `Launcher` solo abre la ventana.

**Plan propuesto** (2 unidades):
- **001 — `AuditService`** (paquete nuevo o `comparison`): `parse catálogo → parse ACES (con headersOfInterest) → compareAll → agrupar por línea → resultado` sin imports de `javax.swing`; la GUI pasa a usarlo (extraer `buildResultsMap`). Verificar con el patrón de checksum que la GUI produce lo mismo que antes.
- **002 — `Cli`** (`com.validador.aces.Cli`): `java -cp … com.validador.aces.Cli --catalog c.xlsx --aces a.xlsx [--sheet-catalog …] [--sheet-aces …] --out reporte.xlsx`, acepta varios ACES (un reporte por archivo), mensajes en español, y debe correr con `-Djava.awt.headless=true`.

**Decisiones abiertas**: *código de salida*. **Recomendado**: 0 si terminó (aunque haya faltantes), 1 si hubo error de archivo/formato, y una opción `--fallar-si-faltan` que devuelve 2 (útil en automatizaciones). *Salida resumen en JSON*: opcional, solo si se pide.

**Verificación**: prueba que ejecuta el CLI en un proceso hijo con archivos sintéticos y comprueba salida, código y reporte generado; prueba con `-Djava.awt.headless=true`; comprobar que `grep -r "javax.swing" src/.../AuditService*.java` no devuelve nada.

**STOP si**: extraer `buildResultsMap` obliga a tocar el estado compartido de `MainWindow` más allá de lo previsto: parar y proponer primero extraer `AuditSession` (ver M5-7).

### 3.6 M5 — Deuda técnica menor (backlog; un plan pequeño cada una)

| # | Tema | Evidencia / nota |
|---|---|---|
| 1 | **Formato de celdas numéricas** (ceros a la izquierda en números de parte, notación científica, `Year` como `2020.0`) | `ExcelApplicationParser.cell(...)` usa `row.getCellText(...)`. Pista de la auditoría **sin comprobar**: probar con un libro que tenga `PartNumber` y `Year` numéricos |
| 2 | **Fila de encabezado distinta de la 1** y detección de hoja que traga toda excepción | `processRows` toma solo la primera fila; `autoDetectApplicationSheet` tiene un `catch (Exception)` vacío |
| 3 | **Normalización** de acentos y espacios internos; `toLowerCase()` sin `Locale.ROOT` | `Catalog.findProductByName`, `Comparator.normalizeKey`, `ExcelApplicationParser` |
| 4 | **Avisar de nombres de línea duplicados en el catálogo** | Hoy gana la primera en silencio (plan 004); un aviso en `CatalogLoadPanel` usando `getWarnings()` es barato |
| 5 | **Exportación CSV del historial** | `gui/AuditPanel.java:~177-198`: `FileWriter` sin charset, separador `;` con `replace(";", ",")`, sin comillas ni confirmación de sobrescritura, y cambia la extensión de `.xlsx` a `.csv` sin avisar |
| 6 | **Cancelar cargas largas y carrera entre dos selecciones de archivo** | Workers de hojas en `CatalogLoadPanel`/`ApplicationLoadPanel` no se cancelan; sin botón cancelar |
| 7 | **`AuditSession`**: sacar el estado compartido de `MainWindow` a una clase Swing-free | Hace testables la invalidación de resultados y la carga (hoy solo se prueban con un arnés temporal) |
| 8 | **Integración continua** (GitHub Actions: `ant test`) | Los tests con datos reales se omiten sin ellos, así que ya corre en un clon; falta el workflow y quitar la advertencia `includeantruntime` (`build.xml:40`) |
| 9 | **Adoptar `FileDialogHelper`** en los demás paneles | 4 sitios crean su propio `JFileChooser` y olvidan el directorio recordado |
| 10 | **Encabezados duplicados en el catálogo** | `ProductLine.addAttribute` no deduplica; baja confianza, depende de los datos |
| 11 | **Pruebas faltantes** | `DateFormatValidator` sin pruebas; tests débiles (`testDefaultReportFileFor_correctSuffix`, `testAcesOriginalNotModified…` solo compara tamaño) |
| 12 | **Memoria con ACES grande** | Sin medir; probar `-Xmx512m` con el ACES real antes de declararlo problema |

---

## 4. Descartado por ahora (no volver a planificar sin motivo nuevo)

- **Migrar a JUnit / Maven / Gradle**: 8 JAR no lo justifican; el ejecutor propio ya falla de verdad (plan 001).
- **Reactivar `ComparisonCache`, `ReportFilter`, `ExcelExporter`, `ReportPrinter`**: se eliminaron por no tener uso; si hiciera falta algo parecido, recuperar del historial con `git log --diff-filter=D -- <ruta>` y cablearlo de verdad.
- **Fusionar nombres de línea duplicados** en vez de "gana la primera": no hay duplicados en el catálogo real; revisar solo si aparecen.
- **API REST / base de datos** (aparecían como "futuro" en el `application.properties` eliminado): sin demanda ni evidencia.

## 5. Preguntas abiertas para el dueño (resumen)

| Pregunta | Recomendación | Afecta a |
|---|---|---|
| ¿Fila física del Excel y dirección de celda (`H1532`)? | Sí, ambas | M1a |
| ¿Persistir el historial en disco (contiene nombres de aplicación)? | Sí, JSONL con tope de 1 000 | M2 |
| ¿Copia del ACES con celdas resaltadas, o basta una hoja "Celdas a corregir"? | Hoja primero; copia solo si el spike pasa | M1b |
| ¿Dónde se declaran las reglas de valores? | Archivo opcional aparte | M3 |
| ¿Un valor inválido cuenta en el compliance? | No (advertencia aparte) | M3 |
| ¿Código de salida del CLI con faltantes? | 0, con `--fallar-si-faltan` → 2 | M4 |
