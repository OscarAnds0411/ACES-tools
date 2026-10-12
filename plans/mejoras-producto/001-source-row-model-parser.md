# Plan 001: Cada aplicación y cada resultado recuerdan su fila física de Excel

> **Instrucciones para el ejecutor**: sigue el plan paso a paso. Ejecuta cada
> comando de verificación y confirma el resultado esperado antes de seguir.
> Si ocurre algo de "Condiciones de PARADA", detente y escribe un handback; no
> improvises. Al terminar, actualiza la fila de este plan en `README.md` del
> esfuerzo. No hagas commit ni push salvo que el usuario lo pida.
>
> **Comprobación de deriva (ejecutar primero)**:
> `git diff --stat 1ad2131 -- src/com/validador/aces/models src/com/validador/aces/parsers src/com/validador/aces/comparison src/com/validador/aces/tests`
> Si hay cambios en esos archivos desde la revisión del plan, compara los
> extractos de "Estado actual" con el código vivo; si no coinciden, es una
> condición de PARADA.

## Estado

- **Esfuerzo**: S–M
- **Riesgo**: BAJO
- **Depende de**: ninguno
- **Planificado en**: revisión `1ad2131` (rama `main`), 2026-10-12

## Por qué importa

El reporte y la tabla dicen *aplicación / línea / atributo*, pero no *dónde
está* en el Excel; quien corrige ~80 000 filas busca cada hueco a mano. Este
plan solo prepara los datos: la fila física de origen viaja de la hoja ACES a
cada `ComparisonResult`, y el parser expone en qué columna está cada atributo.
Las salidas (GUI y reporte) las cambia el plan 002.

## Estado actual

Archivos relevantes:

- `src/com/validador/aces/models/Application.java` — fila del ACES. Campos
  `make/model/year/product/partNumber/mfrLabel/position`, `data`; **sin fila de
  origen**. Implementa `Serializable` (`serialVersionUID = 1L`); hoy nada lo
  serializa a disco (comprobar con `grep -rn "ObjectOutputStream" src` → vacío).
- `src/com/validador/aces/parsers/ExcelApplicationParser.java`:
  - `processRows(...)` (≈línea 203) lee la fila 0 como encabezado y luego
    `while (rows.hasNext()) { Application app = buildApplication(rows.next(), columns, attributeColumns); ... }`.
  - `buildApplication(Row row, HeaderColumns columns, Map<String,Integer> attributeColumns)`
    (≈línea 243) crea la `Application`; **no conserva el número de fila**.
  - `selectAttributeColumns(...)` devuelve `Map<String,Integer>` **nombre canónico
    (el del catálogo) → índice de columna 0-based**. Es justo el mapa que se
    necesita exponer; hoy es local a `processRows`.
  - Ya existe el patrón de "estado de la última lectura" con
    `skippedRowCount` / `getSkippedRowCount()` (se reinicia en cada `parse`).
    Imítalo.
- `src/com/validador/aces/parsers/ExcelCatalogParser.java:197` — método privado
  `columnLetter(int index)` (0 → A, 5 → F, 26 → AA). Se usa en la línea ≈187.
- `src/com/validador/aces/models/ComparisonResult.java` — tiene
  `partNumber` con getter/setter (≈línea 236); mismo estilo para el nuevo campo.
- `src/com/validador/aces/comparison/Comparator.java:241` `buildResult(...)`
  hace `result.setPartNumber(application.getPartNumber());`. Es el **único**
  punto que construye `ComparisonResult` en producción (lo usan `compare` y
  `compareAll`).
- `fastexcel-reader 0.20.2`: `Row.getRowNum()` existe (`javap` lo confirma:
  `public int getRowNum()`). **Falta confirmar si es 1-based** y qué hace con
  filas vacías intercaladas: es el primer paso.

Convenciones: Java 11; texto de interfaz, mensajes y Javadoc en **español**;
tests = clases `*Test` con `public static void testXxx()` y `Assert.*`
(no hay JUnit), registradas en `TestRunner.java`. Ejemplo para construir un
`.xlsx` sintético con fastexcel: `ApplicationRoundTripTest.writeTmpAces`
(`src/com/validador/aces/tests/ApplicationRoundTripTest.java`, al final del
archivo). Los `.java` del repo usan **CRLF**: la herramienta Edit lo respeta;
un archivo nuevo creado con Write sale en LF y hay que normalizarlo (ver Paso 6).

## Comandos que necesitarás

Desde la raíz del repo, en **PowerShell** (no Git Bash: parte el `;` del classpath).

