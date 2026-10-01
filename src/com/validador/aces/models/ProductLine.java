package com.validador.aces.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa una línea de producto en el catálogo ACES.
 * 
 * Cada línea de producto contiene una colección de atributos específicos.
 */
public class ProductLine implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String category;
    private String subCategory;
    private String description;
    private List<Attribute> attributes;

    /**
     * Constructor con parámetros obligatorios.
     * 
     * @param id identificador único de la línea de producto
     * @param name nombre de la línea de producto
     * @throws IllegalArgumentException si id o name son null
     */
    public ProductLine(String id, String name) {
        this(id, name, null, null, null);
    }

    /**
     * Constructor con nombre y descripción.
     * 
     * @param id identificador único de la línea de producto
     * @param name nombre de la línea de producto
     * @param description descripción de la línea de producto
     * @throws IllegalArgumentException si id o name son null
     */
    public ProductLine(String id, String name, String description) {
        this(id, name, null, null, description);
    }

    /**
     * Constructor completo con categoría y subcategoría.
     * Corresponde a las columnas 1-4 del catálogo Excel.
     *
     * @param id          identificador único (columna 1: Product ID del catálogo)
     * @param name        nombre del producto (columna 2: Product Line)
     * @param category    categoría (columna 3: Category)
     * @param subCategory subcategoría (columna 4: Sub Category)
     * @param description descripción opcional
     * @throws IllegalArgumentException si id o name son null
     */
    public ProductLine(String id, String name, String category, String subCategory, String description) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductLine id no puede ser null ni vacío");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductLine name no puede ser null ni vacío");
        }

        this.id          = id;
        this.name        = name;
        this.category    = category;
        this.subCategory = subCategory;
        this.description = description;
        this.attributes  = new ArrayList<>();
    }

    /**
     * Agrega un atributo a la línea de producto.
     * 
     * @param attribute atributo a agregar
     * @throws IllegalArgumentException si attribute es null
     */
    public void addAttribute(Attribute attribute) {
        if (attribute == null) {
            throw new IllegalArgumentException("Attribute no puede ser null");
        }
        this.attributes.add(attribute);
    }

    /**
     * Obtiene todos los atributos de la línea de producto.
     * 
     * @return lista no modificable de atributos
     */
    public List<Attribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    /**
     * Busca un atributo por nombre.
     * 
     * @param attributeName nombre del atributo a buscar
     * @return Attribute si existe, null en otro caso
     */
    public Attribute findAttributeByName(String attributeName) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            return null;
        }
        return attributes.stream()
            .filter(attr -> attr.getName().equalsIgnoreCase(attributeName))
            .findFirst()
            .orElse(null);
    }

    /**
     * Busca un atributo por id.
     * 
     * @param attributeId id del atributo a buscar
     * @return Attribute si existe, null en otro caso
     */
    public Attribute findAttributeById(String attributeId) {
        if (attributeId == null || attributeId.trim().isEmpty()) {
            return null;
        }
        return attributes.stream()
            .filter(attr -> Objects.equals(attr.getId(), attributeId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Obtiene el número de atributos en la línea de producto.
     * 
     * @return cantidad de atributos
     */
    public int getAttributeCount() {
        return attributes.size();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductLine id no puede ser null ni vacío");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("ProductLine name no puede ser null ni vacío");
        }
        this.name = name;
    }

    /** Columna 3 del catálogo Excel: Category (ej: "Electrical, Lighting and Body"). */
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /** Columna 4 del catálogo Excel: Sub Category (ej: "Power Outlets"). */
    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductLine that = (ProductLine) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "ProductLine{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", description='" + description + '\'' +
                ", attributeCount=" + attributes.size() +
                '}';
    }
}
