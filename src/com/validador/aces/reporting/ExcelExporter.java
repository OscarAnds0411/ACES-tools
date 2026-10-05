package com.validador.aces.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.dhatim.fastexcel.BorderStyle;
import org.dhatim.fastexcel.Color;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.validador.aces.models.Application;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;

/**
 * Exporta el resultado de auditoría de una única {@link Application} a un
 * archivo Excel (.xlsx) compacto.
 *
 * <p>A diferencia de {@link ExcelReportGenerator}, que trabaja con lotes
 * agrupados por línea de producto, este exportador produce un archivo
 * orientado a mostrar el detalle de <strong>una sola aplicación</strong>:
 * sus metadatos, el compliance alcanzado y el listado de errores y
 * advertencias.</p>
 *
 * <h2>Sobrescritura de archivo</h2>
 * <p>Si el archivo de salida ya existe en disco, <strong>se sobrescribe</strong>
 * sin confirmación adicional. El llamador es responsable de gestionar
 * colisiones de nombre si lo considera necesario.</p>
 */
public class ExcelExporter {

    /** Nombre de la única hoja del libro exportado. */
    public static final String SHEET_NAME = "Resultado Auditoria";

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exporta el resultado de comparación de una aplicación a un archivo Excel.
     *
     * <p>El archivo producido contiene una hoja única ({@value #SHEET_NAME})
     * con la siguiente estructura:</p>
     * <ol>
     *   <li><b>Bloque de metadata</b> (filas 0–4): Aplicación, Vehículo
     *       (Make/Model/Year), Catálogo, Fecha de exportación, Compliance %.</li>
     *   <li><b>Tabla de problemas</b>: encabezados Severidad | Código | Atributo |
     *       Línea de Producto | Mensaje, seguidos de una fila por cada error y
     *       advertencia. Si no hay problemas, se muestra una fila informativa.</li>
     * </ol>
     *
     * @param application aplicación que fue auditada; no puede ser null
     * @param result      resultado de la comparación; no puede ser null
     * @param outputFile  archivo de destino (.xlsx); se crea o sobrescribe; no puede ser null
     * @throws IllegalArgumentException si algún parámetro es null
     * @throws IOException              si ocurre un error al escribir el archivo
     */
    public void export(Application application, ComparisonResult result, File outputFile)
            throws IOException {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile no puede ser null");
        }

        try (OutputStream os = new FileOutputStream(outputFile)) {
            Workbook wb = new Workbook(os, "Validador de Atributos ACES", "1.0");
            Worksheet ws = wb.newWorksheet(SHEET_NAME);

            int row = writeMetadata(ws, application, result);
            row++; // fila en blanco
            writeProblemsTable(ws, result, row);

            applyColumnWidths(ws);
            wb.finish();
        }
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    /**
     * Escribe el bloque de metadata y retorna el índice de la siguiente fila libre.
     */
    private int writeMetadata(Worksheet ws, Application application, ComparisonResult result) {
        // Título
        ws.value(0, 0, "Resultado de Auditoría ACES");
        ws.range(0, 0, 0, 4).style()
            .bold().fontSize(13).fontColor(Color.WHITE).fillColor(Color.NAVY_BLUE).set();

        String vehicle = buildVehicleLabel(application);

        ws.value(1, 0, "Aplicación");   ws.value(1, 1, result.getApplicationName());
        ws.value(2, 0, "Vehículo");     ws.value(2, 1, vehicle);
        ws.value(3, 0, "Catálogo");     ws.value(3, 1, result.getCatalogName());
        ws.value(4, 0, "Fecha");        ws.value(4, 1, LocalDateTime.now().format(DATE_FORMAT));
        ws.value(5, 0, "Compliance");   ws.value(5, 1,
            String.format("%.1f%%", result.getCompliancePercentage()));

        // Estilo de etiquetas de metadata
        for (int r = 1; r <= 5; r++) {
            ws.range(r, 0, r, 0).style().bold().fillColor(Color.GRAY2).set();
            ws.range(r, 1, r, 4).style()
                .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
        }

        return 6;
    }

    /**
     * Escribe la tabla de problemas (errores y advertencias) a partir de {@code startRow}.
     */
    private void writeProblemsTable(Worksheet ws, ComparisonResult result, int startRow) {
        int r = startRow;

        // Título de sección
        ws.value(r, 0, "Detalle de Problemas");
        ws.range(r, 0, r, 4).style()
            .bold().fontSize(11).fontColor(Color.WHITE).fillColor(Color.TEAL).set();
        r++;

        // Encabezados
        ws.value(r, 0, "Severidad");
        ws.value(r, 1, "Código");
        ws.value(r, 2, "Atributo");
        ws.value(r, 3, "Línea de Producto");
        ws.value(r, 4, "Mensaje");
        ws.range(r, 0, r, 4).style()
            .bold().fillColor(Color.GRAY3)
            .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY6)
            .horizontalAlignment("center").set();
        r++;

        // Filas de datos
        List<ValidationError> combined = new java.util.ArrayList<>();
        combined.addAll(result.getErrors());
        combined.addAll(result.getWarnings());

        if (combined.isEmpty()) {
            ws.value(r, 0, "INFO");
            ws.value(r, 1, "-");
            ws.value(r, 2, "-");
            ws.value(r, 3, "-");
            ws.value(r, 4, "Sin problemas detectados — todos los atributos requeridos presentes");
            ws.range(r, 0, r, 4).style()
                .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
        } else {
            for (ValidationError e : combined) {
                ws.value(r, 0, safe(e.getSeverity() != null ? e.getSeverity().toString() : null));
                ws.value(r, 1, safe(e.getCode()));
                ws.value(r, 2, safe(e.getAttributeName()));
                ws.value(r, 3, safe(e.getProductLine()));
                ws.value(r, 4, safe(e.getMessage()));

                if (e.isError()) {
                    ws.range(r, 0, r, 4).style()
                        .fontColor(Color.DARK_RED)
                        .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
                } else {
                    ws.range(r, 0, r, 4).style()
                        .borderStyle(BorderStyle.THIN).borderColor(Color.GRAY5).set();
                }
                r++;
            }
        }
    }

    private void applyColumnWidths(Worksheet ws) {
        ws.width(0, 20.0);
        ws.width(1, 32.0);
        ws.width(2, 26.0);
        ws.width(3, 28.0);
        ws.width(4, 55.0);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private String buildVehicleLabel(Application app) {
        String make  = app.getMake()  != null ? app.getMake()  : "";
        String model = app.getModel() != null ? app.getModel() : "";
        String year  = app.getYear()  != null ? app.getYear()  : "";
        String label = (make + " " + model + " " + year).trim();
        return label.isEmpty() ? app.getName() : label;
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }
}