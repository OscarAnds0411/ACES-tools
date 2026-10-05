package com.validador.aces.reporting;

import java.util.ArrayList;
import java.util.List;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;

/**
 * Contrato base de los generadores de reporte del sistema.
 *
 * <p>Cada implementación concreta ({@code ExcelReportGenerator},
 * y en el futuro posibles generadores PDF/HTML) produce un {@link Report}
 * agnóstico de formato a partir de un {@link ComparisonResult}, y es
 * responsabilidad de cada subclase renderizar ese {@code Report} al medio
 * de salida correspondiente (hoja de Excel, documento PDF, página HTML,
 * texto de consola, etc.).</p>
 *
 * <p>Esta clase provee además utilidades protegidas
 * ({@link #buildSummarySection(ComparisonResult)} y
 * {@link #buildErrorsSection(ComparisonResult)}) para construir las dos
 * secciones genéricas más comunes a cualquier formato, evitando que cada
 * subclase concreta duplique la lógica de qué datos componen un resumen o
 * un listado de errores/advertencias.</p>
 */
public abstract class ReportGenerator {

    /**
     * @return nombre identificador del formato de salida que produce esta
     *         implementación (ej. "EXCEL", "PDF", "HTML", "TEXT"). Permite
     *         distinguir en tiempo de ejecución qué tipo de generador se
     *         está usando, sin depender de {@code instanceof}.
     */
    public abstract String getFormatName();

    /**
     * Genera el reporte agnóstico de formato correspondiente al resultado
     * de comparación indicado.
     *
     * @param result resultado de comparación de una aplicación contra el catálogo
     * @return reporte compuesto por las secciones que la implementación concreta decida incluir
     * @throws IllegalArgumentException si {@code result} es null
     */
    public abstract Report generate(ComparisonResult result);

    /**
     * Construye la sección de resumen ejecutivo genérica para un resultado
     * de comparación: aplicación, catálogo, totales de atributos y
     * porcentaje de cumplimiento.
     *
     * @param result resultado de comparación de origen
     * @return sección de resumen con dos columnas ("Campo", "Valor")
     * @throws IllegalArgumentException si {@code result} es null
     */
    protected ReportSection buildSummarySection(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }

        List<String> headers = new ArrayList<>();
        headers.add("Campo");
        headers.add("Valor");

        ReportSection section = new ReportSection("Resumen Ejecutivo", headers);
        section.addRow(buildRow("Aplicación", result.getApplicationName()));
        section.addRow(buildRow("Catálogo", result.getCatalogName()));
        section.addRow(buildRow("Total de atributos requeridos", String.valueOf(result.getTotalAttributes())));
        section.addRow(buildRow("Atributos satisfechos", String.valueOf(result.getValidAttributes())));
        section.addRow(buildRow("Atributos incumplidos", String.valueOf(result.getInvalidAttributes())));
        section.addRow(buildRow("Compliance", String.format("%.1f%%", result.getCompliancePercentage())));
        section.addRow(buildRow("Total de errores", String.valueOf(result.getErrorCount())));
        section.addRow(buildRow("Total de advertencias", String.valueOf(result.getWarningCount())));

        return section;
    }

    /**
     * Construye la sección genérica de errores y advertencias para un
     * resultado de comparación, combinando ambas listas en una sola tabla
     * con una columna de severidad para distinguirlas.
     *
     * <p>Si el resultado no tiene errores ni advertencias, la sección
     * contiene una única fila informativa indicando que la aplicación está
     * completa, en lugar de quedar vacía.</p>
     *
     * @param result resultado de comparación de origen
     * @return sección con columnas: Severidad, Código, Atributo, Línea de Producto, Mensaje
     * @throws IllegalArgumentException si {@code result} es null
     */
    protected ReportSection buildErrorsSection(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }

        List<String> headers = new ArrayList<>();
        headers.add("Severidad");
        headers.add("Código");
        headers.add("Atributo");
        headers.add("Línea de Producto");
        headers.add("Mensaje");

        ReportSection section = new ReportSection("Atributos Faltantes", headers);

        List<ValidationError> combined = new ArrayList<>();
        combined.addAll(result.getErrors());
        combined.addAll(result.getWarnings());

        if (combined.isEmpty()) {
            section.addRow(buildRow(
                "INFO", "-", "-", "-", "Completo - Todos los atributos requeridos están presentes"));
            return section;
        }

        for (ValidationError error : combined) {
            section.addRow(buildRow(
                error.getSeverity() != null ? error.getSeverity().toString() : "-",
                error.getCode() != null ? error.getCode() : "-",
                error.getAttributeName() != null ? error.getAttributeName() : "-",
                error.getProductLine() != null ? error.getProductLine() : "-",
                error.getMessage() != null ? error.getMessage() : "-"
            ));
        }

        return section;
    }

    /** Construye una fila a partir de valores variádicos, como lista mutable independiente. */
    private List<String> buildRow(String... values) {
        List<String> row = new ArrayList<>(values.length);
        for (String value : values) {
            row.add(value);
        }
        return row;
    }
}