| Propósito | Comando | Esperado |
|---|---|---|
| Compilar | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | sin errores |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | todos pasan (línea base: **120**) |
| Tests con Ant (opcional) | `ant test` | BUILD SUCCESSFUL |

## Alcance

**Dentro**:
- `src/com/validador/aces/models/Application.java`
- `src/com/validador/aces/models/ComparisonResult.java`
- `src/com/validador/aces/parsers/ExcelApplicationParser.java`
- `src/com/validador/aces/parsers/ExcelCatalogParser.java` (solo para usar la utilidad compartida)
- nueva utilidad compartida `src/com/validador/aces/parsers/ExcelColumns.java`
- `src/com/validador/aces/comparison/Comparator.java` (solo `buildResult`)
- nuevo `src/com/validador/aces/tests/SourceRowTest.java` y su registro en `TestRunner.java`

**Fuera** (no tocar):
- `gui/*` y `reporting/*` — son el plan 002.
- `ApplicationParser.java` (clase base abstracta): no cambies su contrato; el
  mapa de columnas se expone solo en `ExcelApplicationParser`.
- Los `.xlsx` reales de `catalogoDb/` y `ACES/`: solo lectura, nunca modificarlos.

## Pasos

### Paso 1: confirmar la base de `Row.getRowNum()`

Escribe un programa temporal **fuera del repo** (carpeta scratchpad, no en
`src/`) que cree con fastexcel un `.xlsx` de 4 filas (encabezado en la fila 1,
una fila de datos en la 2, **una fila totalmente vacía en la 3** —déjala sin
escribir—, datos en la 4), lo relea con `ReadableWorkbook` y escriba
`getRowNum()` de cada `Row` que devuelve `openStream()`. Debes poder afirmar:
(a) si la fila 1 de Excel devuelve 1 o 0; (b) si la fila vacía intermedia
desaparece del stream **pero la siguiente conserva su número físico (4)**.

**Verifica**: la salida muestra los números; anótalos en el handback si no son
`1, 2, 4`. Si son `0, 1, 3`, la base es 0 y el plan suma 1 (documéntalo en el
Javadoc del campo). Si el número de la fila tras el hueco **no** es el físico
(p. ej. se renumera `1,2,3`) → PARADA.

### Paso 2: utilidad compartida `columnLetter`

Crea `ExcelColumns` (clase final, constructor privado) en `parsers` con
`public static String columnLetter(int zeroBasedIndex)` — mueve ahí el cuerpo
de `ExcelCatalogParser.columnLetter` sin cambiar su comportamiento, y haz que
`ExcelCatalogParser` la llame (borra su copia privada). Añade en la misma clase
`public static String cellAddress(int zeroBasedColumn, int excelRow)` →
`"H1532"` (letra + fila); el plan 002 la usa.

**Verifica**: compila sin errores; la suite sigue en 120 pruebas, todas pasan
(el aviso de "columna F vacía" del catálogo depende de `columnLetter`).

### Paso 3: `Application.sourceRow`

Añade a `Application` un campo `int sourceRow` (0 = desconocida) con
`getSourceRow()` / `setSourceRow(int)`. Javadoc en español: "fila física de la
hoja de Excel de la que proviene (la que ve el usuario en el margen,
contando la fila de encabezado); 0 si no se conoce". Mantén `equals`/`hashCode`
como están (no incluyas el campo: dos aplicaciones iguales en contenido siguen
siendo iguales). No subas `serialVersionUID`.

**Verifica**: compila.

### Paso 4: el parser asigna la fila y expone el mapa de columnas

En `ExcelApplicationParser`:
- `buildApplication` asigna `app.setSourceRow(row.getRowNum())` (ajustado a
  1-based según el Paso 1).
- Guarda el mapa de columnas seleccionado en un campo de instancia que se
  reinicia en cada `parse` (igual que `skippedRowCount`) y expónlo con
  `public Map<String,Integer> getAttributeColumns()`: **vista no modificable**,
  nombre canónico → índice 0-based; vacío (no `null`) si no hubo lectura.
  Javadoc: es por archivo, no por aplicación.

**Verifica**: compila; la suite sigue en 120, todas pasan.

### Paso 5: el resultado conserva la fila

En `ComparisonResult` añade `int sourceRow` (0 = desconocida) con getter y
setter, al estilo de `partNumber`. En `Comparator.buildResult` añade
`result.setSourceRow(application.getSourceRow());` junto a `setPartNumber`.
`equals`/`hashCode`/`toString` de `ComparisonResult` no cambian.

**Verifica**: compila; la suite sigue en 120, todas pasan.

### Paso 6: tests y normalización

