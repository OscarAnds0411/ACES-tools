package com.validador.aces.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;

/**
 * Utilidad genérica de validación de formato de fecha, configurable por
 * constructor.
 *
 * <p>El catálogo ACES real no define formatos de fecha por atributo, por lo
 * que este validador no depende de {@code Catalog} ni de metadata del
 * esquema: el nombre del atributo técnico y los patrones de fecha
 * aceptados se reciben explícitamente al construirlo. Por defecto valida
 * el formato ISO 8601 ({@code yyyy-MM-dd}) si no se especifican formatos
 * alternativos.</p>
 *
 * <p>El parámetro {@code catalog} de {@link #validate(Application, Catalog)}
 * no se utiliza y puede ser {@code null} sin problema.</p>
 */
public class DateFormatValidator extends Validator {

    /** Código de error cuando el valor no coincide con ninguno de los formatos esperados. */
    public static final String INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";

    /** Patrón ISO 8601 usado por defecto cuando no se especifican formatos alternativos. */
    private static final String DEFAULT_ISO_PATTERN = "yyyy-MM-dd";

    private final String attributeName;
    private final List<String> dateFormats;

    /**
     * Construye el validador de fecha para el atributo técnico indicado,
     * usando el formato ISO 8601 ({@code yyyy-MM-dd}) por defecto.
     *
     * @param attributeName nombre del atributo técnico (clave en {@code Application.data})
     * @throws IllegalArgumentException si {@code attributeName} es null/vacío
     */
    public DateFormatValidator(String attributeName) {
        this(attributeName, Collections.singletonList(DEFAULT_ISO_PATTERN));
    }

    /**
     * Construye el validador de fecha para el atributo técnico indicado,
     * aceptando cualquiera de los patrones dados.
     *
     * @param attributeName nombre del atributo técnico (clave en {@code Application.data})
     * @param dateFormats   patrones de {@link DateTimeFormatter} a aceptar (al menos uno debe coincidir)
     * @throws IllegalArgumentException si {@code attributeName} es null/vacío, o si
     *                                   {@code dateFormats} es null o vacío
     */
    public DateFormatValidator(String attributeName, List<String> dateFormats) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            throw new IllegalArgumentException("attributeName no puede ser null ni vacío");
        }
        if (dateFormats == null || dateFormats.isEmpty()) {
            throw new IllegalArgumentException("dateFormats no puede ser null ni vacío");
        }
        this.attributeName = attributeName;
        this.dateFormats = new ArrayList<>(dateFormats);
    }

    @Override
    public List<ValidationError> validate(Application application, Catalog catalog) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }

        Object value = application.getAttributeValue(attributeName);
        if (value == null) {
            return Collections.emptyList();
        }

        String valueStr = value.toString();

        for (String pattern : dateFormats) {
            try {
                LocalDate.parse(valueStr, DateTimeFormatter.ofPattern(pattern));
                return Collections.emptyList();
            } catch (DateTimeParseException e) {
                // Intentar con el siguiente patrón.
            }
        }

        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(
            ErrorSeverity.ERROR,
            INVALID_DATE_FORMAT,
            "El valor '" + valueStr + "' del atributo '" + attributeName +
                "' no coincide con ninguno de los formatos de fecha esperados: " + dateFormats,
            attributeName,
            null
        ));
        return errors;
    }
}
