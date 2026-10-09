# Paquete: parsers

Leen los archivos `.xlsx` y construyen los modelos. Usan `fastexcel-reader` (ver `lib/README.md`).

| Clase | Rol |
|---|---|
| `CatalogParser`, `ApplicationParser` | Clases base abstractas: `parse(File)` / `parse(String)` y validación de que el archivo exista |
| `ExcelCatalogParser` | Lee el catálogo maestro → `Catalog` |
| `ExcelApplicationParser` | Lee el ACES → `List<Application>` |
| `ParseException` | Error de lectura (archivo, hoja o formato inválidos) |

## `ExcelCatalogParser`

- Hoja por defecto: `Product Line ACES Attributes` (`readSheetNames(File)` lista las hojas para que la GUI elija).
- Fila 1 = encabezados. Columnas 0–3 fijas (ID, nombre, categoría, subcategoría); desde la 4, cada encabezado es un atributo con valor `Required` / `Optional` / `Not Required` (sin distinguir mayúsculas).
- Un encabezado vacío se omite **sin desplazar** las columnas siguientes.
- `getWarnings()` devuelve los avisos de la última lectura (columnas sin encabezado, filas sin nombre, valores de requisito no reconocidos); la GUI los muestra.

## `ExcelApplicationParser`

- Hoja por defecto: `Applications`; si no se indica, se autodetecta la primera con `Make`, `Model`, `Year` y `Product`.
- Las columnas se localizan por el nombre del encabezado: `Make/Model/Year/Product` (obligatorias) y `PartNumber/MfrLabel/Position` (opcionales) son el núcleo; cualquier otra es un atributo técnico.
- `parse(file, hoja, headersOfInterest)` solo lee las columnas que el catálogo conoce (menos memoria); por eso el catálogo se carga primero.
- Las filas sin `Make` se omiten; `getSkippedRowCount()` dice cuántas.

Ver `LOGICA_DEL_PROGRAMA.md` §4.1–4.2 para el detalle.
