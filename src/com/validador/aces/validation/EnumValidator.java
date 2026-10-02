package com.validador.aces.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;

/**
 * Utilidad genérica de validación contra una lista de valores permitidos,
 * configurable por constructor.
 *
 * <p>El catálogo ACES real no define listas de valores permitidos por
 * atributo, por lo que este validador no depende de {@code Catalog} ni de
 * metadata del esquema: el nombre del atributo técnico y los valores
 * permitidos se reciben explícitamente al construirlo. Está pensado para
 * ser instanciado puntualmente cuando se necesite validar un atributo
 * técnico específico del mapa de datos de la aplicación contra una
 * enumeración conocida de valores válidos.</p>
 *
 * <p>El parámetro {@code catalog} de {@link #validate(Application, Catalog)}
 * no se utiliza y puede ser {@code null} sin problema.</p>
 */
public class EnumValidator extends Validator {

    /** Código de error cuando el valor no pertenece a la lista de valores permitidos. */
    public static final String VALUE_NOT_IN_ALLOWED_LIST = "VALUE_NOT_IN_ALLOWED_LIST";

    private final String attributeName;
    private final Set<String> allowedValues;
    private final boolean caseInsensitive;

    /**
     * Construye el validador de enumeración para el atributo técnico indicado.
     *
     * @param attributeName   nombre del atributo técnico (clave en {@code Application.data})
     * @param allowedValues   colección de valores permitidos (se preserva el orden de inserción
     *                        para construir mensajes de sugerencia legibles)
     * @param caseInsensitive si la comparación debe ignorar mayúsculas/minúsculas
     * @throws IllegalArgumentException si {@code attributeName} es null/vacío, o si
     *                                   {@code allowedValues} es null o vacío
     */
    public EnumValidator(String attributeName, Collection<String> allowedValues, boolean caseInsensitive) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            throw new IllegalArgumentException("attributeName no puede ser null ni vacío");
        }
        if (allowedValues == null || allowedValues.isEmpty()) {
            throw new IllegalArgumentException("allowedValues no puede ser null ni vacío");
        }
        this.attributeName = attributeName;
        this.allowedValues = new LinkedHashSet<>(allowedValues);
        this.caseInsensitive = caseInsensitive;
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
        boolean matches = false;
        for (String allowed : allowedValues) {
            if (caseInsensitive ? allowed.equalsIgnoreCase(valueStr) : allowed.equals(valueStr)) {
                matches = true;
                break;
            }
        }

        if (!matches) {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError(
                ErrorSeverity.ERROR,
                VALUE_NOT_IN_ALLOWED_LIST,
                "El valor '" + valueStr + "' del atributo '" + attributeName +
                    "' no está en la lista de valores permitidos: " + allowedValues,
                attributeName,
                null
            ));
            return errors;
        }

        return Collections.emptyList();
    }
}
