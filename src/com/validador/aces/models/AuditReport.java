package com.validador.aces.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Encapsula información de auditoría sobre operaciones realizadas.
 * 
 * Registra acciones de usuario, timestamps y resultados para propósitos de auditoría.
 */
public class AuditReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDateTime timestamp;
    private String user;
    private AuditAction action;
    private String details;
    private String result;
    private long durationMillis;

    /**
     * Constructor con parámetros obligatorios.
     * 
     * @param user usuario que realizó la acción
     * @param action tipo de acción
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public AuditReport(String user, AuditAction action) {
        this(user, action, null, null);
    }

    /**
     * Constructor completo.
     * 
     * @param user usuario que realizó la acción
     * @param action tipo de acción
     * @param details detalles adicionales
     * @param result resultado de la acción
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public AuditReport(String user, AuditAction action, String details, String result) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User no puede ser null ni vacío");
        }
        if (action == null) {
            throw new IllegalArgumentException("AuditAction no puede ser null");
        }

        this.timestamp = LocalDateTime.now();
        this.user = user;
        this.action = action;
        this.details = details;
        this.result = result;
        this.durationMillis = 0;
    }

    /**
     * Serializa el reporte a JSON.
     * 
     * @return representación JSON del reporte
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"timestamp\":\"").append(timestamp).append("\",");
        json.append("\"user\":\"").append(escapeJson(user)).append("\",");
        json.append("\"action\":\"").append(action).append("\",");
        
        if (details != null) {
            json.append("\"details\":\"").append(escapeJson(details)).append("\",");
        }
        
        if (result != null) {
            json.append("\"result\":\"").append(escapeJson(result)).append("\",");
        }
        
        json.append("\"durationMillis\":").append(durationMillis);
        json.append("}");
        
        return json.toString();
    }

    /**
     * Escapa caracteres especiales para JSON.
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Obtiene representación simple para log.
     * 
     * @return string formateado para logging
     */
    public String toLogString() {
        StringBuilder log = new StringBuilder();
        log.append("[").append(timestamp).append("] ");
        log.append("User: ").append(user).append(" | ");
        log.append("Action: ").append(action);
        
        if (details != null && !details.isEmpty()) {
            log.append(" | Details: ").append(details);
        }
        
        if (result != null && !result.isEmpty()) {
            log.append(" | Result: ").append(result);
        }
        
        if (durationMillis > 0) {
            log.append(" | Duration: ").append(durationMillis).append("ms");
        }
        
        return log.toString();
    }

    // Getters y Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User no puede ser null ni vacío");
        }
        this.user = user;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        if (action == null) {
            throw new IllegalArgumentException("AuditAction no puede ser null");
        }
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditReport that = (AuditReport) o;
        return Objects.equals(user, that.user) &&
               action == that.action &&
               Objects.equals(timestamp.withNano(0), that.timestamp.withNano(0));
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, action, timestamp.withNano(0));
    }

    @Override
    public String toString() {
        return "AuditReport{" +
                "timestamp=" + timestamp +
                ", user='" + user + '\'' +
                ", action=" + action +
                ", details='" + details + '\'' +
                ", result='" + result + '\'' +
                ", durationMillis=" + durationMillis +
                '}';
    }
}
