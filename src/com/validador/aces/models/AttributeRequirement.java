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
     * Indica si el texto de la celda es un valor que el catálogo entiende:
     * {@code Required}, {@code Optional} o {@code Not Required} (sin distinguir
     * mayúsculas ni espacios en los extremos). Una celda vacía o null también
     * cuenta como reconocida: significa "no requerido" por convención.
     *
     * @param cellValue texto de la celda del catálogo
     * @return false si hay texto que no corresponde a ningún nivel (por ejemplo "Req")
     */
    public static boolean isRecognized(String cellValue) {
        if (cellValue == null || cellValue.trim().isEmpty()) return true;
        return match(cellValue) != null;
    }

    /**
     * Parsea el valor que viene de la celda Excel, sin distinguir mayúsculas ni
     * espacios en los extremos. Un valor desconocido sigue tratándose como
     * NOT_REQUIRED (compatibilidad); use {@link #isRecognized(String)} para detectarlo.
     * @param cellValue valor de la celda, ej: "Required", "Optional", "Not Required"
     * @return AttributeRequirement correspondiente, NOT_REQUIRED si el valor es null o desconocido
     */
    public static AttributeRequirement fromCellValue(String cellValue) {
        if (cellValue == null) return NOT_REQUIRED;
        AttributeRequirement found = match(cellValue);
        return found != null ? found : NOT_REQUIRED;
    }

    /** Nivel cuyo texto coincide (ignorando mayúsculas y espacios en los extremos), o null. */
    private static AttributeRequirement match(String cellValue) {
        String value = cellValue.trim();
        for (AttributeRequirement requirement : values()) {
            if (requirement.label.equalsIgnoreCase(value)) return requirement;
        }
        return null;
    }
}
