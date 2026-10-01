package com.validador.aces.models;

/**
 * Enumeración que define los tipos de acciones de auditoría.
 */
public enum AuditAction {
    /**
     * Validación de datos
     */
    VALIDATION,
    
    /**
     * Comparación de atributos
     */
    COMPARISON,
    
    /**
     * Exportación de datos
     */
    EXPORT,
    
    /**
     * Importación de datos
     */
    IMPORT;

    /**
     * Convierte un string a AuditAction.
     * 
     * @param value valor en string
     * @return AuditAction correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static AuditAction fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AuditAction no puede ser null");
        }
        try {
            return AuditAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Acción de auditoría inválida: " + value + 
                ". Valores soportados: VALIDATION, COMPARISON, EXPORT, IMPORT"
            );
        }
    }
}
