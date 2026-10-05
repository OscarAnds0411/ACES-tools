package com.validador.aces.reporting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;

/**
 * Genera representaciones textuales de reportes de auditoría ACES,
 * adecuadas para consola, logs o salidas de texto plano.
 *
 * <p>A diferencia de {@link ExcelReportGenerator}, este printer no escribe
 * archivos: devuelve un {@code String} formateado con pseudo-tabla ASCII
 * que puede enviarse directamente a {@code System.out}, un logger o cualquier
 * otro destino de texto.</p>
 */
public class ReportPrinter {

    private static final String SEP_THICK = "=".repeat(80);
    private static final String SEP_THIN  = "-".repeat(80);
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Genera el reporte de texto para un único {@link ComparisonResult}.
     *
     * <p>El reporte incluye:</p>
     * <ul>
     *   <li>Cabecera con nombre de aplicación y catálogo.</li>
     *   <li>Sección de resumen ejecutivo: compliance %, atributos totales,
     *       satisfechos, incumplidos, errores y advertencias.</li>
     *   <li>Sección de errores/advertencias en tabla ASCII con columnas
     *       {@code Severidad | Atributo | Línea de Producto | Mensaje}.
     *       Si no hay problemas, muestra "Sin problemas detectados".</li>
     *   <li>Pie con fecha de comparación.</li>
     * </ul>
     *
     * @param result resultado de comparación a imprimir; no puede ser null
     * @return cadena con el reporte formateado, lista para imprimir o loguear
     * @throws IllegalArgumentException si {@code result} es null
     */
    public String print(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }

        StringBuilder sb = new StringBuilder();

        // Cabecera
        sb.append(SEP_THICK).append('\n');
        sb.append("  REPORTE DE AUDITORÍA ACES\n");
        sb.append(SEP_THICK).append('\n');
        sb.append(String.format("  Aplicación : %s%n", result.getApplicationName()));
        sb.append(String.format("  Catálogo   : %s%n", result.getCatalogName()));
        sb.append('\n');

        // Resumen ejecutivo
        sb.append("RESUMEN EJECUTIVO").append('\n');
        sb.append(SEP_THIN).append('\n');
        sb.append(String.format("  Compliance              : %.1f%%%n",
            result.getCompliancePercentage()));
        sb.append(String.format("  Atributos requeridos    : %d%n",
            result.getTotalAttributes()));
        sb.append(String.format("  Atributos satisfechos   : %d%n",
            result.getValidAttributes()));
        sb.append(String.format("  Atributos incumplidos   : %d%n",
            result.getInvalidAttributes()));
        sb.append(String.format("  Errores                 : %d%n",
            result.getErrorCount()));
        sb.append(String.format("  Advertencias            : %d%n",
            result.getWarningCount()));
        sb.append('\n');

        // Sección de errores/advertencias
        sb.append("DETALLE DE PROBLEMAS").append('\n');
        sb.append(SEP_THIN).append('\n');

        List<ValidationError> combined = new java.util.ArrayList<>();
        combined.addAll(result.getErrors());
        combined.addAll(result.getWarnings());

        if (combined.isEmpty()) {
            sb.append("  Sin problemas detectados — todos los atributos requeridos presentes.\n");
        } else {
            String rowFmt = "  %-10s | %-22s | %-25s | %s%n";
            sb.append(String.format(rowFmt, "Severidad", "Atributo",
                "Línea de Producto", "Mensaje"));
            sb.append("  " + "-".repeat(78)).append('\n');
            for (ValidationError e : combined) {
                sb.append(String.format(rowFmt,
                    safe(e.getSeverity() != null ? e.getSeverity().toString() : null),
                    safe(e.getAttributeName()),
                    safe(e.getProductLine()),
                    safe(e.getMessage())
                ));
            }
        }
        sb.append('\n');

        // Pie
        sb.append(SEP_THIN).append('\n');
        sb.append(String.format("  Comparación realizada: %s%n",
            result.getComparisonDate() != null
                ? result.getComparisonDate().format(DATE_FORMAT)
                : LocalDateTime.now().format(DATE_FORMAT)));
        sb.append(SEP_THICK).append('\n');

        return sb.toString();
    }

    /**
     * Genera un resumen de texto para un lote de resultados de comparación.
     *
     * <p>El resumen incluye totales agregados: aplicaciones auditadas,
     * compliance promedio, apps con 100% compliance y apps con errores.</p>
     *
     * @param results lista de resultados de comparación; no puede ser null
     * @return cadena con el resumen formateado
     * @throws IllegalArgumentException si {@code results} es null
     */
    public String printBatch(List<ComparisonResult> results) {
        if (results == null) {
            throw new IllegalArgumentException("La lista de resultados no puede ser null");
        }

        int total = results.size();
        int fullCompliance = 0;
        int withErrors = 0;
        double complianceSum = 0.0;

        for (ComparisonResult r : results) {
            complianceSum += r.getCompliancePercentage();
            if (r.getErrorCount() == 0 && r.getWarningCount() == 0) {
                fullCompliance++;
            }
            if (r.getErrorCount() > 0) {
                withErrors++;
            }
        }

        double avg = total == 0 ? 0.0 : complianceSum / total;

        StringBuilder sb = new StringBuilder();
        sb.append(SEP_THICK).append('\n');
        sb.append("  RESUMEN DE LOTE — AUDITORÍA ACES\n");
        sb.append(SEP_THICK).append('\n');
        sb.append(String.format("  Aplicaciones auditadas   : %d%n", total));
        sb.append(String.format("  Compliance promedio      : %.1f%%%n", avg));
        sb.append(String.format("  Con 100%% compliance      : %d%n", fullCompliance));
        sb.append(String.format("  Con errores              : %d%n", withErrors));
        sb.append(SEP_THICK).append('\n');

        return sb.toString();
    }

    // ── Utilidades ──────────────────────────────────────────────────────────

    private String safe(String value) {
        return value != null ? value : "-";
    }
}