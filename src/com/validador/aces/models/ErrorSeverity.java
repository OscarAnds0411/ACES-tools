package com.validador.aces.models;

/**
 * Enumeración que define los niveles de severidad de errores de validación.
 */
public enum ErrorSeverity {
    /**
     * Error crítico que impide cumplimiento de requisitos
     */
    ERROR,
    
    /**
     * Advertencia que podría causar problemas
     */
    WARNING,
    
    /**
     * Información adicional o sugerencia
     */
    INFO;

    /**
     * Convierte un string a ErrorSeverity.
     * 
     * @param value valor en string
     * @return ErrorSeverity correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static ErrorSeverity fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ErrorSeverity no puede ser null");
        }
        try {
            return ErrorSeverity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Severidad inválida: " + value + 
                ". Valores soportados: ERROR, WARNING, INFO"
            );
        }
    }
}
