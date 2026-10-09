package com.validador.aces.comparison;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.validador.aces.models.Application;
import com.validador.aces.models.AuditAction;
import com.validador.aces.models.AuditReport;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ProductLine;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;
import com.validador.aces.validation.ValidationSchema;

/**
 * Compara {@link Application} contra un {@link Catalog} maestro, ejecutando
 * la cadena de validadores correspondiente y produciendo un
 * {@link ComparisonResult} con las estadísticas de cumplimiento y los
 * errores/advertencias detectados.
 *
 * <p>Expone dos modos de uso:</p>
 * <ul>
 *   <li>{@link #compare(Application, Catalog)}: comparación puntual de una
 *       sola aplicación. Utiliza directamente
 *       {@code Catalog.findProductByName(String)}, cuya búsqueda es lineal
 *       (O(n) sobre la lista de líneas de producto del catálogo). Para una
 *       sola aplicación esto es aceptable.</li>
 *   <li>{@link #compareAll(List, Catalog)}: comparación de un lote de
 *       aplicaciones (uso real esperado en, por ejemplo, la GUI del flujo
 *       de auditoría — TASK-030, donde se comparan decenas de miles de
 *       aplicaciones contra el mismo catálogo). Repetir la búsqueda lineal
 *       de {@code Catalog.findProductByName} por cada aplicación del lote
 *       sería prohibitivamente lento (del orden de miles de millones de
 *       comparaciones de strings para ~38,265 líneas de producto x ~81,886
 *       aplicaciones). Por eso {@code compareAll} construye internamente,
 *       una sola vez, un índice {@code Map<String, ProductLine>} (clave:
 *       nombre de producto en minúsculas) recorriendo
 *       {@code catalog.getProductLines()} una única vez, y lo usa para
 *       resolver la línea de producto de todas las aplicaciones del lote
 *       sin volver a recorrer la lista completa en cada iteración. No se
 *       modifica {@code Catalog} para lograr esto: el índice es un detalle
 *       interno y transitorio de esta clase. La línea y su esquema se
 *       resuelven <b>una sola vez</b> por aplicación y se pasan ya resueltos
 *       a {@link AttributeValidator#validateResolved}, de modo que el validador
 *       no vuelve a buscar en el catálogo ni reconstruye el esquema.</li>
 * </ul>
 *
 * <p>Si el catálogo tiene varias líneas con el mismo nombre (sin distinguir
 * mayúsculas ni espacios en los extremos), <b>gana la primera</b>, tanto en
 * {@code compare} como en {@code compareAll}.</p>
 *
 * <p>Cada invocación de {@code compare} o {@code compareAll} registra un
 * {@link AuditReport} accesible mediante {@link #getLastAuditReport()},
 * reemplazando el reporte de la invocación anterior.</p>
 */
public class Comparator {

    /** Sin estado: se comparte para no crear uno por aplicación. */
    private static final AttributeValidator ATTRIBUTE_VALIDATOR = new AttributeValidator();

    private final String user;
    private boolean stopOnFirstCriticalError = false;
    private AuditReport lastAuditReport;

    /**
     * Crea un comparador asociado al usuario "system", utilizado cuando no
     * se dispone de un usuario autenticado en el contexto de ejecución.
     */
    public Comparator() {
        this("system");
    }

    /**
     * Crea un comparador asociado al usuario indicado, utilizado para
     * registrar los reportes de auditoría de cada comparación.
     *
     * @param user usuario responsable de las comparaciones realizadas por esta instancia
     * @throws IllegalArgumentException si {@code user} es null o vacío
     */
    public Comparator(String user) {
        if (user == null || user.trim().isEmpty()) {
            throw new IllegalArgumentException("User no puede ser null ni vacío");
        }
        this.user = user;
    }

    /**
     * Configura si la cadena de validadores debe abortar en cuanto se produzca
     * el primer error crítico. Ver
     * {@code CompositeValidator.setStopOnFirstCriticalError(boolean)}.
     *
     * <p>Hoy solo se ejecuta {@link AttributeValidator}, un único validador, por
     * lo que esta opción no tiene efecto observable; se conserva por
     * compatibilidad de la API y para cuando se conecten más validadores.</p>
     *
     * @param value true para abortar ante el primer error crítico
     */
    public void setStopOnFirstCriticalError(boolean value) {
        this.stopOnFirstCriticalError = value;
    }

