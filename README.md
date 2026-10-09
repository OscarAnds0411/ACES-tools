# Validador de Atributos ACES

Aplicación de escritorio (Java 11, Swing) que **audita archivos ACES de autopartes** contra un **catálogo maestro** de atributos y genera un reporte Excel de cumplimiento (compliance).

Para cada aplicación del archivo ACES se busca su línea de producto en el catálogo; los atributos marcados `Required` deben tener valor. El compliance es `requeridos satisfechos / requeridos totales`. El detalle de la lógica está en [`LOGICA_DEL_PROGRAMA.md`](LOGICA_DEL_PROGRAMA.md).

## Requisitos

- **JDK 11 o superior** (se compila con `--release 11`; probado con JDK 25).
- **Apache Ant** (opcional; sirve para empaquetar y para `ant test`).
- Dos archivos `.xlsx`: el catálogo y el ACES a validar. No se versionan (`*.xlsx` está en `.gitignore`).

Las librerías están en `lib/` (ver [`lib/README.md`](lib/README.md)); un clon no necesita descargar nada.

## Compilar, probar y ejecutar

Desde la raíz del repositorio. Los comandos completos y verificados están en [`CLAUDE.md`](CLAUDE.md).

**PowerShell** (no usar Git Bash: parte el `;` del classpath):

```powershell
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName

java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner   # pruebas
java -cp "lib\*;$out" com.validador.aces.Launcher                                   # abrir la aplicación
```

**Ant** (`ant` en el PATH, o `ANT_HOME`; `build.bat <target>` también lo encuentra):

| Objetivo | Comando |
|---|---|
| Compilar a `bin/` | `ant compile` |
| Pruebas (falla el build si alguna falla) | `ant test` |
| JAR ejecutable en `dist/` | `ant jar` y luego `java -jar dist/validador-aces.jar` |
| Compilar, empaquetar y ejecutar | `ant run` |
| Limpiar `bin/`, `dist/` y `docs/` | `ant clean` |

Las pruebas que usan los archivos reales (`catalogoDb/`, `ACES/`) se omiten si no existen; ver [`src/com/validador/aces/tests/README.md`](src/com/validador/aces/tests/README.md).

## Uso

1. **Cargar catálogo**: botón *Cargar catálogo*, elegir el `.xlsx` y la hoja (por defecto `Product Line ACES Attributes`). **Debe cargarse antes que el ACES.**
2. **Cargar ACES**: botón *Cargar ACES*, elegir el `.xlsx` y la hoja (se autodetecta la que tenga `Make`, `Model`, `Year` y `Product`). La pantalla informa cuántas filas se omitieron por no tener `Make`.
3. **Ejecutar auditoría**: se habilita cuando ambos archivos están cargados. Muestra métricas y la tabla de atributos faltantes; *Estadísticas* resume por línea de producto y *Historial* lista las auditorías de la sesión.
4. **Exportar reporte**: genera un `.xlsx` con resumen, faltantes, estadísticas por línea y faltantes por número de parte. No permite sobrescribir el ACES original y pide confirmación si el archivo ya existe.

Si cambias el catálogo o el ACES, los resultados anteriores se descartan.

## Configuración

Solo hay una: el diálogo de Configuración (botón ⚙ de la ventana), que guarda `~/.validador_aces_config.properties` con el máximo de filas de la tabla de resultados (por defecto 10 000) y el directorio de exportación por defecto.

## Estructura

```
src/com/validador/aces/
├── models/        datos: Catalog, ProductLine, Attribute, Application, ComparisonResult…
├── parsers/       lectura de los .xlsx
├── validation/    reglas (AttributeValidator, ValidationSchema…)
├── comparison/    Comparator y métricas de compliance
├── reporting/     reporte Excel
├── gui/           interfaz Swing
├── tests/         pruebas con ejecutor propio (sin JUnit)
└── Launcher.java  punto de entrada
lib/               librerías (JAR)         build.xml / build.bat   compilación con Ant
plans/             planes de trabajo       archive/                documentos históricos
```

Cada paquete tiene su propio `README.md`.

## Documentación

- [`LOGICA_DEL_PROGRAMA.md`](LOGICA_DEL_PROGRAMA.md): cómo funciona el programa, paso a paso.
- [`CLAUDE.md`](CLAUDE.md): comandos verificados y convenciones.
- [`tasks.md`](tasks.md): plan de implementación original (con una sección de estado real al inicio).
- [`plans/aces-hardening/`](plans/aces-hardening/README.md): plan de mejoras derivado de una auditoría del código.
- [`archive/`](archive/README.md): notas históricas de planificación y puesta en marcha (pueden estar desactualizadas), incluido el análisis de los archivos de entrada.

## Licencia

Interno - ACES Team.
