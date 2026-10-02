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

import com.validador.aces.models.Application;

/**
 * Parser concreto que construye la lista de {@link Application} a partir
 * del archivo de aplicaciones ACES almacenado en un archivo Excel (.xlsx).
 *
 * <p>Mapeo de columnas de la hoja {@code "Applications"} (0-indexed, según
 * el stream de filas):</p>
 * <ul>
 *   <li>Columna 0: Make → {@code Application.make}</li>
 *   <li>Columna 1: Model → {@code Application.model}</li>
 *   <li>Columna 2: Year → {@code Application.year}</li>
 *   <li>Columna 3: Product → {@code Application.product} (clave de vínculo
 *       con {@code ProductLine.name} del catálogo)</li>
 *   <li>Columna 4: PartNumber → {@code Application.partNumber}</li>
 *   <li>Columna 5: MfrLabel → {@code Application.mfrLabel}</li>
 *   <li>Columna 6: Position → {@code Application.position}</li>
 *   <li>Columnas 7 en adelante: nombres de atributos técnicos (encabezados
 *       en la fila 0), con los valores reales de cada aplicación mapeados
 *       mediante {@code Application.setAttributeValue(headerName, value)}.</li>
 * </ul>
 *
 * <p>El archivo ACES real contiene decenas de miles de filas (81,000+), por
 * lo que esta implementación procesa las filas en modo streaming
 * ({@link Sheet#openStream()}) en lugar de cargar la hoja completa en
 * memoria.</p>
 *
 * <p>A diferencia de {@code ExcelCatalogParser}, los valores de celda vacíos
 * de los atributos técnicos NO se sustituyen por un valor por defecto: se
 * preservan como {@code null} en el mapa de atributos de la aplicación, ya
 * que la ausencia de valor es información relevante para detectar atributos
 * requeridos faltantes en comparaciones posteriores contra el catálogo.</p>
 */
public class ExcelApplicationParser extends ApplicationParser {

    /** Nombre de la hoja de aplicaciones que contiene los datos a procesar. */
    private static final String SHEET_NAME = "Applications";

    /** Índice (0-based) de la primera columna de atributos técnicos. */
    private static final int FIRST_ATTRIBUTE_COLUMN = 7;

    @Override
    public List<Application> parse(File file) throws ParseException {
        validateFileExists(file);

        try (ReadableWorkbook workbook = new ReadableWorkbook(file)) {
            Sheet sheet = findApplicationsSheet(workbook, file);
            List<Application> applications = new ArrayList<>();

            try (Stream<Row> rows = sheet.openStream()) {
                processRows(rows.iterator(), applications);
            }

            return applications;
        } catch (IOException e) {
            throw new ParseException("Error de E/S al leer el archivo de aplicaciones: " + file.getPath(), e);
        } catch (ParseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ParseException("Error inesperado al procesar el archivo de aplicaciones Excel: " + file.getPath(), e);
        }
    }

    /**
     * Busca la hoja que contiene los datos de aplicaciones.
     *
     * @param workbook libro de Excel ya abierto
     * @param file     archivo original (solo para mensajes de error)
     * @return hoja encontrada
     * @throws ParseException si la hoja {@value #SHEET_NAME} no existe
     */
    private Sheet findApplicationsSheet(ReadableWorkbook workbook, File file) throws ParseException {
        Optional<Sheet> sheet = workbook.findSheet(SHEET_NAME);
        if (!sheet.isPresent()) {
            throw new ParseException(
                "El archivo de aplicaciones no contiene la hoja requerida \"" + SHEET_NAME + "\": " + file.getPath());
        }
        return sheet.get();
    }

    /**
     * Procesa el stream de filas de la hoja: la primera fila son los
     * encabezados de atributos técnicos y las siguientes son los datos de
     * cada aplicación (vehículo).
     *
     * @param rows         iterador sobre las filas de la hoja (streaming)
     * @param applications lista donde se agregan las aplicaciones construidas
     */
    private void processRows(java.util.Iterator<Row> rows, List<Application> applications) {
        if (!rows.hasNext()) {
            return;
        }

        Row headerRow = rows.next();
        List<String> attributeHeaders = readAttributeHeaders(headerRow);

        int rowNumber = 1;
        while (rows.hasNext()) {
            rowNumber++;
            Row row = rows.next();
            Application application = buildApplication(row, attributeHeaders, rowNumber);
            if (application != null) {
                applications.add(application);
            }
        }
    }

    /**
     * Captura los nombres de columna de atributos técnicos a partir de la
     * fila de encabezados, comenzando en {@link #FIRST_ATTRIBUTE_COLUMN}.
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
                System.err.println("ExcelApplicationParser: encabezado de atributo vacío en columna " + i + ", se omite.");
                continue;
            }
            headers.add(header);
        }
        return headers;
    }

    /**
     * Construye la {@link Application} correspondiente a una fila de datos,
     * incluyendo sus atributos técnicos.
     *
     * @param row              fila de datos a procesar
     * @param attributeHeaders encabezados de atributos técnicos capturados previamente
     * @param rowNumber        número de fila (1-based, incluyendo encabezado) usado solo para mensajes de aviso
     * @return la {@code Application} construida, o {@code null} si la fila debe omitirse (Make vacío)
     */
    private Application buildApplication(Row row, List<String> attributeHeaders, int rowNumber) {
        String make = row.getCellText(0);
        if (make == null || make.trim().isEmpty()) {
            System.err.println("ExcelApplicationParser: fila " + rowNumber + " omitida por no tener Make.");
            return null;
        }

        String model = row.getCellAsString(1).orElse(null);
        String year = row.getCellAsString(2).orElse(null);
        String product = row.getCellAsString(3).orElse(null);

        Application application = new Application(make, model, year, product);

        application.setPartNumber(row.getCellAsString(4).orElse(null));
        application.setMfrLabel(row.getCellAsString(5).orElse(null));
        application.setPosition(row.getCellAsString(6).orElse(null));

        for (int i = 0; i < attributeHeaders.size(); i++) {
            String headerName = attributeHeaders.get(i);
            int columnIndex = FIRST_ATTRIBUTE_COLUMN + i;
            String cellValue = row.getCellAsString(columnIndex).orElse(null);
            application.setAttributeValue(headerName, cellValue);
        }

        return application;
    }
}
