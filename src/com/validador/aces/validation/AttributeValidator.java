package com.validador.aces.validation;

import java.util.ArrayList;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ProductLine;
import com.validador.aces.models.ValidationError;

/**
 * Validador principal del sistema: utiliza {@link ValidationSchema} para
 * detectar, a partir del catálogo real, los atributos requeridos ausentes
 * en una {@link Application}.
 *
 * <p>Si la línea de producto de la aplicación no existe en el catálogo, se
 * reporta una {@link ErrorSeverity#WARNING} indicando que la aplicación no
 * pudo clasificarse (en lugar de un {@code ERROR}, ya que no hay esquema
 * contra el cual validar). Si la línea de producto existe, se delega en
 * {@link ValidationSchema#findMissingRequiredAttributes(Application)} y se
 * genera un {@link ErrorSeverity#ERROR} por cada atributo requerido
 * faltante.</p>
 */
public class AttributeValidator extends Validator {

    /** Código de error cuando el producto de la aplicación no existe en el catálogo. */
    public static final String PRODUCT_LINE_NOT_FOUND = "PRODUCT_LINE_NOT_FOUND";

    /** Código de error cuando un atributo requerido no está presente en la aplicación. */
    public static final String MISSING_REQUIRED_ATTRIBUTE = "MISSING_REQUIRED_ATTRIBUTE";

    @Override
    public List<ValidationError> validate(Application application, Catalog catalog) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog no puede ser null");
        }

        List<ValidationError> errors = new ArrayList<>();

        ProductLine line = catalog.findProductByName(application.getProductName());
        if (line == null) {
            errors.add(new ValidationError(
                ErrorSeverity.WARNING,
                PRODUCT_LINE_NOT_FOUND,
                "El producto '" + application.getProductName() + "' no se encontró en el catálogo " +
                    "(aplicación sin clasificar)",
                null,
                application.getProductName()
            ));
            return errors;
        }

        ValidationSchema schema = new ValidationSchema(line);
        List<String> missing = schema.findMissingRequiredAttributes(application);

        for (String attributeName : missing) {
            errors.add(new ValidationError(
                ErrorSeverity.ERROR,
                MISSING_REQUIRED_ATTRIBUTE,
                "El atributo requerido '" + attributeName + "' no está presente en la aplicación",
                attributeName,
                line.getName()
            ));
        }

        return errors;
    }
}
