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
     * Si {@code sheetName} es null o vacío, intenta detectar automáticamente
     * la hoja que contiene aplicaciones (con datos en las 7 primeras columnas).
     *
     * @param file      archivo .xlsx del ACES
     * @param sheetName nombre exacto de la hoja a procesar, o null para auto-detectar
     * @return lista de aplicaciones construidas
     * @throws ParseException si el archivo, la hoja o el formato son inválidos
     */
    public List<Application> parse(File file, String sheetName) throws ParseException {
        validateFileExists(file);

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            // Determinar qué hoja procesar
            String targetSheet = sheetName;
            if (targetSheet == null || targetSheet.trim().isEmpty()) {
                // Auto-detectar: buscar primera hoja que tenga datos válidos de aplicaciones
                targetSheet = autoDetectApplicationSheet(workbook);
                if (targetSheet == null) {
                    throw new ParseException(
                        "No se pudo detectar automáticamente una hoja válida con aplicaciones: "
                        + file.getPath());
                }
            }

            Optional<Sheet> sheetOpt = workbook.findSheet(targetSheet);
            if (!sheetOpt.isPresent()) {
                throw new ParseException(
                    "El archivo no contiene la hoja \"" + targetSheet + "\": " + file.getPath());
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

    /**
     * Intenta detectar automáticamente cuál es la hoja que contiene aplicaciones.
     * Busca la primera hoja que tenga datos con estructura válida (Make, Model, Year, Product).
     *
     * @param workbook libro Excel abierto
     * @return nombre de la hoja detectada, o null si ninguna es válida
     */
    private String autoDetectApplicationSheet(ReadableWorkbook workbook) {
        try {
            for (Sheet sheet : (Iterable<Sheet>) () -> workbook.getSheets().iterator()) {
                try (Stream<Row> rows = sheet.openStream()) {
                    if (isValidApplicationSheet(rows.iterator())) {
                        return sheet.getName();
                    }
                }
            }
        } catch (Exception e) {
            // Si hay error durante la detección, retornar null y dejar que falle en parse()
        }
        return null;
    }

    /**
     * Verifica si una hoja tiene estructura válida de aplicaciones.
     * Válida si: tiene encabezados y al menos una fila con Make, Model, Year, Product no vacíos.
     *
     * @param rows iterador de filas de la hoja
     * @return true si la hoja parece tener datos de aplicaciones válidos
     */
    private boolean isValidApplicationSheet(java.util.Iterator<Row> rows) {
        if (!rows.hasNext()) return false;

        // Saltar encabezados
        rows.next();

        // Buscar al menos una fila válida con Make, Model, Year, Product
        int checked = 0;
        int maxToCheck = 100; // revisar hasta 100 filas para no tardar mucho
        while (rows.hasNext() && checked < maxToCheck) {
            Row row = rows.next();
            checked++;

            String make = row.getCellText(0);
            String model = row.getCellText(1);
            String year = row.getCellText(2);
            String product = row.getCellText(3);

            // Si encuentro una fila con estos 4 campos no vacíos, asumimos que es una hoja válida
            if (make != null && !make.trim().isEmpty() &&
                model != null && !model.trim().isEmpty() &&
                year != null && !year.trim().isEmpty() &&
                product != null && !product.trim().isEmpty()) {
                return true;
            }
        }

        return false;
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
            row.getCellText(1),    // usar getCellText en lugar de getCellAsString
            row.getCellText(2),    // funciona tanto para STRING como para NUMBER
            row.getCellText(3)
        );
        app.setPartNumber(row.getCellText(4));
        app.setMfrLabel(row.getCellText(5));
        app.setPosition(row.getCellText(6));
        for (int i = 0; i < attributeHeaders.size(); i++) {
            app.setAttributeValue(attributeHeaders.get(i),
                row.getCellText(FIRST_ATTRIBUTE_COLUMN + i));
        }
        return app;
    }
}