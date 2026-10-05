package com.validador.aces.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;

/**
 * Produce versiones filtradas de un {@link ComparisonResult}, sin modificar
 * el original, devolviendo siempre una nueva instancia que sólo contiene los
 * {@link ValidationError} que superan el criterio de filtrado.
 *
 * <p>Los contadores de atributos ({@code totalAttributes}, {@code validAttributes},
 * {@code invalidAttributes}) del resultado filtrado se recalculan a partir del
 * nuevo conjunto de errores, de modo que {@code getCompliancePercentage()} siga
 * siendo coherente con los errores presentes.</p>
 *
 * <h2>Uso típico</h2>
 * <pre>{@code
 * ReportFilter filter = new ReportFilter();
 *
 * // Solo errores críticos
 * ComparisonResult soloErrores = filter.filterBySeverity(result, ErrorSeverity.ERROR);
 *
 * // Solo problemas de un producto concreto
 * ComparisonResult soloAcCondenser = filter.filterByProductLine(result, "A/C Condenser");
 *
 * // Filtros combinados (AND lógico)
 * List<Predicate<ValidationError>> predicados = new ArrayList<>();
 * predicados.add(e -> ErrorSeverity.ERROR.equals(e.getSeverity()));
 * predicados.add(e -> "EngineLiters".equalsIgnoreCase(e.getAttributeName()));
 * ComparisonResult combinado = filter.applyFilters(result, predicados);
 * }</pre>
 */
public class ReportFilter {

    /**
     * Filtra los errores y advertencias por nivel de severidad.
     *
     * @param result   resultado a filtrar; no puede ser null
     * @param severity nivel de severidad que deben tener los errores a conservar; no puede ser null
     * @return nuevo {@link ComparisonResult} con sólo los errores del severity indicado
     * @throws IllegalArgumentException si {@code result} o {@code severity} son null
     */
    public ComparisonResult filterBySeverity(ComparisonResult result, ErrorSeverity severity) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("ErrorSeverity no puede ser null");
        }
        return applyFilters(result,
            java.util.Collections.singletonList(e -> severity.equals(e.getSeverity())));
    }

    /**
     * Filtra los errores y advertencias por línea de producto (comparación
     * case-insensitive sobre {@link ValidationError#getProductLine()}).
     *
     * @param result      resultado a filtrar; no puede ser null
     * @param productLine nombre de la línea de producto a conservar; no puede ser null
     * @return nuevo {@link ComparisonResult} con sólo los errores cuya línea de
     *         producto coincide con el valor indicado
     * @throws IllegalArgumentException si {@code result} o {@code productLine} son null
     */
    public ComparisonResult filterByProductLine(ComparisonResult result, String productLine) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        if (productLine == null) {
            throw new IllegalArgumentException("productLine no puede ser null");
        }
        return applyFilters(result,
            java.util.Collections.singletonList(
                e -> productLine.equalsIgnoreCase(e.getProductLine())));
    }

    /**
     * Filtra los errores y advertencias por nombre de atributo (comparación
     * case-insensitive sobre {@link ValidationError#getAttributeName()}).
     *
     * @param result        resultado a filtrar; no puede ser null
     * @param attributeName nombre del atributo a conservar; no puede ser null
     * @return nuevo {@link ComparisonResult} con sólo los errores cuyo atributo
     *         coincide con el valor indicado
     * @throws IllegalArgumentException si {@code result} o {@code attributeName} son null
     */
    public ComparisonResult filterByAttributeName(ComparisonResult result, String attributeName) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        if (attributeName == null) {
            throw new IllegalArgumentException("attributeName no puede ser null");
        }
        return applyFilters(result,
            java.util.Collections.singletonList(
                e -> attributeName.equalsIgnoreCase(e.getAttributeName())));
    }

    /**
     * Aplica una cadena de predicados (AND lógico) sobre los errores y
     * advertencias del resultado, conservando sólo aquellos que superan
     * <strong>todos</strong> los predicados.
     *
     * @param result     resultado a filtrar; no puede ser null
     * @param predicates lista de predicados a aplicar; si está vacía, se devuelve
     *                   una copia del original con todos los errores
     * @return nuevo {@link ComparisonResult} con sólo los errores que satisfacen
     *         todos los predicados
     * @throws IllegalArgumentException si {@code result} o {@code predicates} son null
     */
    public ComparisonResult applyFilters(ComparisonResult result,
            List<Predicate<ValidationError>> predicates) {
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        if (predicates == null) {
            throw new IllegalArgumentException("La lista de predicados no puede ser null");
        }

        List<ValidationError> filteredErrors   = filter(result.getErrors(),   predicates);
        List<ValidationError> filteredWarnings = filter(result.getWarnings(), predicates);

        return buildFiltered(result, filteredErrors, filteredWarnings);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Filtra una lista de errores aplicando todos los predicados (AND lógico).
     */
    private List<ValidationError> filter(List<ValidationError> errors,
            List<Predicate<ValidationError>> predicates) {
        List<ValidationError> result = new ArrayList<>();
        for (ValidationError e : errors) {
            boolean passes = true;
            for (Predicate<ValidationError> p : predicates) {
                if (!p.test(e)) {
                    passes = false;
                    break;
                }
            }
            if (passes) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Construye un nuevo {@link ComparisonResult} conservando el nombre de
     * aplicación y catálogo del original, y recalculando los contadores de
     * atributos a partir del nuevo conjunto de errores filtrados.
     *
     * <p>El recálculo usa el mismo criterio que {@link com.validador.aces.comparison.Comparator}:
     * el número de atributos incumplidos equivale al conteo de errores con código
     * {@link AttributeValidator#MISSING_REQUIRED_ATTRIBUTE}. El total de atributos
     * se toma del original (el filtrado no cambia cuántos atributos requería la
     * línea de producto, solo cuáles se reportan).</p>
     */
    private ComparisonResult buildFiltered(ComparisonResult original,
            List<ValidationError> filteredErrors,
            List<ValidationError> filteredWarnings) {

        ComparisonResult filtered = new ComparisonResult(
            original.getApplicationName(),
            original.getCatalogName()
        );

        filtered.addErrors(filteredErrors);
        filtered.addErrors(filteredWarnings);   // addErrors clasifica por severity automáticamente

        // Recalcular contadores de atributos
        int totalAttrs   = original.getTotalAttributes();
        int missingCount = countMissingErrors(filteredErrors);
        filtered.setTotalAttributes(totalAttrs);
        filtered.setInvalidAttributes(missingCount);
        filtered.setValidAttributes(totalAttrs - missingCount);

        return filtered;
    }

    /**
     * Cuenta los errores con código {@link AttributeValidator#MISSING_REQUIRED_ATTRIBUTE}.
     */
    private int countMissingErrors(List<ValidationError> errors) {
        int count = 0;
        for (ValidationError e : errors) {
            if (AttributeValidator.MISSING_REQUIRED_ATTRIBUTE.equals(e.getCode())) {
                count++;
            }
        }
        return count;
    }
}