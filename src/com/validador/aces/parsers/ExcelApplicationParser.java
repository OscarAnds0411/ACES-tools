package com.validador.aces.parsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import com.validador.aces.models.Application;

/**
 * Parser concreto para el archivo ACES de aplicaciones (.xlsx).
 *
 * <p>Mapeo de columnas de la hoja (0-indexed):</p>
 * <ul>
 *   <li>Col 0: Make, Col 1: Model, Col 2: Year, Col 3: Product</li>
 *   <li>Col 4: PartNumber, Col 5: MfrLabel, Col 6: Position</li>
 *   <li>Col 7+: atributos técnicos con valores reales (null si vacío)</li>
 * </ul>
 */
public class ExcelApplicationParser extends ApplicationParser {

    /**
     * Nombre por defecto de la hoja que contiene las aplicaciones.
     * Se usa como selección automática en el combo de hojas de la GUI.
     */
    public static final String DEFAULT_SHEET_NAME = "Applications";

    private static final int FIRST_ATTRIBUTE_COLUMN = 7;

    // ── Utilidad estática ─────────────────────────────────────────────────

    /**
     * Lee únicamente los nombres de las hojas del archivo Excel sin parsear
     * su contenido. Útil para poblar el selector de hojas en la GUI.
     *
     * @param file archivo .xlsx a inspeccionar
     * @return lista de nombres de hojas en orden de aparición
     * @throws ParseException si el archivo no existe o no puede leerse
     */
    public static List<String> readSheetNames(File file) throws ParseException {
        if (file == null || !file.exists()) {
            throw new ParseException(
                "El archivo no existe: " + (file != null ? file.getPath() : "null"));
        }
        try (ReadableWorkbook wb = new ReadableWorkbook(file)) {
            return wb.getSheets().map(Sheet::getName).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ParseException(
                "No se pueden leer las hojas del archivo: " + file.getPath(), e);
        }
    }

    // ── Implementación de ApplicationParser ───────────────────────────────

    /** Parsea usando la hoja {@link #DEFAULT_SHEET_NAME}. */
    @Override
    public List<Application> parse(File file) throws ParseException {
        return parse(file, DEFAULT_SHEET_NAME);
    }

    /**
     * Parsea las aplicaciones de la hoja indicada por el usuario.
     * Si {@code sheetName} es null o vacío usa {@link #DEFAULT_SHEET_NAME}.
     *
     * @param file      archivo .xlsx del ACES
     * @param sheetName nombre exacto de la hoja a procesar
     * @return lista de aplicaciones construidas
     * @throws ParseException si el archivo, la hoja o el formato son inválidos
     */
    public List<Application> parse(File file, String sheetName) throws ParseException {
        validateFileExists(file);
        String target = (sheetName != null && !sheetName.trim().isEmpty())
                ? sheetName : DEFAULT_SHEET_NAME;

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Optional<Sheet> sheetOpt = workbook.findSheet(target);
            if (!sheetOpt.isPresent()) {
                throw new ParseException(
                    "El archivo no contiene la hoja \"" + target + "\": " + file.getPath());
            }
            List<Application> applications = new ArrayList<>();
            try (Stream<Row> rows = sheetOpt.get().openStream()) {
                processRows(rows.iterator(), applications);
            }
            return applications;
        } catch (IOException e) {
            throw new ParseException(
                "Error de E/S al leer el archivo de aplicaciones: " + file.getPath(), e);
        } catch (ParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ParseException(
                "Error inesperado al procesar el archivo de aplicaciones: " + file.getPath(), e);
        }
    }

    // ── Procesado de filas ─────────────────────────────────────────────────

    private void processRows(java.util.Iterator<Row> rows, List<Application> applications) {
        if (!rows.hasNext()) return;
        Row headerRow = rows.next();
        List<String> attributeHeaders = readAttributeHeaders(headerRow);
        int rowNumber = 1;
        while (rows.hasNext()) {
            rowNumber++;
            Application app = buildApplication(rows.next(), attributeHeaders, rowNumber);
            if (app != null) applications.add(app);
        }
    }

    private List<String> readAttributeHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        int cellCount = headerRow.getCellCount();
        for (int i = FIRST_ATTRIBUTE_COLUMN; i < cellCount; i++) {
            String header = headerRow.getCellAsString(i).orElse(null);
            if (header == null || header.trim().isEmpty()) {
                System.err.println("ExcelApplicationParser: encabezado vacío en col " + i + ", se omite.");
                continue;
            }
            headers.add(header);
        }
        return headers;
    }

    private Application buildApplication(Row row, List<String> attributeHeaders, int rowNum) {
        String make = row.getCellText(0);
        if (make == null || make.trim().isEmpty()) {
            System.err.println("ExcelApplicationParser: fila " + rowNum + " omitida (Make vacío).");
            return null;
        }
        Application app = new Application(
            make,
            row.getCellAsString(1).orElse(null),
            row.getCellAsString(2).orElse(null),
            row.getCellAsString(3).orElse(null)
        );
        app.setPartNumber(row.getCellAsString(4).orElse(null));
        app.setMfrLabel(row.getCellAsString(5).orElse(null));
        app.setPosition(row.getCellAsString(6).orElse(null));
        for (int i = 0; i < attributeHeaders.size(); i++) {
            app.setAttributeValue(attributeHeaders.get(i),
                row.getCellAsString(FIRST_ATTRIBUTE_COLUMN + i).orElse(null));
        }
        return app;
    }
}