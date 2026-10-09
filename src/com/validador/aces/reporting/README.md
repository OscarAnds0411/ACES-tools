# Paquete: reporting

Genera el reporte de auditoría en Excel.

| Clase | Rol |
|---|---|
| `Report`, `ReportSection` | Modelo del reporte: título, fecha y secciones con filas de texto |
| `ReportGenerator` | Clase base abstracta (`getFormatName()`, `generate(ComparisonResult)`) |
| `ExcelReportGenerator` | Única implementación; es la que usa la GUI |

## `ExcelReportGenerator`

- `generateBatchReport(Map<línea, resultados>)` → `Report` con 4 secciones: Resumen ejecutivo, Atributos faltantes, Estadísticas por línea de producto y Faltantes por número de parte. `generate(ComparisonResult)` arma el reporte de una sola aplicación (2 secciones).
- `writeReportToFile(report, archivo)` escribe el `.xlsx` (hoja `REPORT_SHEET_NAME`) con **escritura atómica**: primero a un `.tmp` del mismo directorio y luego reemplaza al destino, así un fallo deja intacto un reporte existente.
- `generateAndWriteBatch(...)` hace ambos pasos.
- `defaultReportFileFor(acesFile)` propone `<nombre>_Reporte_Auditoria.xlsx` junto al ACES.
- `isSameFile(a, b)` comprueba si dos rutas son el mismo archivo; la GUI la usa para no sobrescribir el ACES original.

Usa `fastexcel` (ver `lib/README.md`). Ver `LOGICA_DEL_PROGRAMA.md` §4.6.
