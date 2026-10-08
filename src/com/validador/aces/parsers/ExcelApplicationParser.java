package com.validador.aces.parsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import com.validador.aces.models.Application;

/**
 * Parser concreto para el archivo ACES de aplicaciones (.xlsx).
 *
 * <p>Las columnas se localizan por el nombre de su encabezado (sin distinguir
 * mayúsculas), no por posición:</p>
 * <ul>
 *   <li>Obligatorias: Make, Model, Year, Product</li>
 *   <li>Opcionales: PartNumber, MfrLabel, Position</li>
 *   <li>Cualquier otra columna es un atributo técnico (null si la celda está vacía).
 *       Si se indican "encabezados de interés" (los atributos del catálogo), solo se
 *       leen esas columnas y el resto se ignora.</li>
 * </ul>
 * <p>Solo se procesa la hoja indicada; las demás hojas del libro se ignoran.</p>
 */
public class ExcelApplicationParser extends ApplicationParser {

    /**
     * Nombre por defecto de la hoja que contiene las aplicaciones.
     * Se usa como selección automática en el combo de hojas de la GUI.
     */
    public static final String DEFAULT_SHEET_NAME = "Applications";

    private static final String[] REQUIRED_HEADERS = {"Make", "Model", "Year", "Product"};
    private static final String[] OPTIONAL_CORE_HEADERS = {"PartNumber", "MfrLabel", "Position"};

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

    /** Parsea usando la hoja {@link #DEFAULT_SHEET_NAME} y todas las columnas de atributos. */
    @Override
    public List<Application> parse(File file) throws ParseException {
        return parse(file, DEFAULT_SHEET_NAME, null);
    }

    /** Parsea la hoja indicada leyendo todas las columnas de atributos. */
    public List<Application> parse(File file, String sheetName) throws ParseException {
        return parse(file, sheetName, null);
    }

