package com.validador.aces.parsers;

import java.io.File;

import com.validador.aces.models.Catalog;

/**
 * Contrato base para los parsers del catálogo maestro de atributos ACES.
 *
 * <p>Un {@code CatalogParser} toma un archivo de entrada (Excel, XML, JSON,
 * etc.) y produce una instancia de {@link Catalog} completamente construida,
 * con sus {@code ProductLine} y {@code Attribute} correspondientes.</p>
 *
 * <p>Esta clase es solo el esqueleto/contrato: no implementa ninguna lógica
 * de lectura de archivos. Las implementaciones concretas (ej.
 * {@code ExcelCatalogParser}, que se implementa en una tarea posterior) deben
 * extender esta clase e implementar {@link #parse(File)}. Dichas
 * implementaciones deben invocar {@link #validateFileExists(File)} antes de
 * procesar el archivo, de modo que los errores de archivo inválido se
 * reporten de forma consistente mediante {@link ParseException}.</p>
 */
public abstract class CatalogParser {

    /**
     * Parsea el archivo indicado y construye el {@link Catalog} resultante.
     *
     * <p>Las implementaciones concretas deben llamar a
     * {@link #validateFileExists(File)} como primer paso antes de intentar
     * leer el contenido del archivo.</p>
     *
     * @param file archivo de catálogo a parsear (Excel, XML, JSON, etc.)
     * @return catálogo construido a partir del contenido del archivo
     * @throws ParseException si el archivo es inválido, no se puede leer, o
     *                        su contenido no tiene el formato esperado
     */
    public abstract Catalog parse(File file) throws ParseException;

    /**
     * Método de conveniencia que permite parsear un catálogo a partir de una
     * ruta de archivo en lugar de un objeto {@link File}.
     *
     * @param filePath ruta del archivo de catálogo a parsear; no puede ser
     *                 null ni estar vacía
     * @return catálogo construido a partir del contenido del archivo
     * @throws ParseException si {@code filePath} es null, está vacía, o si
     *                        ocurre un error durante el parsing del archivo
     */
    public Catalog parse(String filePath) throws ParseException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new ParseException("La ruta del archivo de catálogo no puede ser null ni vacía");
        }
        return parse(new File(filePath));
    }

    /**
     * Valida que el archivo exista, sea un archivo regular y se pueda leer.
     *
     * <p>Las implementaciones concretas de {@link #parse(File)} deben llamar
     * a este método antes de intentar procesar el contenido del archivo.</p>
     *
     * @param file archivo a validar
     * @throws ParseException si {@code file} es null, no existe, no es un
     *                        archivo regular, o no se puede leer
     */
    protected void validateFileExists(File file) throws ParseException {
        if (file == null) {
            throw new ParseException("El archivo de catálogo no puede ser null");
        }
        if (!file.exists()) {
            throw new ParseException("El archivo de catálogo no existe: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new ParseException("La ruta indicada no corresponde a un archivo regular: " + file.getPath());
        }
        if (!file.canRead()) {
            throw new ParseException("No se tienen permisos de lectura sobre el archivo: " + file.getPath());
        }
    }
}
