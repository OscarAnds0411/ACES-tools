package com.validador.aces.comparison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase de datos inmutable que representa métricas de cumplimiento
 * (compliance) de atributos requeridos, ya sea para una sola aplicación
 * (producida por {@code ComparisonResult}) o para un grupo agregado de
 * resultados (por ejemplo, todas las aplicaciones de una misma línea de
 * producto).
 *
 * <p>Es el formato de salida de {@link ComplianceCalculator}, que actúa
 * como puente entre {@code ComparisonResult} (una sola aplicación) y
 * métricas agregadas por producto o por lote.</p>
 */
public class ComplianceMetrics {

    private final String subjectName;
    private final String productLineName;
    private final int totalRequiredAttributes;
    private final int satisfiedRequiredAttributes;
    private final int unmetRequiredAttributes;
    private final double compliancePercentage;
    private final List<String> unmetAttributeNames;

    /**
     * Construye las métricas de cumplimiento.
     *
     * @param subjectName                 nombre de la aplicación o etiqueta descriptiva del grupo agregado
     * @param productLineName             nombre de la línea de producto asociada, o null si no se pudo determinar
     * @param totalRequiredAttributes     cantidad total de atributos requeridos considerados
     * @param satisfiedRequiredAttributes cantidad de atributos requeridos satisfechos
     * @param unmetAttributeNames         nombres de los atributos requeridos incumplidos (puede ser null, se interpreta como lista vacía)
     * @throws IllegalArgumentException si {@code subjectName} es null/vacío, si {@code totalRequiredAttributes} es negativo,
     *                                   o si {@code satisfiedRequiredAttributes} está fuera del rango [0, totalRequiredAttributes]
     */
    public ComplianceMetrics(String subjectName, String productLineName, int totalRequiredAttributes,
                              int satisfiedRequiredAttributes, List<String> unmetAttributeNames) {
        if (subjectName == null || subjectName.trim().isEmpty()) {
            throw new IllegalArgumentException("subjectName no puede ser null ni vacío");
        }
        if (totalRequiredAttributes < 0) {
            throw new IllegalArgumentException("totalRequiredAttributes no puede ser negativo");
        }
        if (satisfiedRequiredAttributes < 0 || satisfiedRequiredAttributes > totalRequiredAttributes) {
            throw new IllegalArgumentException(
                "satisfiedRequiredAttributes debe estar entre 0 y totalRequiredAttributes");
        }

        this.subjectName = subjectName;
        this.productLineName = productLineName;
        this.totalRequiredAttributes = totalRequiredAttributes;
        this.satisfiedRequiredAttributes = satisfiedRequiredAttributes;
        this.unmetRequiredAttributes = totalRequiredAttributes - satisfiedRequiredAttributes;
        this.compliancePercentage = totalRequiredAttributes == 0
            ? 0.0
            : (satisfiedRequiredAttributes / (double) totalRequiredAttributes) * 100.0;
        this.unmetAttributeNames = unmetAttributeNames == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(unmetAttributeNames));
    }

    /** @return false si no había atributos requeridos que evaluar (el cumplimiento es "N/A"). */
    public boolean isApplicable() {
        return totalRequiredAttributes > 0;
    }

    /** @return el porcentaje con un decimal (ej. "87.5%"), o "N/A" si no es aplicable */
    public String getComplianceLabel() {
        return isApplicable() ? String.format("%.1f%%", compliancePercentage) : "N/A";
    }

    /** @return nombre de la aplicación o etiqueta del grupo agregado. */
    public String getSubjectName() {
        return subjectName;
    }

    /** @return nombre de la línea de producto asociada, o null si no se pudo determinar. */
    public String getProductLineName() {
        return productLineName;
    }

    /** @return cantidad total de atributos requeridos considerados. */
    public int getTotalRequiredAttributes() {
        return totalRequiredAttributes;
    }

    /** @return cantidad de atributos requeridos satisfechos. */
    public int getSatisfiedRequiredAttributes() {
        return satisfiedRequiredAttributes;
    }

    /** @return cantidad de atributos requeridos incumplidos. */
    public int getUnmetRequiredAttributes() {
        return unmetRequiredAttributes;
    }

    /** @return porcentaje de cumplimiento (0-100). */
    public double getCompliancePercentage() {
        return compliancePercentage;
    }

    /** @return lista no modificable de nombres de atributos requeridos incumplidos. */
    public List<String> getUnmetAttributeNames() {
        return unmetAttributeNames;
    }

    @Override
    public String toString() {
        return "ComplianceMetrics{" +
                "subjectName='" + subjectName + '\'' +
                ", productLineName='" + productLineName + '\'' +
                ", totalRequiredAttributes=" + totalRequiredAttributes +
                ", satisfiedRequiredAttributes=" + satisfiedRequiredAttributes +
                ", unmetRequiredAttributes=" + unmetRequiredAttributes +
                ", compliancePercentage=" + String.format("%.2f%%", compliancePercentage) +
                ", unmetAttributeNames=" + unmetAttributeNames +
                '}';
    }
}
