package com.validador.aces.reporting;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Color;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.validador.aces.comparison.ComplianceCalculator;
import com.validador.aces.comparison.ProductLineSummary;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;

/**
 * Generador de reportes de auditoría ACES en formato Excel (.xlsx).
 *
 * <p>Extiende {@link ReportGenerator} produciendo un {@link Report} agnóstico
 * de formato y, adicionalmente, lo renderiza a un archivo Excel real con
 * formato visual (títulos de sección, encabezados en negrita con relleno,
 * bordes de tabla y resaltado de filas con errores) mediante la librería
 * fastexcel.</p>
 *
 * <h2>Preservación del archivo ACES original</h2>
 * <p>La librería de escritura fastexcel ({@link Workbook}) construye un libro
 * nuevo desde cero a partir de un {@code OutputStream}; no permite abrir un
 * {@code .xlsx} existente y anexarle una hoja conservando las hojas
 * originales. Por lo tanto, para garantizar de forma estructural que el
 * archivo ACES original <strong>nunca</strong> se modifica, este generador
 * escribe el reporte en un archivo <strong>separado</strong> y jamás abre el
 * ACES de entrada en modo escritura. El helper
 * {@link #defaultReportFileFor(File)} produce una ruta hermana segura (con
 * sufijo {@code _Reporte_Auditoria}) para que el llamador no sobrescriba el
 * original por accidente.</p>
 *
 * <h2>Modos de generación</h2>
 * <ul>
 *   <li>{@link #generate(ComparisonResult)}: reporte de una sola aplicación
 *       (contrato base), con secciones de resumen ejecutivo y errores.</li>
 *   <li>{@link #generateBatchReport(Map)}: reporte de auditoría de un lote de
 *       aplicaciones agrupadas por línea de producto, con resumen ejecutivo
 *       agregado, listado de atributos faltantes (aplicación / producto /
 *       atributo) y estadísticas por línea de producto.</li>
 * </ul>
 */
public class ExcelReportGenerator extends ReportGenerator {

    /** Nombre de la hoja Excel donde se escribe el reporte. */
    public static final String REPORT_SHEET_NAME = "Reporte Auditoria ACES";

    /** Título de la sección de estadísticas agregadas por línea de producto. */
    public static final String PRODUCT_LINE_STATS_SECTION = "Estadisticas por Linea de Producto";

    /** Título de la sección de listado de atributos faltantes del lote. */
    public static final String MISSING_ATTRIBUTES_SECTION = "Atributos Faltantes por Aplicacion";

    /** Título de la sección con los números de parte a los que les falta algún atributo. */
    public static final String PART_NUMBER_GAPS_SECTION = "Numeros de Parte con Faltantes";

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ComplianceCalculator complianceCalculator = new ComplianceCalculator();

