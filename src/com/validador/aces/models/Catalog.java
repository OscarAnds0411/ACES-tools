package com.validador.aces.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/**
 * Representa el catálogo maestro de atributos ACES.
 * 
 * Un catálogo contiene múltiples líneas de producto, cada una con sus atributos específicos.
 */
public class Catalog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String version;
    private String description;
    private LocalDateTime createdDate;
    private List<ProductLine> productLines;

    /**
     * Constructor con parámetros obligatorios.
     * 
     * @param id identificador único del catálogo
     * @param name nombre del catálogo
     * @throws IllegalArgumentException si id o name son null
     */
    public Catalog(String id, String name) {
        this(id, name, null, null, null);
    }

    /**
     * Constructor completo.
     * 
     * @param id identificador único del catálogo
     * @param name nombre del catálogo
     * @param version versión del catálogo
     * @param description descripción del catálogo
     * @param createdDate fecha de creación
     * @throws IllegalArgumentException si id o name son null
     */
    public Catalog(String id, String name, String version, String description, LocalDateTime createdDate) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog id no puede ser null ni vacío");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog name no puede ser null ni vacío");
        }

        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.createdDate = createdDate != null ? createdDate : LocalDateTime.now();
        this.productLines = new ArrayList<>();
    }

    /**
     * Agrega una línea de producto al catálogo.
     * 
     * @param productLine línea de producto a agregar
     * @throws IllegalArgumentException si productLine es null
     */
    public void addProductLine(ProductLine productLine) {
        if (productLine == null) {
            throw new IllegalArgumentException("ProductLine no puede ser null");
        }
        this.productLines.add(productLine);
    }

    /**
     * Obtiene todas las líneas de producto.
     * 
     * @return lista no modificable de líneas de producto
     */
    public List<ProductLine> getProductLines() {
        return Collections.unmodifiableList(productLines);
    }

    /**
     * Nombres de todos los atributos que aparecen en alguna línea de producto.
     * Sirve para saber qué columnas de un ACES son de interés.
     *
     * @return conjunto ordenado (por aparición) de nombres de atributo
     */
    public Set<String> getAllAttributeNames() {
        Set<String> names = new LinkedHashSet<>();
        for (ProductLine pl : productLines) {
            for (Attribute a : pl.getAttributes()) names.add(a.getName());
        }
        return names;
    }

    /**
     * Busca una línea de producto por id.
     * 
     * @param productLineId id de la línea a buscar
     * @return ProductLine si existe, null en otro caso
     */
    public ProductLine findProductLineById(String productLineId) {
        return productLines.stream()
            .filter(pl -> Objects.equals(pl.getId(), productLineId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Busca una línea de producto por nombre exacto (case-insensitive).
     * El nombre de producto en el catálogo se vincula con la columna "Product" del ACES.
     *
     * @param productName nombre del producto, ej: "A/C Condenser"
     * @return ProductLine correspondiente, o null si no se encuentra
     */
    public ProductLine findProductByName(String productName) {
        if (productName == null || productName.trim().isEmpty()) return null;
        return productLines.stream()
            .filter(pl -> pl.getName().equalsIgnoreCase(productName.trim()))
            .findFirst()
            .orElse(null);
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog id no puede ser null ni vacío");
        }
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog name no puede ser null ni vacío");
        }
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate != null ? createdDate : LocalDateTime.now();
    }

    public int getProductLineCount() {
        return productLines.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Catalog catalog = (Catalog) o;
        return Objects.equals(id, catalog.id) &&
               Objects.equals(name, catalog.name) &&
               Objects.equals(version, catalog.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, version);
    }

    @Override
    public String toString() {
        return "Catalog{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", createdDate=" + createdDate +
                ", productLineCount=" + productLines.size() +
                '}';
    }
}
