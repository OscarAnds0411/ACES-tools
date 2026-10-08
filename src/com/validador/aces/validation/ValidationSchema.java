package com.validador.aces.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Attribute;
import com.validador.aces.models.ProductLine;

/**
 * Representa el esquema de validación de una {@link ProductLine} específica
 * del catálogo ACES.
 *
 * <p>Un {@code ValidationSchema} separa los atributos de la línea de
 * producto en requeridos y opcionales (ignorando los no requeridos), y
 * expone la lógica para determinar si una {@link Application} concreta
 * satisface todos los atributos requeridos de dicha línea de producto.</p>
 *
 * <p>Esta clase es el puente entre el catálogo (que define las reglas de
 * requerimiento por producto) y la validación de aplicaciones concretas: no
 * depende de {@code Catalog} directamente, sino únicamente de la
 * {@code ProductLine} ya resuelta (por ejemplo, mediante
 * {@code Catalog.findProductByName(application.getProductName())}).
 * Será utilizada por {@code AttributeValidator} (TASK-014) para generar los
 * {@code ValidationError} correspondientes a atributos requeridos
 * faltantes.</p>
 */
public class ValidationSchema {

    private final ProductLine productLine;
    private final List<Attribute> requiredAttributes;
    private final List<Attribute> optionalAttributes;
    private final List<String> optionalAttributeNames;

    /**
     * Construye el esquema de validación a partir de la línea de producto
     * indicada, compilando de una sola vez (en el constructor) las listas
     * de atributos requeridos y opcionales a partir de
     * {@link ProductLine#getAttributes()}.
     *
     * @param productLine línea de producto cuyo esquema de validación se desea representar
     * @throws IllegalArgumentException si {@code productLine} es null
     */
    public ValidationSchema(ProductLine productLine) {
        if (productLine == null) {
            throw new IllegalArgumentException("ProductLine no puede ser null");
        }
        this.productLine = productLine;

        List<Attribute> required = new ArrayList<>();
        List<Attribute> optional = new ArrayList<>();
        for (Attribute attribute : productLine.getAttributes()) {
            if (attribute.isRequired()) {
                required.add(attribute);
            } else if (attribute.isOptional()) {
                optional.add(attribute);
            }
        }
        this.requiredAttributes = Collections.unmodifiableList(required);
        this.optionalAttributes = Collections.unmodifiableList(optional);
        List<String> optionalNames = new ArrayList<>();
        for (Attribute attribute : optional) optionalNames.add(attribute.getName());
        this.optionalAttributeNames = Collections.unmodifiableList(optionalNames);
    }

    /** @return la línea de producto representada por este esquema. */
    public ProductLine getProductLine() {
        return productLine;
    }

    /** @return lista no modificable de atributos requeridos de la línea de producto. */
    public List<Attribute> getRequiredAttributes() {
        return requiredAttributes;
    }

    /** @return lista no modificable de atributos opcionales de la línea de producto. */
    public List<Attribute> getOptionalAttributes() {
        return optionalAttributes;
    }

    /** @return nombres (no modificable, compartido) de los atributos opcionales de la línea. */
    public List<String> getOptionalAttributeNames() {
        return optionalAttributeNames;
    }

    /**
     * Atributos opcionales que la aplicación no trae (valor null o en blanco).
     * No son errores: sirven para informar qué tan completa viene la aplicación.
     *
     * @param application aplicación ACES a revisar
     * @return nombres de los opcionales ausentes (lista vacía si trae todos)
     * @throws IllegalArgumentException si {@code application} es null
     */
    public List<String> findMissingOptionalAttributes(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }
        List<String> missing = new ArrayList<>();
        for (Attribute attribute : optionalAttributes) {
            Object value = application.getAttributeValue(attribute.getName());
            boolean blank = value == null
                || (value instanceof String && ((String) value).trim().isEmpty());
            if (blank) missing.add(attribute.getName());
        }
        return missing;
    }

    /**
     * Valida que la aplicación dada satisfaga todos los atributos
     * requeridos de esta línea de producto.
     *
     * @param application aplicación ACES a validar contra este esquema
     * @return lista de nombres de atributos requeridos que NO están
     *         satisfechos (lista vacía si la aplicación cumple con todos
     *         los requeridos)
     * @throws IllegalArgumentException si {@code application} es null
     */
    public List<String> findMissingRequiredAttributes(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }

        List<String> missing = new ArrayList<>();
        for (Attribute attribute : requiredAttributes) {
            Object value = application.getAttributeValue(attribute.getName());
            if (!attribute.isSatisfiedBy(value)) {
                missing.add(attribute.getName());
            }
        }
        return missing;
    }

    /**
     * Determina si la aplicación dada satisface todos los atributos
     * requeridos de esta línea de producto.
     *
     * @param application aplicación ACES a validar contra este esquema
     * @return true si la aplicación satisface todos los atributos requeridos
     * @throws IllegalArgumentException si {@code application} es null
     */
    public boolean isSatisfiedBy(Application application) {
        return findMissingRequiredAttributes(application).isEmpty();
    }
}
