# Paquete: tests

Pruebas del proyecto. **No usan JUnit**: hay un ejecutor propio (`TestRunner`) y unas aserciones mínimas (`Assert`). Línea base: **120 tests, todos pasan**.

## Cómo ejecutarlas

Desde la **raíz del repositorio** (las rutas a los datos reales son relativas). Comandos verificados en `CLAUDE.md`; en resumen:

```powershell
# PowerShell
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName
java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner

# Ant (falla el build si algún test falla)
ant test
```

El proceso termina con código 0 si todo pasa y 1 si algo falla o si una suite no tiene tests ejecutables. El resumen final indica pasados, fallidos y **omitidos**.

## Suites (todas registradas en `TestRunner.java`)

| Clase | Qué cubre | Datos |
|---|---|---|
| `CatalogParserTest`, `ApplicationParserTest` | Parsers sobre los archivos reales | reales* |
| `CatalogRoundTripTest`, `ApplicationRoundTripTest` | Escribe un .xlsx y lo vuelve a leer | sintéticos |
| `ParserRegressionTest` | Encabezado vacío del catálogo, requisitos sin distinguir mayúsculas, avisos, filas sin Make | sintéticos |
| `AttributeValidatorTest`, `RangeValidatorTest`, `EnumValidatorTest`, `CompositeValidatorTest` | Validadores | sintéticos |
| `ComparatorTest` | `Comparator` con datos en memoria | sintéticos |
| `ComparatorEquivalenceTest` | `compare` ≡ `compareAll`; nombres de línea duplicados (gana la primera) | sintéticos |
| `ValidationIdempotenceTest` | Validar dos veces da lo mismo | sintéticos |
| `ProductLineSummaryTest`, `ReportCompletenessTest` | Resúmenes y completitud del reporte | sintéticos |
| `ExcelReportGeneratorTest` | Reporte Excel, escritura atómica, `isSameFile` | sintéticos |
| `EndToEndTest` | Flujo completo con archivos reales | reales* |

\* **Datos reales**: `catalogoDb/Atributos ACES por línea de producto.xlsx` y `ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx`. No están en git (`*.xlsx` está ignorado). Si faltan, esos tests se **omiten** (`Assert.assumeFileExists`) y se muestran como omitidos; no cuentan como fallo.

## Cómo agregar un test

1. Crea (o edita) una clase `public final class XxxTest` en este paquete, con constructor privado.
2. Cada test es un método `public static void testXxx()` sin parámetros, que usa `Assert.*` (`assertEquals`, `assertTrue`, `assertNotNull`, `assertThrows`…).
3. Si es una clase nueva, **regístrala** en `TestRunner.java` con `runSuite("...", XxxTest.class)`. Una clase sin registrar no se ejecuta.
4. Para datos, construye un `.xlsx` temporal con fastexcel (ejemplos: `CatalogRoundTripTest.writeTmpCatalog`, `ParserRegressionTest`) y bórralo en un `finally`. Si el test necesita un archivo real, empieza con `Assert.assumeFileExists(RUTA)`.
5. Un método `test*` que no sea `public static` sin parámetros se ignora con una advertencia, y una suite sin tests ejecutables falla: así los errores de nombre no pasan desapercibidos.

## Qué no hay

- Pruebas de la GUI (los paneles Swing no se prueban automáticamente).
- Pruebas de `DateFormatValidator`.
- Integración continua.