    /** @return true si la cadena de validadores aborta ante el primer error crítico. */
    public boolean isStopOnFirstCriticalError() {
        return stopOnFirstCriticalError;
    }

    /** @return el último {@link AuditReport} generado por una comparación, o null si aún no se ha comparado nada. */
    public AuditReport getLastAuditReport() {
        return lastAuditReport;
    }

    /**
     * Compara una sola aplicación contra el catálogo (uso puntual).
     *
     * <p>Resuelve la línea de producto mediante
     * {@code catalog.findProductByName(application.getProductName())}, cuya
     * búsqueda es lineal; aceptable para una sola comparación pero no para
     * lotes grandes (ver {@link #compareAll(List, Catalog)}).</p>
     *
     * @param application aplicación a comparar
     * @param catalog     catálogo maestro de referencia
     * @return resultado de la comparación con estadísticas y errores/advertencias
     * @throws IllegalArgumentException si {@code application} o {@code catalog} son null
     */
    public ComparisonResult compare(Application application, Catalog catalog) {
        if (application == null) {
            throw new IllegalArgumentException("Application no puede ser null");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog no puede ser null");
        }

        long start = System.currentTimeMillis();

        ProductLine line = catalog.findProductByName(application.getProductName());
        ValidationSchema schema = line != null ? new ValidationSchema(line) : null;
        ComparisonResult result = buildResult(application, catalog, line, schema);

        long duration = System.currentTimeMillis() - start;
        recordSingleAudit(application, result, duration);
        return result;
    }

    /**
     * Compara un lote de aplicaciones contra el catálogo de forma eficiente.
     *
     * <p>En lugar de invocar {@code Catalog.findProductByName} (O(n)) por
     * cada aplicación del lote, construye una sola vez un índice
     * {@code Map<String, ProductLine>} recorriendo
     * {@code catalog.getProductLines()} una única vez, y resuelve cada
     * aplicación del lote contra dicho índice (búsqueda O(1) amortizada).
     * Esto evita una complejidad O(n * m) (aplicaciones x líneas de
     * producto) que sería inviable para los volúmenes reales del sistema
     * (decenas de miles de aplicaciones y líneas de producto).</p>
     *
     * @param applications lista de aplicaciones a comparar
     * @param catalog      catálogo maestro de referencia
     * @return lista de resultados de comparación, en el mismo orden que {@code applications}
     * @throws IllegalArgumentException si {@code applications} o {@code catalog} son null
     */
    public List<ComparisonResult> compareAll(List<Application> applications, Catalog catalog) {
        if (applications == null) {
            throw new IllegalArgumentException("La lista de aplicaciones no puede ser null");
        }
        if (catalog == null) {
            throw new IllegalArgumentException("Catalog no puede ser null");
        }

        long start = System.currentTimeMillis();

        // Construir índices: productLine y ValidationSchema
        Map<String, ProductLine> productIndex = buildProductLineIndex(catalog);
        Map<String, ValidationSchema> schemaCache = buildValidationSchemaCache(productIndex);

        List<ComparisonResult> results = new ArrayList<>();
        for (Application application : applications) {
            String key = normalizeKey(application.getProductName());
            ProductLine line = key == null ? null : productIndex.get(key);
            ValidationSchema schema = key == null ? null : schemaCache.get(key);
            results.add(buildResult(application, catalog, line, schema));
        }

        long duration = System.currentTimeMillis() - start;
        recordBatchAudit(results, duration);
        return results;
    }

    /**
     * Construye el índice de líneas de producto por nombre normalizado
     * (minúsculas, sin espacios al borde), recorriendo
     * {@code catalog.getProductLines()} una sola vez.
     *
     * @param catalog catálogo cuyas líneas de producto se indexarán
     * @return mapa de nombre normalizado a {@link ProductLine}
     */
    private Map<String, ProductLine> buildProductLineIndex(Catalog catalog) {
        Map<String, ProductLine> index = new HashMap<>();
        for (ProductLine line : catalog.getProductLines()) {
            String key = normalizeKey(line.getName());
            if (key != null) {
                // Nombres duplicados: gana la primera línea (igual que Catalog.findProductByName)
                index.putIfAbsent(key, line);
            }
        }
        return index;
    }

