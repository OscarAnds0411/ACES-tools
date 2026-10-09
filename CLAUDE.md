# CLAUDE.md — Validador de Atributos ACES

Aplicación de escritorio (Java 11, Swing) que valida archivos ACES `.xlsx` de
autopartes contra un catálogo maestro de atributos Required/Optional y genera un
reporte Excel de compliance. Detalle de la lógica: `LOGICA_DEL_PROGRAMA.md`.

- Nivel de lenguaje: **Java 11** (`build.xml`: `source`/`target` 11). Funciona con JDK más nuevos usando `--release 11`.
- Build: **Apache Ant** (`build.xml`, ayudante `build.bat`). Librerías en `lib/` (ver `lib/README.md`). Sin Maven/Gradle, sin JUnit.
- Entrada de la aplicación: `com.validador.aces.Launcher`.

## Comandos verificados

Todos desde la **raíz del repositorio** (los tests usan rutas relativas).

**PowerShell** (no usar Git Bash: parte el `;` del classpath):

```powershell
# Compilar (salida fuera del repo)
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName

# Ejecutar la suite de tests
java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner

# Abrir la aplicación sin empaquetar
java -cp "lib\*;$out" com.validador.aces.Launcher
```

**Ant** (`ant` en el PATH, o `ANT_HOME`, o `C:\opt\ant`; `build.bat <target>` los busca en ese orden):

| Objetivo | Comando |
|---|---|
| Compilar a `bin/` | `ant compile` |
| Tests (falla el build si algún test falla) | `ant test` |
| JAR ejecutable en `dist/` | `ant jar` |
| Compilar, empaquetar y ejecutar | `ant run` |
| Limpiar `bin/`, `dist/` y `docs/` | `ant clean` |

Línea base: **95 tests, todos pasan**. La consola de Windows puede mostrar `?` en lugar
de acentos y símbolos; es solo la página de códigos, no un fallo.

## Tests

- Clases `*Test` en `src/com/validador/aces/tests/`; cada test es `public static void testXxx()` y usa `Assert.*`. **No hay JUnit.**
- Todo test nuevo debe **registrarse en `TestRunner.java`** (lista de `runSuite(...)`). Una suite sin tests ejecutables cuenta como fallo, y un método `test*` mal declarado muestra una advertencia.
- Los tests que usan archivos reales (`catalogoDb/…xlsx`, `ACES/…xlsx`, ignorados por git) se **omiten** si faltan (`Assert.assumeFileExists`); no cuentan como fallo. Los demás construyen sus `.xlsx` temporales con fastexcel (ejemplo: `CatalogRoundTripTest.writeTmpCatalog`).

## Mapa de paquetes (`src/com/validador/aces/`)

`models` (datos) · `parsers` (leen Excel) · `validation` (reglas) · `comparison` (orquesta y calcula métricas) · `reporting` (reportes Excel) · `gui` (Swing) · `tests`.
Flujo: parsers → `Comparator` → `ComplianceCalculator`/`ProductLineSummary` → `ExcelReportGenerator`; la GUI conecta todo.

## Convenciones

- Texto de interfaz, mensajes y Javadoc en **español**.
- Los `.xlsx` de entrada y datos de prueba no se versionan (`*.xlsx` está en `.gitignore`).
- No editar ni versionar `bin/` ni `dist/` (generados). `docs/` está ignorado y `ant clean` lo borra: no guardar documentación allí.
- Los planes de trabajo viven en `plans/` (ver `plans/aces-hardening/README.md`).
