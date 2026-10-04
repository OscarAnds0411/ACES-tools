package com.validador.aces.comparison;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;

/**
 * Calcula métricas de cumplimiento ({@link ComplianceMetrics}) a partir de
 * {@link ComparisonResult}, actuando como puente entre el resultado de
 * comparación de una sola aplicación y las métricas agregadas por línea de
 * producto o por lote.
 *
 * <p>{@code ComparisonResult} no almacena la línea de producto como campo
 * propio (solo el nombre de catálogo y de aplicación), por lo que el
 * agrupamiento por línea de producto en
 * {@link #calculateByProductLine(Map)} debe ser resuelto por el llamador
 * (por ejemplo, el orquestador que ejecuta las comparaciones conoce la
 * línea de producto de cada aplicación en el momento de agrupar).</p>
 */
public class ComplianceCalculator {

    /**
     * Calcula las métricas de cumplimiento globales de un único resultado
     * de comparación.
     *
     * <p>El nombre de la línea de producto se obtiene, si es posible, del
     * primer {@link ValidationError} (entre errores y advertencias) que
     * tenga un {@code productLine} no null. Si el resultado está 100%
     * compliant y no se generó ninguna entrada de error/advertencia con
     * dicha información (por ejemplo, cuando no hubo ningún atributo
     * faltante ni advertencia de producto no encontrado), el nombre de
     * línea de producto quedará como null; esta es una limitación conocida
     * derivada de que {@code ComparisonResult} no expone directamente la
     * línea de producto.</p>
     *
     * @param result resultado de comparación de una aplicación
     * @return métricas de cumplimiento calculadas a partir del resultado
     * @throws IllegalArgumentException si {@code result} es null
     */
    public ComplianceMetrics calculate(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }

        List<String> unmetAttributeNames = identifyUnmetRequiredAttributes(result);
        String productLineName = findProductLineName(result);

        return new ComplianceMetrics(
            result.getApplicationName(),
            productLineName,
            result.getTotalAttributes(),
            result.getValidAttributes(),
            unmetAttributeNames
        );
    }

    /**
     * Agrega métricas de cumplimiento por línea de producto a partir de un
     * agrupamiento ya resuelto por el llamador.
     *
     * <p>Para cada línea de producto, suma el total de atributos
     * requeridos y el total de atributos satisfechos a través de todos los
     * {@code ComparisonResult} del grupo, y une (sin duplicados, en orden
     * de aparición) los nombres de atributos requeridos incumplidos de
     * cada resultado.</p>
     *
     * @param resultsByProductLine mapa de nombre de línea de producto a lista de resultados de esa línea
     * @return mapa de nombre de línea de producto a sus métricas de cumplimiento agregadas
     * @throws IllegalArgumentException si {@code resultsByProductLine} es null
     */
    public Map<String, ComplianceMetrics> calculateByProductLine(
            Map<String, List<ComparisonResult>> resultsByProductLine) {
        if (resultsByProductLine == null) {
            throw new IllegalArgumentException("resultsByProductLine no puede ser null");
        }

        Map<String, ComplianceMetrics> aggregated = new LinkedHashMap<>();

        for (Map.Entry<String, List<ComparisonResult>> entry : resultsByProductLine.entrySet()) {
            String productLineName = entry.getKey();
            List<ComparisonResult> results = entry.getValue();

            int totalRequiredAttributes = 0;
            int satisfiedRequiredAttributes = 0;
            Set<String> unmetAttributeNames = new LinkedHashSet<>();

            for (ComparisonResult r : results) {
                totalRequiredAttributes += r.getTotalAttributes();
                satisfiedRequiredAttributes += r.getValidAttributes();
                unmetAttributeNames.addAll(identifyUnmetRequiredAttributes(r));
            }

            String subjectName = productLineName + " (" + results.size() + " aplicaciones)";

            aggregated.put(productLineName, new ComplianceMetrics(
                subjectName,
                productLineName,
                totalRequiredAttributes,
                satisfiedRequiredAttributes,
                new ArrayList<>(unmetAttributeNames)
            ));
        }

        return aggregated;
    }

    /**
     * Extrae los nombres de los atributos requeridos incumplidos de un
     * resultado de comparación, a partir de los errores con código
     * {@link AttributeValidator#MISSING_REQUIRED_ATTRIBUTE}.
     *
     * @param result resultado de comparación a inspeccionar
     * @return lista de nombres de atributos requeridos incumplidos (puede contener duplicados si el validador los generara)
     * @throws IllegalArgumentException si {@code result} es null
     */
    public List<String> identifyUnmetRequiredAttributes(ComparisonResult result) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }

        List<String> unmet = new ArrayList<>();
        for (ValidationError error : result.getErrors()) {
            if (AttributeValidator.MISSING_REQUIRED_ATTRIBUTE.equals(error.getCode())) {
                unmet.add(error.getAttributeName());
            }
        }
        return unmet;
    }

    private String findProductLineName(ComparisonResult result) {
        for (ValidationError error : result.getErrors()) {
            if (error.getProductLine() != null) {
                return error.getProductLine();
            }
        }
        for (ValidationError warning : result.getWarnings()) {
            if (warning.getProductLine() != null) {
                return warning.getProductLine();
            }
        }
        return null;
    }
}
