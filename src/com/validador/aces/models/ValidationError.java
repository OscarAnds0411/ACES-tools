package com.validador.aces.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa un error o advertencia de validación.
 * 
 * Registra información detallada sobre problemas encontrados durante la validación de atributos.
 */
public class ValidationError implements Serializable {
    private static final long serialVersionUID = 1L;

    private ErrorSeverity severity;
    private String code;
    private String message;
    private String attributeName;
    private String productLine;
    private LocalDateTime timestamp;
    private Object invalidValue;

    /**
     * Constructor con parámetros obligatorios.
     * 
     * @param severity nivel de severidad del error
     * @param code código de error único
     * @param message mensaje descriptivo del error
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public ValidationError(ErrorSeverity severity, String code, String message) {
        this(severity, code, message, null, null);
    }

    /**
     * Constructor completo.
     * 
     * @param severity nivel de severidad del error
     * @param code código de error único
     * @param message mensaje descriptivo del error
     * @param attributeName nombre del atributo con error
     * @param productLine línea de producto afectada
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public ValidationError(ErrorSeverity severity, String code, String message, 
                          String attributeName, String productLine) {
        if (severity == null) {
            throw new IllegalArgumentException("ErrorSeverity no puede ser null");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code no puede ser null ni vacío");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message no puede ser null ni vacío");
        }

        this.severity = severity;
        this.code = code;
        this.message = message;
        this.attributeName = attributeName;
        this.productLine = productLine;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor con valor inválido incluido.
     * 
     * @param severity nivel de severidad del error
     * @param code código de error único
     * @param message mensaje descriptivo del error
     * @param attributeName nombre del atributo con error
     * @param productLine línea de producto afectada
     * @param invalidValue valor que causó el error
     */
    public ValidationError(ErrorSeverity severity, String code, String message,
                          String attributeName, String productLine, Object invalidValue) {
        this(severity, code, message, attributeName, productLine);
        this.invalidValue = invalidValue;
    }

    // Getters y Setters
    public ErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ErrorSeverity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("ErrorSeverity no puede ser null");
        }
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code no puede ser null ni vacío");
        }
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message no puede ser null ni vacío");
        }
        this.message = message;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getProductLine() {
        return productLine;
    }

    public void setProductLine(String productLine) {
        this.productLine = productLine;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public void setInvalidValue(Object invalidValue) {
        this.invalidValue = invalidValue;
    }

    public boolean isError() {
        return severity == ErrorSeverity.ERROR;
    }

    public boolean isWarning() {
        return severity == ErrorSeverity.WARNING;
    }

    public boolean isInfo() {
        return severity == ErrorSeverity.INFO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return severity == that.severity &&
               Objects.equals(code, that.code) &&
               Objects.equals(attributeName, that.attributeName) &&
               Objects.equals(productLine, that.productLine);
    }

    @Override
    public int hashCode() {
        return Objects.hash(severity, code, attributeName, productLine);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append("Code: ").append(code);
        
        if (attributeName != null && !attributeName.isEmpty()) {
            sb.append(", Attribute: ").append(attributeName);
        }
        
        if (productLine != null && !productLine.isEmpty()) {
            sb.append(", ProductLine: ").append(productLine);
        }
        
        sb.append(" - ").append(message);
        
        if (invalidValue != null) {
            sb.append(" (Value: ").append(invalidValue).append(")");
        }
        
        return sb.toString();
    }
}
