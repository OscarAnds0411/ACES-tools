package com.validador.aces.parsers;

/**
 * Excepción checked que representa un error ocurrido durante el proceso de
 * parsing de un archivo de catálogo o de aplicación ACES.
 *
 * <p>Se declara como checked exception (extiende {@link Exception} y no
 * {@link RuntimeException}) para forzar el manejo explícito en los parsers
 * concretos (ej. {@code ExcelCatalogParser}), evitando que errores de
 * formato de archivo pasen silenciosamente al resto de la aplicación.</p>
 */
public class ParseException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Crea una nueva excepción de parsing con un mensaje descriptivo.
     *
     * @param message mensaje descriptivo del error ocurrido durante el parsing
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Crea una nueva excepción de parsing con un mensaje descriptivo y la
     * causa original del error.
     *
     * @param message mensaje descriptivo del error ocurrido durante el parsing
     * @param cause   excepción original que provocó el error de parsing
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
