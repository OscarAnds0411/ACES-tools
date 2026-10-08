package com.validador.aces.comparison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resumen de auditoría de una línea de producto: cumplimiento de atributos
 * requeridos (en celdas), atributos opcionales de la línea y números de parte
 * a los que les falta algún atributo (requerido u opcional).
 */
public class ProductLineSummary {

    /** Etiqueta usada cuando una aplicación no trae número de parte. */
    public static final String NO_PART_NUMBER = "(sin numero de parte)";

    /** Atributos faltantes de un número de parte (unión de todas sus aplicaciones). */
    public static final class PartNumberGap {
        private final Set<String> missingRequired = new LinkedHashSet<>();
        private final Set<String> missingOptional = new LinkedHashSet<>();

        public Set<String> getMissingRequired() { return Collections.unmodifiableSet(missingRequired); }
        public Set<String> getMissingOptional() { return Collections.unmodifiableSet(missingOptional); }
        boolean isEmpty() { return missingRequired.isEmpty() && missingOptional.isEmpty(); }
    }

    private final String productLineName;
    private int applicationCount;
    private int requiredTotal;
    private int requiredSatisfied;
    private final Set<String> optionalAttributeNames = new LinkedHashSet<>();
    private final Set<String> allPartNumbers = new LinkedHashSet<>();
    private final Map<String, PartNumberGap> gaps = new LinkedHashMap<>();

    public ProductLineSummary(String productLineName) {
        this.productLineName = productLineName;
    }

    // ── Acumulación (usado por ComplianceCalculator) ───────────────────────

    void addApplication(String partNumber, int requiredTotal, int requiredSatisfied,
                        List<String> optionalNames, List<String> missingRequired,
                        List<String> missingOptional) {
        applicationCount++;
        this.requiredTotal += requiredTotal;
        this.requiredSatisfied += requiredSatisfied;
        optionalAttributeNames.addAll(optionalNames);

        String pn = (partNumber == null || partNumber.trim().isEmpty()) ? NO_PART_NUMBER : partNumber.trim();
        allPartNumbers.add(pn);
        if (!missingRequired.isEmpty() || !missingOptional.isEmpty()) {
            PartNumberGap gap = gaps.computeIfAbsent(pn, k -> new PartNumberGap());
            gap.missingRequired.addAll(missingRequired);
            gap.missingOptional.addAll(missingOptional);
        }
    }

    // ── Consulta ───────────────────────────────────────────────────────────

    public String getProductLineName() { return productLineName; }
    public int getApplicationCount() { return applicationCount; }
    public int getRequiredTotal() { return requiredTotal; }
    public int getRequiredSatisfied() { return requiredSatisfied; }
    public int getPartNumberCount() { return allPartNumbers.size(); }

    public List<String> getOptionalAttributeNames() {
        return Collections.unmodifiableList(new ArrayList<>(optionalAttributeNames));
    }

    /** @return números de parte con al menos un atributo faltante, con el detalle de cada uno */
    public Map<String, PartNumberGap> getPartNumbersWithGaps() {
        return Collections.unmodifiableMap(gaps);
    }

    /** @return false si la línea no tiene atributos requeridos (el cumplimiento es N/A) */
    public boolean isApplicable() { return requiredTotal > 0; }

    public boolean hasAllRequired() { return requiredSatisfied == requiredTotal; }

    public double getCompliancePercentage() {
        return isApplicable() ? requiredSatisfied * 100.0 / requiredTotal : 0.0;
    }

    /** @return "satisfechos/totales" de atributos requeridos (celdas), ej. "1200/1250" */
    public String getRequiredFraction() {
        return requiredSatisfied + "/" + requiredTotal;
    }

    /** @return porcentaje con un decimal, o "N/A" si no hay atributos requeridos */
    public String getComplianceLabel() {
        return isApplicable() ? String.format("%.1f%%", getCompliancePercentage()) : "N/A";
    }

    /** @return opcionales de la línea separados por coma, o "ninguno" */
    public String getOptionalAttributesText() {
        return optionalAttributeNames.isEmpty() ? "ninguno" : String.join(", ", optionalAttributeNames);
    }

    /**
     * Frase de auditoría de la línea, por ejemplo:
     * <pre>Linea de producto: "Belt" - contiene todos sus atributos requeridos (1250/1250, 100.0%);
     * los opcionales son: A, B; a 12 de 340 numeros de parte les falta algun atributo.</pre>
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("Linea de producto: \"").append(productLineName).append("\" - ");
        if (!isApplicable()) {
            sb.append("no tiene atributos requeridos (N/A)");
        } else if (hasAllRequired()) {
            sb.append("contiene todos sus atributos requeridos (")
              .append(getRequiredFraction()).append(", ").append(getComplianceLabel()).append(")");
        } else {
            sb.append("NO contiene todos sus atributos requeridos (")
              .append(getRequiredFraction()).append(", ").append(getComplianceLabel()).append(")");
        }
        sb.append("; los opcionales son: ").append(getOptionalAttributesText()).append("; ");
        if (gaps.isEmpty()) {
            sb.append("todos sus numeros de parte tienen sus atributos completos.");
        } else {
            sb.append("a ").append(gaps.size()).append(" de ").append(allPartNumbers.size())
              .append(" numeros de parte les falta algun atributo.");
        }
        return sb.toString();
    }
}
