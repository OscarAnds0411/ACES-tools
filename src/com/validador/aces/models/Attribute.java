package com.validador.aces.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representa un atributo del estándar ACES en el catálogo.
 *
 * <p>En el catálogo Excel, un atributo corresponde al encabezado de una columna
 * (ej: "EngineLiters", "VehicleType") y su nivel de requerimiento para un
 * producto específico ("Required", "Optional", "Not Required").</p>
 */
public class Attribute implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private AttributeRequirement requirement;
    private String description;

    /**
     * Constructor principal.
     *
     * @param id   identificador (generalmente el nombre de columna del catálogo)
     * @param name nombre del atributo, ej: "EngineLiters"
     * @param requirement nivel de requerimiento para este producto
     */
    public Attribute(String id, String name, AttributeRequirement requirement) {
        if (id == null || id.trim().isEmpty())
            throw new IllegalArgumentException("Attribute id no puede ser null ni vacío");
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Attribute name no puede ser null ni vacío");
        if (requirement == null)
            throw new IllegalArgumentException("AttributeRequirement no puede ser null");

        this.id          = id;
        this.name        = name;
        this.requirement = requirement;
    }

    /** Constructor de conveniencia que parsea el valor de la celda Excel directamente. */
    public Attribute(String id, String name, String cellValue) {
        this(id, name, AttributeRequirement.fromCellValue(cellValue));
    }

    // ── Métodos de negocio ────────────────────────────────────────────────────

    /** @return true si este atributo es obligatorio para el producto. */
    public boolean isRequired()    { return requirement.isRequired(); }

    /** @return true si este atributo es opcional para el producto. */
    public boolean isOptional()    { return requirement.isOptional(); }

    /** @return true si este atributo no es necesario para el producto. */
    public boolean isNotRequired() { return requirement.isNotRequired(); }

    /**
     * Determina si un valor de la aplicación ACES satisface este atributo.
     *
     * <p>La lógica es simple: si el atributo es {@code REQUIRED}, el valor debe
     * estar presente y no ser vacío. Si es {@code OPTIONAL} o {@code NOT_REQUIRED},
     * cualquier valor (incluyendo null) es aceptado.</p>
     *
     * @param value valor presente en la aplicación ACES para este atributo
     * @return true si el valor satisface el requerimiento
     */
    public boolean isSatisfiedBy(Object value) {
        if (!requirement.isRequired()) return true;
        if (value == null) return false;
        if (value instanceof String) return !((String) value).trim().isEmpty();
        return true;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId()   { return id; }
    public String getName() { return name; }

    public AttributeRequirement getRequirement() { return requirement; }
    public void setRequirement(AttributeRequirement requirement) {
        if (requirement == null)
            throw new IllegalArgumentException("AttributeRequirement no puede ser null");
        this.requirement = requirement;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute that = (Attribute) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "Attribute{name='" + name + "', requirement=" + requirement.getLabel() + "}";
    }
}
