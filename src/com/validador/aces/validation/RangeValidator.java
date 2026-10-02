package com.validador.aces.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;

/**
 * Utilidad genérica de validación de rango numérico, configurable por
 * constructor.
 *
 * <p>El catálogo ACES real no define rangos ni tipos de dato para los
 * atributos (solo nivel de requerimiento), por lo que este validador no
 * depende de {@code Catalog} ni de metadata del esquema: el nombre del
 * atributo técnico y los límites del rango se reciben explícitamente al
 * construirlo. Está pensado para ser instanciado puntualmente cuando se
 * necesite validar un atributo técnico específico del mapa de datos de la
 * aplicación (por ejemplo, "EngineCylinder") contra un rango conocido.</p>
 *
 * <p>El parámetro {@code catalog} de {@link #validate(Application, Catalog)}
 * no se utiliza y puede ser {@code null} sin problema.</p>
 */
public class RangeValidator extends Validator {

    /** Código de error cuando el valor numérico está fuera del rango permitido. */
    public static final String VALUE_OUT_OF_RANGE = "VALUE_OUT_OF_RANGE";

    private final String attributeName;
    private final double min;
    private final double max;

    /**
     * Construye el validador de rango para el atributo técnico indicado.
     *
     * @param attributeName nombre del atributo técnico (clave en {@code Application.data})
     * @param min           límite inferior (inclusive) del rango permitido
     * @param max           límite superior (inclusive) del rango permitido
     * @throws IllegalArgumentException si {@code attributeName} es null/vacío, o si {@code min > max}
     */
    public RangeValidator(String attributeName, double min, double max) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            throw new IllegalArgumentException("attributeName no puede ser null ni vacío");
        }
        if (min > max) {
            throw new IllegalArgumentException("min no puede ser mayor que max");
        }
        this.attributeName = attributeName;
        this.min = min;
        this.max = max;
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

        double numericValue;
        try {
            numericValue = Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            // No es un valor numérico: no aplica este validador.
            return Collections.emptyList();
        }

        if (numericValue < min || numericValue > max) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError(
                ErrorSeverity.ERROR,
                VALUE_OUT_OF_RANGE,
                "El valor '" + numericValue + "' del atributo '" + attributeName +
                    "' está fuera del rango permitido [" + min + ", " + max + "]",
                attributeName,
                null
            ));
            return errors;
        }

        return Collections.emptyList();
    }
}