    /**
     * Construye el índice de esquemas de validación precalculados, uno por cada
     * línea de producto encontrada. Esto evita recalcular el esquema para cada
     * aplicación en compareAll(), lo que es crucial para lotes grandes.
     *
     * @param productIndex índice de líneas de producto por nombre normalizado
     * @return mapa de nombre normalizado a {@link ValidationSchema}
     */
    private Map<String, ValidationSchema> buildValidationSchemaCache(Map<String, ProductLine> productIndex) {
        Map<String, ValidationSchema> cache = new HashMap<>();
        for (Map.Entry<String, ProductLine> entry : productIndex.entrySet()) {
            cache.put(entry.getKey(), new ValidationSchema(entry.getValue()));
        }
        return cache;
    }

    /**
     * Lógica común de {@code compare} y {@code compareAll}, una vez resuelta
     * (o no) la {@link ProductLine} de la aplicación. Valida con
     * {@link AttributeValidator#validateResolved} y calcula los contadores con
     * el <b>mismo</b> esquema, de modo que {@code 0 <= inválidos <= total}
     * siempre se cumple (los faltantes son un subconjunto de los requeridos
     * del esquema).
     *
     * @param application aplicación a comparar
     * @param catalog     catálogo maestro (solo aporta el nombre al resultado)
     * @param line        línea de producto ya resuelta, o null si no se encontró
     * @param schema      esquema de {@code line}, o null si {@code line} es null
     * @return resultado de la comparación
     */
    private ComparisonResult buildResult(Application application, Catalog catalog,
                                         ProductLine line, ValidationSchema schema) {
        List<ValidationError> allErrors = ATTRIBUTE_VALIDATOR.validateResolved(application, line, schema);

        ComparisonResult result = new ComparisonResult(application.getName(), catalog.getName());
        result.addErrors(allErrors);
        result.setPartNumber(application.getPartNumber());

        if (line != null) {
            int total = schema.getRequiredAttributes().size();
            int missingCount = countErrorsByCode(allErrors, AttributeValidator.MISSING_REQUIRED_ATTRIBUTE);
            result.setTotalAttributes(total);
            result.setInvalidAttributes(missingCount);
            result.setValidAttributes(total - missingCount);
            result.setOptionalAttributeNames(schema.getOptionalAttributeNames());
            result.setMissingOptionalAttributes(schema.findMissingOptionalAttributes(application));
        } else {
            result.setTotalAttributes(0);
            result.setValidAttributes(0);
            result.setInvalidAttributes(0);
        }

        return result;
    }

    private String normalizeKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private int countErrorsByCode(List<ValidationError> errors, String code) {
        int count = 0;
        for (ValidationError error : errors) {
            if (code.equals(error.getCode())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Registra el {@link AuditReport} correspondiente a una comparación
     * puntual de una sola aplicación.
     */
    private void recordSingleAudit(Application application, ComparisonResult result, long durationMillis) {
        String details = "Comparación de aplicación: " + application.getName();
        String resultSummary = String.format(
            "Compliance: %s, Errores: %d, Advertencias: %d",
            result.getComplianceLabel(), result.getErrorCount(), result.getWarningCount());

        AuditReport report = new AuditReport(user, AuditAction.COMPARISON, details, resultSummary);
        report.setDurationMillis(durationMillis);
        this.lastAuditReport = report;
    }

    /**
     * Registra el {@link AuditReport} correspondiente a una comparación por
     * lote, resumiendo totales de errores y advertencias sobre todos los
     * resultados producidos.
     */
    private void recordBatchAudit(List<ComparisonResult> results, long durationMillis) {
        int totalErrors = 0;
        int totalWarnings = 0;
        for (ComparisonResult r : results) {
            totalErrors += r.getErrorCount();
            totalWarnings += r.getWarningCount();
        }

        String details = "Comparación por lote de " + results.size() + " aplicaciones";
        String resultSummary = String.format("Total errores: %d, Total advertencias: %d", totalErrors, totalWarnings);

        AuditReport report = new AuditReport(user, AuditAction.COMPARISON, details, resultSummary);
        report.setDurationMillis(durationMillis);
        this.lastAuditReport = report;
    }
}