Crea `SourceRowTest` (patrón: `ApplicationRoundTripTest`) y regístrala en
`TestRunner.java` (`runSuite("[MEJORA-001] SourceRowTest", SourceRowTest.class);`
junto a las demás). Normaliza a CRLF los archivos nuevos (`SourceRowTest.java`,
`ExcelColumns.java`) con
`[regex]::Replace($t,'(?<!\r)\n',"`r`n")` y escríbelos en UTF-8.

**Verifica**: la suite pasa con los tests nuevos incluidos (ver Plan de pruebas).

## Plan de pruebas

En `SourceRowTest` (todo con `.xlsx` temporales vía fastexcel, borrándolos en `finally`):

1. **Numeración física**: encabezado en fila 1, datos en 2, 3, 5 y 6 con una
   **fila con Make vacío** en la 4 (se omite) y otra completamente vacía
   intercalada. Las aplicaciones devueltas tienen `getSourceRow()` = 2, 3, 5, 6
   (según lo confirmado en el Paso 1) y `getSkippedRowCount()` = 1.
2. **Mapa de columnas**: con encabezados `Make, Model, Year, Product, Attr1, Attr2`
   y `headersOfInterest = {"attr2"}`, `getAttributeColumns()` contiene solo la
   clave **canónica** `"attr2"` (la que vino en `headersOfInterest`) → índice 5;
   sin `headersOfInterest` contiene ambos atributos; el mapa devuelto no se
   puede modificar (`assertThrows(UnsupportedOperationException.class, …)`).
3. **Reinicio**: un segundo `parse` con otro archivo no arrastra el mapa del primero.
4. **Viaja al resultado**: `Comparator.compare` y `compareAll` copian
   `sourceRow` de la aplicación a su `ComparisonResult` (mismo número en ambos
   caminos; ver `ComparatorEquivalenceTest` como patrón para comparar los dos caminos).
5. **`ExcelColumns`**: `columnLetter(0)="A"`, `(5)="F"`, `(25)="Z"`, `(26)="AA"`,
   `(701)="ZZ"`, `(702)="AAA"`; `cellAddress(7,1532)="H1532"`.

**Verifica**: comando de tests → todos pasan, **120 + los nuevos**.

Prueba con el ACES real (se omite si el archivo no existe; **no lo modifiques**):
programa temporal fuera del repo que haga
`new ExcelApplicationParser().parse("ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx")`
y compruebe `apps.get(0).getSourceRow() == 2` y que la fila de la **última**
aplicación es `81887` (81 886 aplicaciones, encabezado en la fila 1) — salvo que
el contador de filas de Excel diga otra cosa, en cuyo caso gana el contador de Excel.

## Criterios de terminado

- [ ] Compilación sin errores y suite completa en verde (≥ 120 + nuevos).
- [ ] `grep -rn "private static String columnLetter" src` → sin resultados (una sola copia, en `ExcelColumns`).
- [ ] `SourceRowTest` está registrada en `TestRunner.java`.
- [ ] `git status` no muestra archivos fuera del alcance modificados.
- [ ] Los `.java` nuevos terminan en CRLF (`git diff --stat` no avisa de mezcla; en PowerShell: `(Get-Content src\...\SourceRowTest.java -Raw) -match "(?<!\r)\n"` → `False`).

## Condiciones de PARADA

Detente si:

- El código en "Estado actual" no coincide con los extractos.
- `getRowNum()` no devuelve el número físico tras una fila vacía (Paso 1) o
  resulta inconsistente: entrega un handback con el ejemplo concreto.
- Un paso falla dos veces tras un intento razonable de arreglo.
- Cambiar `Application` rompe algo que la serialice (el `grep` de arriba no estaba vacío).
- Hace falta tocar un archivo fuera del alcance (`gui/`, `reporting/`).
- Surge una bifurcación de diseño no prevista (p. ej. dónde guardar el mapa de columnas fuera del parser).

Al detenerte, escribe un **handback** para quien planificó: estado actual del
trabajo, resultado deseado y dudas pendientes. Descriptivo, no prescriptivo.

## Notas de mantenimiento

- `sourceRow` asume que el encabezado está en la **primera fila** de la hoja
  (así lo lee hoy `processRows`; ver M5-2 en `mejora.md`). Si algún día se
  soporta otra fila de encabezado, el número seguirá siendo el físico porque
  viene de `Row.getRowNum()`, no de un contador propio.
- El mapa atributo→columna es por **archivo**; el plan 002 lo guarda en
  `MainWindow` junto a la lista de aplicaciones.
- Diferido: fila en `PartNumberGap` (ver plan 002, opcional).
