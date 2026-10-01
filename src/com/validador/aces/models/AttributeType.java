package com.validador.aces.models;

/**
 * Enumeración que define los tipos de datos soportados para atributos ACES.
 */
public enum AttributeType {
    /**
     * Texto libre
     */
    STRING,
    
    /**
     * Número entero
     */
    INTEGER,
    
    /**
     * Número decimal
     */
    DECIMAL,
    
    /**
     * Valor booleano (true/false)
     */
    BOOLEAN,
    
    /**
     * Fecha (formato especificado)
     */
    DATE;

    /**
     * Convierte un string a AttributeType.
     * 
     * @param value valor en string
     * @return AttributeType correspondiente
     * @throws IllegalArgumentException si el valor no es válido
     */
    public static AttributeType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AttributeType no puede ser null");
        }
        try {
            return AttributeType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Tipo de atributo inválido: " + value + 
                ". Valores soportados: STRING, INTEGER, DECIMAL, BOOLEAN, DATE"
            );
        }
    }
}
