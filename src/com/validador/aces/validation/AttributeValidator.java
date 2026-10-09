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

        ProductLine line = catalog.findProductByName(application.getProductName());
        ValidationSchema schema = line != null ? new ValidationSchema(line) : null;
        return validateResolved(application, line, schema);
    }

    /**
     * Igual que {@link #validate(Application, Catalog)} pero con la línea de
     * producto y su esquema ya resueltos por el llamador. Sirve para validar
     * lotes grandes sin volver a buscar la línea en el catálogo (búsqueda lineal)
     * ni reconstruir el {@link ValidationSchema} por cada aplicación, y garantiza
     * que los errores y los totales del llamador salgan de la misma línea.
     *
     * @param application aplicación a validar
     * @param line        línea de producto de la aplicación, o null si no se encontró
     * @param schema      esquema de {@code line}; debe ser null si y solo si {@code line} es null
     * @return errores/advertencias de la aplicación (mismo contenido que {@code validate})
     * @throws IllegalArgumentException si {@code application} es null, o si solo uno de
     *                                  {@code line}/{@code schema} es null
     */
    public List<ValidationError> validateResolved(Application application, ProductLine line,
                                                  ValidationSchema schema) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }
        if ((line == null) != (schema == null)) {
            throw new IllegalArgumentException("line y schema deben ser ambos null o ambos no null");
        }

        List<ValidationError> errors = new ArrayList<>();

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
