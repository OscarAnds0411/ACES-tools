# Mejoras de producto (M1a: fila y celda de Excel en cada hallazgo)

Objetivo: que cada atributo faltante diga **en qué fila y celda del ACES** está,
en la tabla de resultados y en el reporte Excel, para corregirlo sin buscarlo a
mano. Planificado desde [`mejora.md`](../../mejora.md) §3.1 (M1a) sobre la
revisión `1ad2131` (rama `main`), 2026-10-12. Las demás mejoras de `mejora.md`
(M2, M1b, M4, M3, M5) no están planificadas aún; se añadirán aquí con el
siguiente número.

Se divide en dos unidades: **001** prepara los datos (parser y modelos, sin
cambios visibles) y **002** cambia las salidas (GUI y reporte), para que cada
cambio sea revisable por separado y la suite siga en verde entre ambos.

Ejecuta en el orden de abajo salvo que las dependencias indiquen otra cosa.
Cada ejecutor: lee el plan completo antes de empezar, respeta sus condiciones de
PARADA y actualiza su fila al terminar. No hagas commit ni push salvo que el
usuario lo pida.

Comandos comunes (PowerShell, raíz del repo; línea base **120 tests**):

```
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName
java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner
```

## Orden de ejecución y estado

| Plan | Título | Esfuerzo | Depende de | Estado |
|------|--------|----------|------------|--------|
| [001](001-source-row-model-parser.md) | Cada aplicación y resultado recuerdan su fila física de Excel | S–M | — | TODO |
| [002](002-source-row-outputs.md) | La tabla y el reporte muestran "Fila Excel" y "Celda" | M | 001 | TODO |

Valores de estado: TODO | IN PROGRESS | DONE | BLOCKED (motivo) | SUPERSEDED (puntero)

## Notas de dependencia

- **001 → 002**: 002 lee `Application.getSourceRow`, `ComparisonResult.getSourceRow`,
  `ExcelApplicationParser.getAttributeColumns` y `ExcelColumns.cellAddress`, que crea 001.

## Registro de reconciliación

Más reciente primero; pocas líneas por entrada.

- **2026-10-12**: planes 001 y 002 escritos desde `mejora.md` (M1a). Nota: `mejora.md`
  fue escrito sobre `da151fc`/`fix/UI-UX`; el árbol actual está en `1ad2131`/`main`
  y las líneas citadas se re-verificaron. Siguiente: 001.

## Considerado y descartado

- Guardar el mapa atributo→columna dentro de cada `ComparisonResult`: se duplicaría
  por cada una de ~80 000 aplicaciones; es un dato por archivo.

## Diferido

- Fila en `PartNumberGap` y hoja "Celdas a corregir" (`mejora.md` §3.3, alternativa a M1b).
- Encabezado en una fila distinta de la 1 (`mejora.md` M5-2): `sourceRow` ya es físico, así que seguirá siendo correcto.
