# Librerías externas (`lib/`)

Los JAR de esta carpeta se versionan en git (excepción `!lib/*.jar` en `.gitignore`)
para que un clon compile sin descargar nada. El `Class-Path` del JAR ejecutable se
genera automáticamente desde `lib/*.jar` (`build.xml`, target `jar`): para cambiar
una versión basta reemplazar el archivo aquí y actualizar esta tabla.

| JAR | Versión | Uso | Proyecto | Licencia |
|---|---|---|---|---|
| `fastexcel` | 0.20.2 | **Directo**: escribe los reportes `.xlsx` (`reporting/`) | https://github.com/dhatim/fastexcel | **Sin verificar**: el JAR no la declara; confirmar en el `LICENSE` del repositorio antes de redistribuir |
| `fastexcel-reader` | 0.20.2 | **Directo**: lee catálogo y ACES (`parsers/`) | https://github.com/dhatim/fastexcel | **Sin verificar** (igual que `fastexcel`) |
| `opczip` | 1.2.0 | Dependencia de `fastexcel` (según su `pom`) | https://github.com/rzymek/opczip | Apache-2.0 (declarada en el `pom` del JAR) |
| `aalto-xml` | 1.4.0 | Dependencia de `fastexcel-reader` (según su `pom`) | https://github.com/FasterXML/aalto-xml | Apache-2.0 (`Bundle-License` del JAR) |
| `stax2-api` | 4.2.2 | Se asume dependencia de `aalto-xml` (no verificado) | https://github.com/FasterXML/stax2-api | BSD (`Bundle-License` del JAR) |
| `commons-compress` | 1.28.0 | Dependencia de `fastexcel-reader` (según su `pom`) | https://commons.apache.org/proper/commons-compress/ | Apache-2.0 (`Bundle-License` del JAR) |
| `commons-io` | 2.20.0 | No se importa en `src/`; se asume transitiva (no verificado) | https://commons.apache.org/proper/commons-io/ | Apache-2.0 (`Bundle-License` del JAR) |
| `commons-lang3` | 3.18.0 | No se importa en `src/`; se asume transitiva (no verificado) | https://commons.apache.org/proper/commons-lang/ | Apache-2.0 (`Bundle-License` del JAR) |

Notas:

- "Directo" = importado desde `src/`; el resto no se importa en el código del proyecto.
- Las licencias se leyeron de los metadatos de cada JAR; donde no existen, la
  tabla lo dice en lugar de suponerlo.
- No se comprobó si estas versiones tienen vulnerabilidades conocidas. Antes de
  actualizar, revisar las notas de versión de cada proyecto.