    @Override
    public String getFormatName() {
        return "EXCEL";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produce un reporte de una sola aplicación con dos secciones: el
     * resumen ejecutivo ({@link #buildSummarySection(ComparisonResult)}) y el
     * listado de errores/advertencias
     * ({@link #buildErrorsSection(ComparisonResult)}).</p>
     */
    @Override
    public Report generate(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        Report report = new Report("Reporte de Auditoria ACES - " + result.getApplicationName());
        report.addSection(buildSummarySection(result));
        report.addSection(buildErrorsSection(result));
        return report;
    }

    /**
     * Genera un reporte de auditoría para un lote de aplicaciones agrupadas
     * por línea de producto.
     *
     * <p>El agrupamiento lo provee el llamador (que conoce la línea de
     * producto de cada aplicación en el momento de comparar), ya que
     * {@link ComparisonResult} no almacena la línea de producto. El reporte
     * resultante contiene tres secciones:</p>
     * <ol>
     *   <li><b>Resumen Ejecutivo</b>: totales agregados (aplicaciones
     *       auditadas, 100% compliant, con faltantes, sin clasificar) y
     *       compliance promedio.</li>
     *   <li><b>{@value #MISSING_ATTRIBUTES_SECTION}</b>: una fila por cada
     *       atributo requerido faltante, con aplicación, producto y atributo.</li>
     *   <li><b>{@value #PRODUCT_LINE_STATS_SECTION}</b>: métricas agregadas por
     *       línea de producto (apps, requeridos, satisfechos, compliance %).</li>
     * </ol>
     *
     * @param resultsByProductLine mapa de nombre de línea de producto a sus resultados de comparación
     * @return reporte de lote con las tres secciones descritas
     * @throws IllegalArgumentException si {@code resultsByProductLine} es null
     */
    public Report generateBatchReport(Map<String, List<ComparisonResult>> resultsByProductLine) {
        if (resultsByProductLine == null) {
            throw new IllegalArgumentException("resultsByProductLine no puede ser null");
        }

        Report report = new Report("Reporte de Auditoria ACES");
        report.addSection(buildBatchSummarySection(resultsByProductLine));
        report.addSection(buildBatchMissingAttributesSection(resultsByProductLine));
        report.addSection(buildProductLineStatsSection(resultsByProductLine));
        report.addSection(buildPartNumberGapsSection(resultsByProductLine));
        return report;
    }

    /**
     * Construye la sección de resumen ejecutivo agregada de un lote.
     */
    private ReportSection buildBatchSummarySection(
            Map<String, List<ComparisonResult>> resultsByProductLine) {
        int totalApps = 0;
        int fullCompliance = 0;
        int withMissing = 0;
        int unclassified = 0;
        int notApplicable = 0;
        double complianceSum = 0.0;
        int complianceCounted = 0;

        for (List<ComparisonResult> group : resultsByProductLine.values()) {
            for (ComparisonResult r : group) {
                totalApps++;
                if (isUnclassified(r)) {
                    unclassified++;
                    continue;
                }
                if (!r.isApplicable()) {
                    notApplicable++;
                    continue;
                }
                complianceSum += r.getCompliancePercentage();
                complianceCounted++;
                if (r.getInvalidAttributes() > 0) {
                    withMissing++;
                } else {
                    fullCompliance++;
                }
            }
        }

        String averageCompliance = complianceCounted == 0
            ? "N/A" : String.format("%.1f%%", complianceSum / complianceCounted);

        List<String> headers = new ArrayList<>();
        headers.add("Metrica");
        headers.add("Valor");

        ReportSection section = new ReportSection("Resumen Ejecutivo", headers);
        section.addRow(row("Total de aplicaciones auditadas", String.valueOf(totalApps)));
        section.addRow(row("Aplicaciones con 100% compliance", String.valueOf(fullCompliance)));
        section.addRow(row("Aplicaciones con atributos faltantes", String.valueOf(withMissing)));
        section.addRow(row("Aplicaciones sin clasificar (producto no hallado)", String.valueOf(unclassified)));
        section.addRow(row("Aplicaciones sin atributos requeridos (N/A)", String.valueOf(notApplicable)));
        section.addRow(row("Lineas de producto auditadas", String.valueOf(resultsByProductLine.size())));
        section.addRow(row("Compliance promedio (clasificadas, con requisitos)", averageCompliance));
        return section;
    }

    /**
     * Construye la sección de listado de atributos faltantes del lote.
     */
    private ReportSection buildBatchMissingAttributesSection(
            Map<String, List<ComparisonResult>> resultsByProductLine) {
        List<String> headers = new ArrayList<>();
        headers.add("Aplicacion");
        headers.add("Linea de Producto");
        headers.add("Atributo Faltante");

        ReportSection section = new ReportSection(MISSING_ATTRIBUTES_SECTION, headers);
        boolean anyMissing = false;

        for (Map.Entry<String, List<ComparisonResult>> entry : resultsByProductLine.entrySet()) {
            String productLine = entry.getKey();
            for (ComparisonResult r : entry.getValue()) {
                for (ValidationError error : r.getErrors()) {
                    if (AttributeValidator.MISSING_REQUIRED_ATTRIBUTE.equals(error.getCode())) {
                        String attr = error.getAttributeName() != null ? error.getAttributeName() : "-";
                        section.addRow(row(r.getApplicationName(), productLine, attr));
                        anyMissing = true;
                    }
                }
            }
        }

        if (!anyMissing) {
            section.addRow(row("-", "-", "Sin atributos requeridos faltantes en el lote"));
        }
        return section;
    }

    /**
     * Construye la sección por línea de producto: requeridos satisfechos/totales
     * (en celdas), atributos opcionales de la línea, cuántos números de parte
     * tienen faltantes y una frase descriptiva de la auditoría.
     */
    private ReportSection buildProductLineStatsSection(
            Map<String, List<ComparisonResult>> resultsByProductLine) {
        List<String> headers = new ArrayList<>();
        headers.add("Linea de Producto");
        headers.add("Aplicaciones");
        headers.add("Requeridos (satisfechos/totales)");
        headers.add("Compliance %");
        headers.add("Atributos Opcionales");
        headers.add("Numeros de Parte con Faltantes");
        headers.add("Resumen");

        ReportSection section = new ReportSection(PRODUCT_LINE_STATS_SECTION, headers);

        Map<String, ProductLineSummary> summaries =
            complianceCalculator.summarizeByProductLine(resultsByProductLine);

        for (ProductLineSummary m : summaries.values()) {
            section.addRow(row(
                m.getProductLineName(),
                String.valueOf(m.getApplicationCount()),
                m.getRequiredFraction(),
                m.getComplianceLabel(),
                m.getOptionalAttributesText(),
                m.getPartNumbersWithGaps().size() + " de " + m.getPartNumberCount(),
                m.describe()
            ));
        }
        return section;
    }

    /**
     * Construye la sección con un renglón por número de parte al que le falta
     * algún atributo, indicando cuáles son requeridos y cuáles opcionales.
     */
    private ReportSection buildPartNumberGapsSection(
            Map<String, List<ComparisonResult>> resultsByProductLine) {
        List<String> headers = new ArrayList<>();
        headers.add("Linea de Producto");
        headers.add("Numero de Parte");
        headers.add("Requeridos Faltantes");
        headers.add("Opcionales Faltantes");

        ReportSection section = new ReportSection(PART_NUMBER_GAPS_SECTION, headers);
        boolean any = false;
        for (ProductLineSummary m : complianceCalculator.summarizeByProductLine(resultsByProductLine).values()) {
            for (Map.Entry<String, ProductLineSummary.PartNumberGap> e : m.getPartNumbersWithGaps().entrySet()) {
                section.addRow(row(
                    m.getProductLineName(),
                    e.getKey(),
                    e.getValue().getMissingRequired().isEmpty() ? "-" : String.join(", ", e.getValue().getMissingRequired()),
                    e.getValue().getMissingOptional().isEmpty() ? "-" : String.join(", ", e.getValue().getMissingOptional())
                ));
                any = true;
            }
        }
        if (!any) {
            section.addRow(row("-", "-", "-", "Todos los numeros de parte tienen sus atributos completos"));
        }
        return section;
    }

    /**
     * Indica si dos archivos son el mismo, aunque se escriban con rutas distintas
     * (por ejemplo {@code dir\.\a.xlsx} y {@code dir\a.xlsx}). Si ambos existen se
     * comparan en el sistema de archivos; si alguno no existe (todavía), por su
     * ruta absoluta normalizada. Sirve para impedir que un reporte se escriba
     * encima del ACES original.
     *
     * @return true si son el mismo archivo; false si no, o si alguno es null
     */
    public static boolean isSameFile(File a, File b) {
        if (a == null || b == null) return false;
        try {
            if (a.exists() && b.exists()) {
                return Files.isSameFile(a.toPath(), b.toPath());
            }
        } catch (IOException | SecurityException e) {
            // Si no se puede comparar en el sistema de archivos, usar la comparación por ruta
        }
        return a.toPath().toAbsolutePath().normalize()
                .equals(b.toPath().toAbsolutePath().normalize());
    }

    /**
     * Renderiza un {@link Report} a un archivo Excel (.xlsx) con formato
     * visual, en un archivo <strong>separado</strong> del ACES original.
     *
     * <p>El archivo de salida nunca debe ser el ACES original: este método
     * escribe un libro nuevo desde cero. Use {@link #defaultReportFileFor(File)}
     * para derivar una ruta hermana segura y {@link #isSameFile(File, File)} para
     * comprobarlo.</p>
     *
     * <p>La escritura es atómica: el libro se escribe primero a un archivo
     * temporal del mismo directorio y luego reemplaza al destino. Si algo falla,
     * un destino existente queda intacto y no se deja el temporal.</p>
     *
     * @param report     reporte a renderizar
     * @param outputFile archivo de salida (.xlsx), distinto del ACES original
     * @throws IllegalArgumentException si {@code report} u {@code outputFile} son null
     * @throws IOException              si ocurre un error de escritura
     */
    public void writeReportToFile(Report report, File outputFile) throws IOException {
        if (report == null) {
            throw new IllegalArgumentException("Report no puede ser null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile no puede ser null");
        }

        // Escritura atómica: se escribe a un temporal del MISMO directorio y solo al
        // terminar se mueve sobre el destino. Así, si el renderizado o la escritura
        // fallan, un archivo existente queda intacto en lugar de truncado, y no
        // queda un reporte a medias.
        Path target = outputFile.toPath().toAbsolutePath();
        Path tmp = Files.createTempFile(target.getParent(), "reporte_", ".tmp");
        boolean moved = false;
        try {
            try (OutputStream os = Files.newOutputStream(tmp)) {
                Workbook workbook = new Workbook(os, "Validador de Atributos ACES", "1.0");
                Worksheet ws = workbook.newWorksheet(REPORT_SHEET_NAME);

                int currentRow = renderTitle(ws, report);
                for (ReportSection section : report.getSections()) {
                    currentRow = renderSection(ws, section, currentRow);
                    currentRow++; // fila en blanco entre secciones
                }

                applyColumnWidths(ws);
                workbook.finish();
            }

            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                           StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(tmp);
            }
        }
    }

    /**
     * Genera y escribe en un solo paso el reporte de lote al archivo indicado.
     *
     * @param resultsByProductLine agrupamiento de resultados por línea de producto
     * @param outputFile           archivo de salida (.xlsx), distinto del ACES original
     * @throws IOException si ocurre un error de escritura
     */
    public void generateAndWriteBatch(
            Map<String, List<ComparisonResult>> resultsByProductLine, File outputFile) throws IOException {
        Report report = generateBatchReport(resultsByProductLine);
        writeReportToFile(report, outputFile);
    }

    /**
     * Deriva una ruta de archivo de reporte hermana del ACES original, con el
     * sufijo {@code _Reporte_Auditoria.xlsx}, para evitar sobrescribir el
     * archivo de entrada.
     *
     * @param acesFile archivo ACES original
     * @return archivo hermano sugerido para el reporte
     * @throws IllegalArgumentException si {@code acesFile} es null
     */
    public static File defaultReportFileFor(File acesFile) {
        if (acesFile == null) {
            throw new IllegalArgumentException("acesFile no puede ser null");
        }
        String name = acesFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        File parent = acesFile.getAbsoluteFile().getParentFile();
        return new File(parent, base + "_Reporte_Auditoria.xlsx");
    }

    // ── Renderizado Excel ─────────────────────────────────────────────────────

    private int renderTitle(Worksheet ws, Report report) {
        ws.value(0, 0, report.getTitle());
        ws.range(0, 0, 0, 4).style()
            .bold().fontSize(14).fontColor(Color.WHITE).fillColor(Color.NAVY_BLUE)
            .horizontalAlignment("left").set();

        ws.value(1, 0, "Generado: " + report.getGeneratedDate().format(DATE_FORMAT));
        ws.range(1, 0, 1, 4).style().italic().fontColor(Color.GRAY6).set();

        return 3; // deja una fila en blanco tras el encabezado del reporte
    }

    private int renderSection(Worksheet ws, ReportSection section, int startRow) {
        int r = startRow;
        int colCount = Math.max(1, section.getHeaders().size());

        // Título de sección
        ws.value(r, 0, section.getTitle());
        ws.range(r, 0, r, colCount - 1).style()
            .bold().fontSize(12).fontColor(Color.WHITE).fillColor(Color.TEAL)
            .horizontalAlignment("left").set();
        r++;

        // Encabezados de columna
        List<String> headers = section.getHeaders();
        if (!headers.isEmpty()) {
            for (int c = 0; c < headers.size(); c++) {
                ws.value(r, c, headers.get(c));
            }
            ws.range(r, 0, r, headers.size() - 1).style()
                .bold().fillColor(Color.GRAY3)
                .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY6)
                .horizontalAlignment("center").set();
            r++;
        }

        // Filas de datos
        for (List<String> dataRow : section.getRows()) {
            boolean isErrorRow = isErrorRow(section, dataRow);
            for (int c = 0; c < dataRow.size(); c++) {
                ws.value(r, c, dataRow.get(c));
            }
            int lastCol = Math.max(0, dataRow.size() - 1);
            if (isErrorRow) {
                ws.range(r, 0, r, lastCol).style()
                    .fontColor(Color.DARK_RED)
                    .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
            } else {
                ws.range(r, 0, r, lastCol).style()
                    .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
            }
            r++;
        }

        return r;
    }

    /**
     * Determina si una fila de datos corresponde a un error, para resaltarla.
     * Aplica solo a la sección de errores de un reporte individual, cuya
     * primera columna es la severidad.
     */
    private boolean isErrorRow(ReportSection section, List<String> dataRow) {
        if (dataRow.isEmpty()) {
            return false;
        }
        List<String> headers = section.getHeaders();
        if (!headers.isEmpty() && "Severidad".equals(headers.get(0))) {
            return "ERROR".equals(dataRow.get(0));
        }
        return false;
    }

    private void applyColumnWidths(Worksheet ws) {
        ws.width(0, 42.0);
        ws.width(1, 26.0);
        ws.width(2, 26.0);
        ws.width(3, 22.0);
        ws.width(4, 55.0);
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    private boolean isUnclassified(ComparisonResult result) {
        for (ValidationError warning : result.getWarnings()) {
            if (AttributeValidator.PRODUCT_LINE_NOT_FOUND.equals(warning.getCode())) {
                return true;
            }
        }
        return false;
    }

    private List<String> row(String... values) {
        List<String> r = new ArrayList<>(values.length);
        for (String v : values) {
            r.add(v);
        }
        return r;
    }
}