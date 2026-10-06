package com.validador.aces.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.reporting.ExcelReportGenerator;
import com.validador.aces.reporting.Report;
import com.validador.aces.reporting.ReportSection;
import com.validador.aces.validation.AttributeValidator;

/**
 * Tests de completitud del reporte (TASK-047):
 * verifica que el reporte contiene todos los errores del ComparisonResult.
 */
public final class ReportCompletenessTest {

    private ReportCompletenessTest() {}

    // ── Utilidad: crear N errores MISSING_REQUIRED_ATTRIBUTE ──────────────

    private static ComparisonResult resultWithNErrors(int n) {
        ComparisonResult r = new ComparisonResult("App", "Catalog");
        r.setTotalAttributes(n);
        r.setValidAttributes(0);
        r.setInvalidAttributes(n);
        List<ValidationError> errors = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            errors.add(new ValidationError(
                ErrorSeverity.ERROR,
                AttributeValidator.MISSING_REQUIRED_ATTRIBUTE,
                "Falta Attr" + i, "Attr" + i, "Product"));
        }
        r.addErrors(errors);
        return r;
    }

    // ── Test 1: reporte con N errores contiene N filas en la sección ──────

    public static void testReport_containsAllErrors() throws Exception {
        int N = 5;
        ComparisonResult result = resultWithNErrors(N);
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Report report = gen.generate(result);
        ReportSection errorsSection = report.findSectionByTitle("Atributos Faltantes");
        Assert.assertNotNull(errorsSection, "La sección 'Atributos Faltantes' debe existir");
        Assert.assertEquals(N, errorsSection.getRowCount(),
            N + " errores deben producir " + N + " filas en la sección");
    }

    // ── Test 2: todos los atributos faltantes aparecen en la sección ──────

    public static void testReport_allAttributeNamesPresent() throws Exception {
        List<String> expectedAttrs = Arrays.asList("EngineLiters", "VehicleType", "Region");
        ComparisonResult r = new ComparisonResult("App", "Cat");
        r.setTotalAttributes(3);
        r.setInvalidAttributes(3);
        r.setValidAttributes(0);
        List<ValidationError> errs = new ArrayList<>();
        for (String attr : expectedAttrs) {
            errs.add(new ValidationError(ErrorSeverity.ERROR,
                AttributeValidator.MISSING_REQUIRED_ATTRIBUTE,
                "Falta " + attr, attr, "TestProduct"));
        }
        r.addErrors(errs);

        Report report = new ExcelReportGenerator().generate(r);
        ReportSection section = report.findSectionByTitle("Atributos Faltantes");
        Assert.assertNotNull(section, "Sección de atributos faltantes debe existir");

        // Verificar que cada atributo aparece en alguna fila (columna 2 = Atributo)
        for (String expected : expectedAttrs) {
            boolean found = section.getRows().stream()
                .anyMatch(row -> row.size() > 2 && expected.equals(row.get(2)));
            Assert.assertTrue(found,
                "El atributo '" + expected + "' debe aparecer en la sección del reporte");
        }
    }

    // ── Test 3: 0 errores → "Completo" en la sección ─────────────────────

    public static void testReport_zeroErrors_showsCompleteMessage() throws Exception {
        ComparisonResult r = new ComparisonResult("App", "Cat");
        r.setTotalAttributes(3);
        r.setValidAttributes(3);
        r.setInvalidAttributes(0);

        Report report = new ExcelReportGenerator().generate(r);
        ReportSection section = report.findSectionByTitle("Atributos Faltantes");
        Assert.assertNotNull(section, "La sección debe existir aunque no haya errores");
        Assert.assertEquals(1, section.getRowCount(),
            "0 errores → 1 fila con el mensaje 'Sin problemas'");
        Assert.assertEquals("INFO", section.getRows().get(0).get(0),
            "La fila del mensaje debe tener Severidad=INFO");
    }

    // ── Test 4: compliance % en resumen coincide con el resultado ─────────

    public static void testReport_compliancePercentageMatchesResult() throws Exception {
        ComparisonResult r = new ComparisonResult("App", "Cat");
        r.setTotalAttributes(4);
        r.setValidAttributes(3);
        r.setInvalidAttributes(1);
        r.addErrors(Collections.singletonList(
            new ValidationError(ErrorSeverity.ERROR,
                AttributeValidator.MISSING_REQUIRED_ATTRIBUTE, "Falta Attr1", "Attr1", "Product")));

        Report report = new ExcelReportGenerator().generate(r);
        ReportSection summary = report.findSectionByTitle("Resumen Ejecutivo");
        Assert.assertNotNull(summary, "Sección 'Resumen Ejecutivo' debe existir");

        // Buscar la fila de Compliance (col 0 = "Compliance")
        String complianceValue = summary.getRows().stream()
            .filter(row -> "Compliance".equals(row.get(0)))
            .map(row -> row.get(1))
            .findFirst().orElse(null);
        Assert.assertNotNull(complianceValue, "La fila de Compliance debe existir en el resumen");
        Assert.assertEquals("75.0%", complianceValue,
            "3/4 atributos presentes → Compliance = 75.0%");
    }

    // ── Test 5: warnings también aparecen en la sección ──────────────────

    public static void testReport_warningsAlsoAppearInSection() throws Exception {
        ComparisonResult r = new ComparisonResult("App", "Cat");
        r.setTotalAttributes(0);
        r.setValidAttributes(0);
        r.setInvalidAttributes(0);
        r.addErrors(Collections.singletonList(
            new ValidationError(ErrorSeverity.WARNING,
                AttributeValidator.PRODUCT_LINE_NOT_FOUND,
                "Producto no encontrado", null, "Unknown Product")));

        Report report = new ExcelReportGenerator().generate(r);
        ReportSection section = report.findSectionByTitle("Atributos Faltantes");
        Assert.assertEquals(1, section.getRowCount(),
            "1 warning debe producir 1 fila en la sección");
        Assert.assertEquals("WARNING", section.getRows().get(0).get(0),
            "La fila debe mostrar Severidad=WARNING");
    }

    // ── Test 6: resumen contiene métricas correctas ───────────────────────

    public static void testReport_summaryContainsCorrectMetrics() throws Exception {
        ComparisonResult r = new ComparisonResult("MyApp", "MyCatalog");
        r.setTotalAttributes(10);
        r.setValidAttributes(8);
        r.setInvalidAttributes(2);

        Report report = new ExcelReportGenerator().generate(r);
        ReportSection summary = report.findSectionByTitle("Resumen Ejecutivo");
        Assert.assertNotNull(summary, "Sección de resumen debe existir");

        // Buscar valor de "Aplicación"
        String appName = summary.getRows().stream()
            .filter(row -> "Aplicaci\u00f3n".equals(row.get(0)))
            .map(row -> row.get(1))
            .findFirst().orElse(null);
        Assert.assertEquals("MyApp", appName,
            "El nombre de aplicación en el resumen debe coincidir con el ComparisonResult");

        // Buscar total de errores
        String totalErrors = summary.getRows().stream()
            .filter(row -> "Total de errores".equals(row.get(0)))
            .map(row -> row.get(1))
            .findFirst().orElse(null);
        Assert.assertEquals("0", totalErrors,
            "Total de errores en el resumen debe ser 0 (no hay ValidationErrors en el resultado)");
    }
}