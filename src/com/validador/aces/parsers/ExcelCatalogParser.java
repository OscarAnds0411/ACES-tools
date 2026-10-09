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

import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;

/**
 * Parser concreto para el catálogo maestro de atributos ACES (.xlsx).
 *
 * <p>Mapeo de columnas de la hoja (0-indexed):</p>
 * <ul>
 *   <li>Col 0: Product ID &rarr; {@code ProductLine.id}</li>
 *   <li>Col 1: Product Line &rarr; {@code ProductLine.name}</li>
 *   <li>Col 2: Category &rarr; {@code ProductLine.category}</li>
 *   <li>Col 3: Sub Category &rarr; {@code ProductLine.subCategory}</li>
 *   <li>Col 4+: nombres de atributos con valores "Required"/"Optional"/"Not Required"</li>
 * </ul>
 */
public class ExcelCatalogParser extends CatalogParser {

    /**
     * Nombre por defecto de la hoja que contiene los datos del catálogo.
     * Se usa como selección automática en el combo de hojas de la GUI.
     */
    public static final String DEFAULT_SHEET_NAME = "Product Line ACES Attributes";

    /** Índice (0-based) de la primera columna de atributos. */
    private static final int FIRST_ATTRIBUTE_COLUMN = 4;

    // ── Métodos de utilidad estática ──────────────────────────────────────

    /**
     * Lee únicamente los nombres de las hojas del archivo Excel, sin parsear
     * su contenido. Útil para poblar el selector de hojas en la GUI.
     *
     * @param file archivo .xlsx a inspeccionar
     * @return lista de nombres de hojas en el orden en que aparecen en el libro
     * @throws ParseException si el archivo no existe, no es legible o está corrupto
     */
    public static List<String> readSheetNames(File file) throws ParseException {
        if (file == null || !file.exists()) {
            throw new ParseException(
                "El archivo no existe: " + (file != null ? file.getPath() : "null"));
        }
        try (ReadableWorkbook wb = new ReadableWorkbook(file)) {
            return wb.getSheets()
                     .map(Sheet::getName)
                     .collect(Collectors.toList());
        } catch (IOException e) {
            throw new ParseException(
                "No se pueden leer las hojas del archivo: " + file.getPath(), e);
        }
    }

    // ── Implementación de CatalogParser ───────────────────────────────────

    // ── Avisos de la última lectura ───────────────────────────────────────

    /** Máximo de avisos detallados que se conservan; el resto solo se cuenta. */
    private static final int MAX_WARNINGS = 50;

    private final List<String> warnings = new ArrayList<>();
    private int suppressedWarnings;

    /**
     * Avisos generados por la última llamada a {@code parse}: columnas sin
     * encabezado, filas omitidas y valores de requisito no reconocidos (que se
     * tratan como "Not Required"). Se reinicia en cada {@code parse}. Si hay más
     * de {@value #MAX_WARNINGS}, el último elemento resume cuántos se omitieron.
     *
     * @return lista inmutable, vacía si la lectura no tuvo incidencias
     */
    public List<String> getWarnings() {
        List<String> all = new ArrayList<>(warnings);
        if (suppressedWarnings > 0) {
            all.add("… y " + suppressedWarnings + " avisos más.");
        }
        return java.util.Collections.unmodifiableList(all);
    }

    private void addWarning(String message) {
        if (warnings.size() < MAX_WARNINGS) {
            warnings.add(message);
        } else {
            suppressedWarnings++;
        }
    }

    /**
     * Parsea el catálogo usando la hoja {@link #DEFAULT_SHEET_NAME}.
     */
    @Override
    public Catalog parse(File file) throws ParseException {
        return parse(file, DEFAULT_SHEET_NAME);
    }

