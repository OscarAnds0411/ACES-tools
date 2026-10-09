# Paquete: comparison

Compara las aplicaciones contra el catálogo y calcula las métricas de cumplimiento (compliance).

| Clase | Rol |
|---|---|
| `Comparator` | `compare(app, catalog)` (una aplicación) y `compareAll(apps, catalog)` (lote) → `ComparisonResult` |
| `ComplianceCalculator` | Métricas por resultado y agrupadas por línea de producto; resúmenes y faltantes |
| `ComplianceMetrics` | Requeridos totales / satisfechos / faltantes y porcentaje (N/A si no hay requeridos) |
| `ProductLineSummary` | Resumen por línea: aplicaciones, requeridos, opcionales y números de parte con faltantes |

## `Comparator`

- `compareAll` construye **una vez** un índice `nombre normalizado → ProductLine` y un caché de `ValidationSchema`; cada aplicación resuelve su línea y esquema una sola vez y se valida con `AttributeValidator.validateResolved`.
- Nombres de línea duplicados (sin distinguir mayúsculas ni espacios en los extremos): **gana la primera**, igual en `compare` y `compareAll`.
- Total e inválidos salen del mismo esquema, así que `0 ≤ inválidos ≤ total`.
- Registra un `AuditReport` por invocación (`getLastAuditReport()`).
- `setStopOnFirstCriticalError` se conserva por compatibilidad; con un único validador no tiene efecto.

## Compliance

`satisfechos / requeridos × 100`. Si una línea no tiene atributos requeridos el resultado es **N/A**, no 0 %. `calculateByProductLine` pondera por atributos, no promedia porcentajes.

Ver `LOGICA_DEL_PROGRAMA.md` §4.3–4.4.
