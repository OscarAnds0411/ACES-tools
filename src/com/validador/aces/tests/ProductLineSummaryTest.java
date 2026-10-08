package com.validador.aces.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.validador.aces.comparison.ComplianceCalculator;
import com.validador.aces.comparison.ProductLineSummary;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;

/** Resumen por línea de producto: requeridos x/y, opcionales y números de parte con faltantes. */
public final class ProductLineSummaryTest {

    private ProductLineSummaryTest() {}

    private static ComparisonResult result(String part, int required, int valid,
                                           String missingRequired, String... missingOptional) {
        ComparisonResult r = new ComparisonResult("app-" + part, "cat");
        r.setPartNumber(part);
        r.setTotalAttributes(required);
        r.setValidAttributes(valid);
        r.setInvalidAttributes(required - valid);
        r.setOptionalAttributeNames(Arrays.asList("Color", "Size"));
        r.setMissingOptionalAttributes(Arrays.asList(missingOptional));
        if (missingRequired != null) {
            r.addErrors(Collections.singletonList(new ValidationError(ErrorSeverity.ERROR,
                AttributeValidator.MISSING_REQUIRED_ATTRIBUTE, "falta", missingRequired, "Line")));
        }
        return r;
    }

    private static ProductLineSummary summarize(ComparisonResult... results) {
        Map<String, List<ComparisonResult>> by = new LinkedHashMap<>();
        by.put("Line", Arrays.asList(results));
        return new ComplianceCalculator().summarizeByProductLine(by).get("Line");
    }

    public static void testSummary_allRequiredPresent_fractionAndPercentage() {
        ProductLineSummary s = summarize(result("P1", 2, 2, null, "Color"), result("P2", 2, 2, null));
        Assert.assertEquals("4/4", s.getRequiredFraction(), "Fracción de requeridos en celdas");
        Assert.assertEquals("100.0%", s.getComplianceLabel(), "Porcentaje de requeridos");
        Assert.assertTrue(s.hasAllRequired(), "Contiene todos los requeridos");
    }

    public static void testSummary_partNumbersWithGaps_listedWithDetail() {
        ProductLineSummary s = summarize(
            result("P1", 2, 1, "EngineLiters", "Color"),
            result("P1", 2, 2, null, "Size"),          // mismo número de parte: se unen faltantes
            result("P2", 2, 2, null));
        Assert.assertEquals(2, s.getPartNumberCount(), "Números de parte distintos");
        Assert.assertEquals(1, s.getPartNumbersWithGaps().size(), "Solo P1 tiene faltantes");
        ProductLineSummary.PartNumberGap gap = s.getPartNumbersWithGaps().get("P1");
        Assert.assertTrue(gap.getMissingRequired().contains("EngineLiters"), "Requerido faltante");
        Assert.assertTrue(gap.getMissingOptional().contains("Color")
            && gap.getMissingOptional().contains("Size"), "Opcionales faltantes unidos");
        Assert.assertEquals("5/6", s.getRequiredFraction(), "Una celda requerida faltante de 6");
    }

    public static void testSummary_noRequired_isNotApplicable() {
        ProductLineSummary s = summarize(result("P1", 0, 0, null));
        Assert.assertEquals("N/A", s.getComplianceLabel(), "Sin requeridos es N/A");
        Assert.assertTrue(s.describe().contains("N/A"), "La frase indica N/A");
    }

    public static void testDescribe_mentionsLineOptionalsAndPartNumbers() {
        String text = summarize(result("P1", 1, 1, null, "Color")).describe();
        Assert.assertTrue(text.contains("\"Line\""), "Menciona la línea de producto");
        Assert.assertTrue(text.contains("contiene todos sus atributos requeridos (1/1, 100.0%)"), "Requeridos x/x%");
        Assert.assertTrue(text.contains("los opcionales son: Color, Size"), "Lista de opcionales");
        Assert.assertTrue(text.contains("a 1 de 1 numeros de parte"), "Números de parte con faltantes");
    }
}
