package com.validador.aces.models;

/**
 * Nivel de requerimiento de un atributo en el catálogo ACES.
 * Mapea directamente los tres valores posibles en las celdas del catálogo Excel.
 */
public enum AttributeRequirement {
    REQUIRED("Required"),
    OPTIONAL("Optional"),
    NOT_REQUIRED("Not Required");

    private final String label;

    AttributeRequirement(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public boolean isRequired()    { return this == REQUIRED; }
    public boolean isOptional()    { return this == OPTIONAL; }
    public boolean isNotRequired() { return this == NOT_REQUIRED; }

    /**
     * Parsea el valor exacto que viene de la celda Excel.
     * @param cellValue valor de la celda, ej: "Required", "Optional", "Not Required"
     * @return AttributeRequirement correspondiente, NOT_REQUIRED si el valor es null o desconocido
     */
    public static AttributeRequirement fromCellValue(String cellValue) {
        if (cellValue == null) return NOT_REQUIRED;
        switch (cellValue.trim()) {
            case "Required":     return REQUIRED;
            case "Optional":     return OPTIONAL;
            case "Not Required": return NOT_REQUIRED;
            default:             return NOT_REQUIRED;
        }
    }
}
