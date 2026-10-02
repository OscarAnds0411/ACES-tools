package com.validador.aces.parsers;

import java.io.File;
import java.util.List;

import com.validador.aces.models.Application;

/**
 * Contrato base para los parsers de archivos de aplicaciones ACES.
 *
 * <p>Un {@code ApplicationParser} toma un archivo de entrada (Excel, XML,
 * JSON, etc.) y produce una lista de {@link Application}, dado que un
 * archivo ACES contiene miles de filas, cada una representando una
 * aplicación (vehículo) distinta — a diferencia de {@link CatalogParser},
 * que produce una única instancia de catálogo por archivo.</p>
 *
 * <p>Esta clase es solo el esqueleto/contrato: no implementa ninguna lógica
 * de lectura de archivos. Las implementaciones concretas (ej.
 * {@code ExcelApplicationParser}, que se implementa en una tarea posterior)
 * deben extender esta clase e implementar {@link #parse(File)}. Dichas
 * implementaciones deben invocar {@link #validateFileExists(File)} antes de
 * procesar el archivo, de modo que los errores de archivo inválido se
 * reporten de forma consistente mediante {@link ParseException}.</p>
 */
public abstract class ApplicationParser {

    /**
     * Parsea el archivo indicado y construye la lista de {@link Application}
     * resultante.
     *
     * <p>Las implementaciones concretas deben llamar a
     * {@link #validateFileExists(File)} como primer paso antes de intentar
     * leer el contenido del archivo.</p>
     *
     * @param file archivo de aplicaciones ACES a parsear (Excel, XML, JSON, etc.)
     * @return lista de aplicaciones construidas a partir del contenido del archivo
     * @throws ParseException si el archivo es inválido, no se puede leer, o
     *                        su contenido no tiene el formato esperado
     */
    public abstract List<Application> parse(File file) throws ParseException;

    /**
     * Método de conveniencia que permite parsear aplicaciones a partir de
     * una ruta de archivo en lugar de un objeto {@link File}.
     *
     * @param filePath ruta del archivo de aplicaciones a parsear; no puede
     *                 ser null ni estar vacía
     * @return lista de aplicaciones construidas a partir del contenido del archivo
     * @throws ParseException si {@code filePath} es null, está vacía, o si
     *                        ocurre un error durante el parsing del archivo
     */
    public List<Application> parse(String filePath) throws ParseException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new ParseException("La ruta del archivo de aplicaciones no puede ser null ni vacía");
        }
        return parse(new File(filePath));
    }

    /**
     * Valida que el archivo exista, sea un archivo regular y se pueda leer.
     *
     * <p>Las implementaciones concretas de {@link #parse(File)} deben llamar
     * a este método antes de intentar procesar el contenido del archivo.</p>
     *
     * <p>Esta lógica se duplica respecto a {@code CatalogParser.validateFileExists}
     * intencionalmente, para mantener ambas jerarquías de parsers
     * independientes entre sí.</p>
     *
     * @param file archivo a validar
     * @throws ParseException si {@code file} es null, no existe, no es un
     *                        archivo regular, o no se puede leer
     */
    protected void validateFileExists(File file) throws ParseException {
        if (file == null) {
            throw new ParseException("El archivo de aplicaciones no puede ser null");
        }
        if (!file.exists()) {
            throw new ParseException("El archivo de aplicaciones no existe: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new ParseException("La ruta indicada no corresponde a un archivo regular: " + file.getPath());
        }
        if (!file.canRead()) {
            throw new ParseException("No se tienen permisos de lectura sobre el archivo: " + file.getPath());
        }
    }
}
