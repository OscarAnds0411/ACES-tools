package com.validador.aces.parsers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import com.validador.aces.models.Attribute;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;

/**
 * Parser concreto que construye un {@link Catalog} a partir del catálogo
 * maestro de atributos ACES almacenado en un archivo Excel (.xlsx).
 *
 * <p>Mapeo de columnas de la hoja {@code "Product Line ACES Attributes"}
 * (0-indexed, según el stream de filas):</p>
 * <ul>
 *   <li>Columna 0: Product ID → {@code ProductLine.id}</li>
 *   <li>Columna 1: Product Line → {@code ProductLine.name} (clave de vínculo
 *       con {@code Application.getProduct()})</li>
 *   <li>Columna 2: Category → {@code ProductLine.category}</li>
 *   <li>Columna 3: Sub Category → {@code ProductLine.subCategory}</li>
 *   <li>Columnas 4 en adelante: nombres de atributos ACES (encabezados en la
 *       fila 0), con valores de celda {@code "Required"} / {@code "Optional"}
 *       / {@code "Not Required"} en cada fila de datos, mapeados a
 *       {@link Attribute} mediante {@code AttributeRequirement.fromCellValue}.</li>
 * </ul>
 *
 * <p>El catálogo real contiene decenas de miles de filas, por lo que esta
 * implementación procesa las filas en modo streaming
 * ({@link Sheet#openStream()}) en lugar de cargar la hoja completa en
 * memoria con {@code Sheet.read()}.</p>
 */
public class ExcelCatalogParser extends CatalogParser {

    /** Nombre de la hoja del catálogo que contiene los datos a procesar. */
    private static final String SHEET_NAME = "Product Line ACES Attributes";

    /** Índice (0-based) de la primera columna de atributos. */
    private static final int FIRST_ATTRIBUTE_COLUMN = 4;

    @Override
    public Catalog parse(File file) throws ParseException {
        validateFileExists(file);

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Sheet sheet = findAttributesSheet(workbook, file);
            Catalog catalog = new Catalog(baseName(file), baseName(file));

            try (Stream<Row> rows = sheet.openStream()) {
                processRows(rows.iterator(), catalog);
            }

            return catalog;
        } catch (IOException e) {
            throw new ParseException("Error de E/S al leer el archivo de catálogo: " + file.getPath(), e);
        } catch (ParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ParseException("Error inesperado al procesar el catálogo Excel: " + file.getPath(), e);
        }
    }

    /**
     * Busca la hoja del catálogo que contiene los datos de atributos por
     * línea de producto.
     *
     * @param workbook libro de Excel ya abierto
     * @param file     archivo original (solo para mensajes de error)
     * @return hoja encontrada
     * @throws ParseException si la hoja {@value #SHEET_NAME} no existe
     */
    private Sheet findAttributesSheet(ReadableWorkbook workbook, File file) throws ParseException {
        Optional<Sheet> sheet = workbook.findSheet(SHEET_NAME);
        if (!sheet.isPresent()) {
            throw new ParseException(
                "El archivo de catálogo no contiene la hoja requerida \"" + SHEET_NAME + "\": " + file.getPath());
        }
        return sheet.get();
    }

    /**
     * Procesa el stream de filas de la hoja: la primera fila son los
     * encabezados de atributos y las siguientes son los datos de cada
     * línea de producto.
     *
     * @param rows    iterador sobre las filas de la hoja (streaming)
     * @param catalog catálogo donde se agregan las líneas de producto construidas
     */
    private void processRows(java.util.Iterator<Row> rows, Catalog catalog) {
        if (!rows.hasNext()) {
            return;
        }

        Row headerRow = rows.next();
        List<String> attributeHeaders = readAttributeHeaders(headerRow);

        while (rows.hasNext()) {
            Row row = rows.next();
            ProductLine productLine = buildProductLine(row, attributeHeaders);
            if (productLine != null) {
                catalog.addProductLine(productLine);
            }
        }
    }

    /**
     * Captura los nombres de columna de atributos a partir de la fila de
     * encabezados, comenzando en {@link #FIRST_ATTRIBUTE_COLUMN}.
     *
     * <p>Encabezados null o vacíos se omiten (se registra un aviso en
     * {@code System.err}), para no detener todo el parseo por una columna
     * mal formada.</p>
     */
    private List<String> readAttributeHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        int cellCount = headerRow.getCellCount();
        for (int i = FIRST_ATTRIBUTE_COLUMN; i < cellCount; i++) {
            String header = headerRow.getCellAsString(i).orElse(null);
            if (header == null || header.trim().isEmpty()) {
                System.err.println("ExcelCatalogParser: encabezado de atributo vacío en columna " + i + ", se omite.");
                continue;
            }
            headers.add(header);
        }
        return headers;
    }

    /**
     * Construye la {@link ProductLine} correspondiente a una fila de datos,
     * incluyendo sus atributos ACES.
     *
     * @return la {@code ProductLine} construida, o {@code null} si la fila
     *         debe omitirse (Product ID o Product Line vacíos)
     */
    private ProductLine buildProductLine(Row row, List<String> attributeHeaders) {
        String productId = row.getCellText(0);
        if (productId == null || productId.trim().isEmpty()) {
            return null;
        }

        String productLineName = row.getCellAsString(1).orElse(null);
        if (productLineName == null || productLineName.trim().isEmpty()) {
            System.err.println("ExcelCatalogParser: fila con Product ID '" + productId
                + "' omitida por no tener Product Line (nombre).");
            return null;
        }

        String category = row.getCellAsString(2).orElse(null);
        String subCategory = row.getCellAsString(3).orElse(null);

        ProductLine productLine = new ProductLine(productId, productLineName, category, subCategory, null);

        for (int i = 0; i < attributeHeaders.size(); i++) {
            String headerName = attributeHeaders.get(i);
            int columnIndex = FIRST_ATTRIBUTE_COLUMN + i;
            String cellValue = row.getCellAsString(columnIndex).orElse("Not Required");
            productLine.addAttribute(new Attribute(headerName, headerName, cellValue));
        }

        return productLine;
    }

    /**
     * Obtiene el nombre del archivo sin extensión, usado como id/name del
     * {@link Catalog} resultante.
     */
    private String baseName(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }
}