    /**
     * Parsea el catálogo leyendo la hoja indicada por el usuario.
     * Si {@code sheetName} es null o vacío se usa {@link #DEFAULT_SHEET_NAME}.
     *
     * @param file      archivo .xlsx del catálogo
     * @param sheetName nombre exacto de la hoja a procesar
     * @return catálogo construido a partir del contenido de la hoja
     * @throws ParseException si el archivo, la hoja o el formato son inválidos
     */
    public Catalog parse(File file, String sheetName) throws ParseException {
        validateFileExists(file);
        warnings.clear();
        suppressedWarnings = 0;
        String target = (sheetName != null && !sheetName.trim().isEmpty())
                ? sheetName : DEFAULT_SHEET_NAME;

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Optional<Sheet> sheetOpt = workbook.findSheet(target);
            if (!sheetOpt.isPresent()) {
                throw new ParseException(
                    "El archivo no contiene la hoja \"" + target + "\": " + file.getPath());
            }

            Catalog catalog = new Catalog(baseName(file), baseName(file));
            try (Stream<Row> rows = sheetOpt.get().openStream()) {
                processRows(rows.iterator(), catalog);
            }
            return catalog;

        } catch (IOException e) {
            throw new ParseException(
                "Error de E/S al leer el archivo de catálogo: " + file.getPath(), e);
        } catch (ParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ParseException(
                "Error inesperado al procesar el catálogo Excel: " + file.getPath(), e);
        }
    }

    // ── Procesado de filas ─────────────────────────────────────────────────

    private void processRows(java.util.Iterator<Row> rows, Catalog catalog) {
        if (!rows.hasNext()) return;

        Row headerRow = rows.next();
        List<AttributeColumn> attributeColumns = readAttributeHeaders(headerRow);

        while (rows.hasNext()) {
            ProductLine pl = buildProductLine(rows.next(), attributeColumns);
            if (pl != null) catalog.addProductLine(pl);
        }
    }

    /**
     * Un atributo del catálogo: nombre del encabezado y su columna ORIGINAL en la
     * hoja. Se guarda la columna porque los encabezados vacíos se omiten y, si se
     * usara la posición en la lista, todas las columnas siguientes quedarían
     * desplazadas (cada atributo leería el requisito de otro).
     */
    private static final class AttributeColumn {
        final int index;
        final String name;

        AttributeColumn(int index, String name) {
            this.index = index;
            this.name = name;
        }
    }

    private List<AttributeColumn> readAttributeHeaders(Row headerRow) {
        List<AttributeColumn> columns = new ArrayList<>();
        int cellCount = headerRow.getCellCount();
        for (int i = FIRST_ATTRIBUTE_COLUMN; i < cellCount; i++) {
            String header = headerRow.getCellAsString(i).orElse(null);
            if (header == null || header.trim().isEmpty()) {
                System.err.println("ExcelCatalogParser: encabezado vacío en columna " + i + ", se omite.");
                addWarning("Encabezado vacío en la columna " + columnLetter(i)
                    + "; se omite esa columna.");
                continue;
            }
            columns.add(new AttributeColumn(i, header));
        }
        return columns;
    }

    /** Letra de columna estilo Excel para un índice 0-based (0 → A, 5 → F, 26 → AA). */
    private static String columnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) {
            sb.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return sb.toString();
    }

    private ProductLine buildProductLine(Row row, List<AttributeColumn> attributeColumns) {
        String productId = row.getCellText(0);
        if (productId == null || productId.trim().isEmpty()) return null;

        String name = row.getCellAsString(1).orElse(null);
        if (name == null || name.trim().isEmpty()) {
            System.err.println("ExcelCatalogParser: fila con ID '" + productId + "' sin nombre, omitida.");
            addWarning("Fila con ID '" + productId.trim() + "' sin nombre; se omite.");
            return null;
        }

        ProductLine pl = new ProductLine(
            productId, name,
            row.getCellAsString(2).orElse(null),
            row.getCellAsString(3).orElse(null),
            null
        );

        for (AttributeColumn column : attributeColumns) {
            String cellValue = row.getCellAsString(column.index).orElse("Not Required");
            if (!AttributeRequirement.isRecognized(cellValue)) {
                addWarning("Línea '" + name.trim() + "': valor '" + cellValue.trim()
                    + "' no reconocido en el atributo '" + column.name
                    + "' (se trata como Not Required).");
            }
            pl.addAttribute(new Attribute(column.name, column.name, cellValue));
        }
        return pl;
    }

    private String baseName(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}