    /**
     * Parsea únicamente la hoja indicada y solo los encabezados de interés.
     *
     * @param file              archivo .xlsx del ACES
     * @param sheetName         hoja a procesar, o null/vacío para auto-detectar
     * @param headersOfInterest nombres de atributos a leer (normalmente los del catálogo);
     *                          las columnas que no estén aquí se ignoran. Si es null se
     *                          leen todas las columnas de atributos.
     * @throws ParseException si el archivo, la hoja o el encabezado son inválidos
     */
    public List<Application> parse(File file, String sheetName, Set<String> headersOfInterest)
            throws ParseException {
        validateFileExists(file);

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            String targetSheet = sheetName;
            if (targetSheet == null || targetSheet.trim().isEmpty()) {
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
                processRows(rows.iterator(), applications, headersOfInterest);
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

    /** Devuelve la primera hoja cuyo encabezado contiene las columnas obligatorias, o null. */
    private String autoDetectApplicationSheet(ReadableWorkbook workbook) {
        try {
            for (Sheet sheet : (Iterable<Sheet>) () -> workbook.getSheets().iterator()) {
                try (Stream<Row> rows = sheet.openStream()) {
                    java.util.Iterator<Row> it = rows.iterator();
                    if (it.hasNext() && missingRequired(readHeaderColumns(it.next())) == null) {
                        return sheet.getName();
                    }
                }
            }
        } catch (Exception e) {
            // Si hay error durante la detección, retornar null y dejar que falle en parse()
        }
        return null;
    }

    // ── Encabezados ────────────────────────────────────────────────────────

    /** Columnas de una hoja resueltas por nombre de encabezado. */
    private static final class HeaderColumns {
        /** Nombre núcleo en minúsculas (make, model, ...) → índice de columna. */
        final Map<String, Integer> core = new HashMap<>();
        /** Nombre de atributo → índice de columna (en orden de aparición). */
        final Map<String, Integer> attributes = new LinkedHashMap<>();
    }

    /** Clasifica los encabezados en columnas núcleo y de atributos (la primera aparición gana). */
    private static HeaderColumns readHeaderColumns(Row headerRow) {
        HeaderColumns result = new HeaderColumns();
        for (int i = 0; i < headerRow.getCellCount(); i++) {
            String header = headerRow.getCellText(i);
            if (header == null || header.trim().isEmpty()) continue;
            header = header.trim();
            String key = header.toLowerCase();
            if (isCoreHeader(key)) {
                result.core.putIfAbsent(key, i);
            } else {
                result.attributes.putIfAbsent(header, i);
            }
        }
        return result;
    }

    private static boolean isCoreHeader(String lowerKey) {
        for (String h : REQUIRED_HEADERS) if (h.equalsIgnoreCase(lowerKey)) return true;
        for (String h : OPTIONAL_CORE_HEADERS) if (h.equalsIgnoreCase(lowerKey)) return true;
        return false;
    }

    /** @return nombre de la primera columna obligatoria que falta, o null si están todas */
    private static String missingRequired(HeaderColumns columns) {
        for (String h : REQUIRED_HEADERS) {
            if (!columns.core.containsKey(h.toLowerCase())) return h;
        }
        return null;
    }

    // ── Procesado de filas ─────────────────────────────────────────────────

    private void processRows(java.util.Iterator<Row> rows, List<Application> applications,
                             Set<String> headersOfInterest) throws ParseException {
        if (!rows.hasNext()) return;
        HeaderColumns columns = readHeaderColumns(rows.next());
        String missing = missingRequired(columns);
        if (missing != null) {
            throw new ParseException(
                "La hoja no tiene el formato ACES esperado: falta el encabezado \"" + missing
                + "\" (se requieren Make, Model, Year y Product).");
        }

        // Columnas de atributos a leer: nombre canónico (el del catálogo) → índice
        Map<String, Integer> attributeColumns = selectAttributeColumns(columns, headersOfInterest);

        int skipped = 0;
        while (rows.hasNext()) {
            Application app = buildApplication(rows.next(), columns, attributeColumns);
            if (app != null) applications.add(app); else skipped++;
        }
        if (skipped > 0) {
            System.err.println("ExcelApplicationParser: " + skipped + " filas omitidas (Make vacío).");
        }
    }

    private static Map<String, Integer> selectAttributeColumns(HeaderColumns columns,
                                                               Set<String> headersOfInterest) {
        if (headersOfInterest == null) return columns.attributes;
        Map<String, String> canonicalByLower = new HashMap<>();
        for (String name : headersOfInterest) {
            if (name != null) canonicalByLower.putIfAbsent(name.trim().toLowerCase(), name);
        }
        Map<String, Integer> selected = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : columns.attributes.entrySet()) {
            String canonical = canonicalByLower.get(e.getKey().toLowerCase());
            if (canonical != null) selected.put(canonical, e.getValue());
        }
        return selected;
    }

    private Application buildApplication(Row row, HeaderColumns columns,
                                         Map<String, Integer> attributeColumns) {
        String make = cell(row, columns.core.get("make"));
        if (make == null) return null;
        Application app = new Application(
            make,
            cell(row, columns.core.get("model")),
            cell(row, columns.core.get("year")),
            cell(row, columns.core.get("product"))
        );
        app.setPartNumber(cell(row, columns.core.get("partnumber")));
        app.setMfrLabel(cell(row, columns.core.get("mfrlabel")));
        app.setPosition(cell(row, columns.core.get("position")));
        for (Map.Entry<String, Integer> e : attributeColumns.entrySet()) {
            app.setAttributeValue(e.getKey(), cell(row, e.getValue()));
        }
        return app;
    }

    /** Texto de la celda, o null si la columna no existe o la celda está vacía. */
    private static String cell(Row row, Integer column) {
        if (column == null) return null;
        String value = row.getCellText(column);
        return (value == null || value.trim().isEmpty()) ? null : value;
    }
}
