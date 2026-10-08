package com.validador.aces.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agrupa los resultados de una comparación entre una aplicación y un catálogo.
 * 
 * Contiene estadísticas, errores y advertencias de la validación.
 */
public class ComparisonResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String applicationName;
    private String catalogName;
    private int totalAttributes;
    private int validAttributes;
    private int invalidAttributes;
    private List<ValidationError> errors;
    private List<ValidationError> warnings;
    private LocalDateTime comparisonDate;
    private String partNumber;
    private List<String> optionalAttributeNames = Collections.emptyList();
    private List<String> missingOptionalAttributes = Collections.emptyList();

    /**
     * Constructor con parámetros obligatorios.
     * 
     * @param applicationName nombre de la aplicación
     * @param catalogName nombre del catálogo
     * @throws IllegalArgumentException si algún parámetro es null
     */
    public ComparisonResult(String applicationName, String catalogName) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name no puede ser null ni vacío");
        }
        if (catalogName == null || catalogName.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog name no puede ser null ni vacío");
        }

        this.applicationName = applicationName;
        this.catalogName = catalogName;
        this.totalAttributes = 0;
        this.validAttributes = 0;
        this.invalidAttributes = 0;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.comparisonDate = LocalDateTime.now();
    }

    /**
     * Agrega un error a los resultados.
     * 
     * @param error error a agregar
     * @throws IllegalArgumentException si error es null
     */
    public void addError(ValidationError error) {
        if (error == null) {
            throw new IllegalArgumentException("ValidationError no puede ser null");
        }
        errors.add(error);
    }

    /**
     * Agrega una advertencia a los resultados.
     * 
     * @param warning advertencia a agregar
     * @throws IllegalArgumentException si warning es null
     */
    public void addWarning(ValidationError warning) {
        if (warning == null) {
            throw new IllegalArgumentException("ValidationError no puede ser null");
        }
        warnings.add(warning);
    }

    /**
     * Agrega múltiples errores.
     * 
     * @param errorList lista de errores
     */
    public void addErrors(List<ValidationError> errorList) {
        if (errorList != null) {
            for (ValidationError error : errorList) {
                if (error.isError()) {
                    addError(error);
                } else if (error.isWarning()) {
                    addWarning(error);
                }
            }
        }
    }

    /**
     * Obtiene todos los errores.
     * 
     * @return lista no modificable de errores
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Obtiene todas las advertencias.
     * 
     * @return lista no modificable de advertencias
     */
    public List<ValidationError> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Calcula el porcentaje de cumplimiento.
     * 
     * @return porcentaje de cumplimiento (0-100)
     */
    public double getCompliancePercentage() {
        if (totalAttributes == 0) {
            return 0.0;
        }
        return (double) validAttributes / totalAttributes * 100.0;
    }

    /**
     * Indica si el porcentaje de cumplimiento tiene sentido: es falso cuando no hay
     * atributos requeridos que evaluar (el resultado es "N/A", no 0 % ni 100 %).
     */
    public boolean isApplicable() {
        return totalAttributes > 0;
    }

    /** @return el porcentaje con un decimal (ej. "87.5%"), o "N/A" si no es aplicable */
    public String getComplianceLabel() {
        return isApplicable() ? String.format("%.1f%%", getCompliancePercentage()) : "N/A";
    }

    /**
     * Obtiene el número total de errores.
     * 
     * @return cantidad de errores
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Obtiene el número total de advertencias.
     * 
     * @return cantidad de advertencias
     */
    public int getWarningCount() {
        return warnings.size();
    }

    /**
     * Obtiene el número total de problemas (errores + advertencias).
     * 
     * @return cantidad total de problemas
     */
    public int getTotalIssueCount() {
        return errors.size() + warnings.size();
    }

    /**
     * Determina si la validación fue completamente exitosa.
     * 
     * @return true si no hay errores
     */
    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    /**
     * Determina si hay problemas (errores o advertencias).
     * 
     * @return true si hay al menos un problema
     */
    public boolean hasIssues() {
        return !errors.isEmpty() || !warnings.isEmpty();
    }

    // Getters y Setters
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name no puede ser null ni vacío");
        }
        this.applicationName = applicationName;
    }

    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        if (catalogName == null || catalogName.trim().isEmpty()) {
            throw new IllegalArgumentException("Catalog name no puede ser null ni vacío");
        }
        this.catalogName = catalogName;
    }

    public int getTotalAttributes() {
        return totalAttributes;
    }

    public void setTotalAttributes(int totalAttributes) {
        this.totalAttributes = totalAttributes;
    }

    public int getValidAttributes() {
        return validAttributes;
    }

    public void setValidAttributes(int validAttributes) {
        this.validAttributes = validAttributes;
    }

    public int getInvalidAttributes() {
        return invalidAttributes;
    }

    public void setInvalidAttributes(int invalidAttributes) {
        this.invalidAttributes = invalidAttributes;
    }

    public LocalDateTime getComparisonDate() {
        return comparisonDate;
    }

    public void setComparisonDate(LocalDateTime comparisonDate) {
        this.comparisonDate = comparisonDate != null ? comparisonDate : LocalDateTime.now();
    }

    /** @return número de parte de la aplicación evaluada, o null si no lo tiene */
    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    /** @return nombres de los atributos opcionales de la línea de producto (lista compartida, no modificable) */
    public List<String> getOptionalAttributeNames() {
        return optionalAttributeNames;
    }

    public void setOptionalAttributeNames(List<String> names) {
        this.optionalAttributeNames = names == null ? Collections.<String>emptyList() : names;
    }

    /** @return atributos opcionales que esta aplicación no trae (vacío o null en el Excel) */
    public List<String> getMissingOptionalAttributes() {
        return missingOptionalAttributes;
    }

    public void setMissingOptionalAttributes(List<String> names) {
        this.missingOptionalAttributes = names == null ? Collections.<String>emptyList() : names;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparisonResult that = (ComparisonResult) o;
        return Objects.equals(applicationName, that.applicationName) &&
               Objects.equals(catalogName, that.catalogName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationName, catalogName);
    }

    @Override
    public String toString() {
        return "ComparisonResult{" +
                "applicationName='" + applicationName + '\'' +
                ", catalogName='" + catalogName + '\'' +
                ", totalAttributes=" + totalAttributes +
                ", validAttributes=" + validAttributes +
                ", invalidAttributes=" + invalidAttributes +
                ", errorCount=" + errors.size() +
                ", warningCount=" + warnings.size() +
                ", compliancePercentage=" + String.format("%.2f%%", getCompliancePercentage()) +
                ", comparisonDate=" + comparisonDate +
                '}';
    }
}
