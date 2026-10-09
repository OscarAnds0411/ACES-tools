package com.validador.aces.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.validador.aces.comparison.ComplianceCalculator;
import com.validador.aces.comparison.Comparator;
import com.validador.aces.models.Application;
import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ProductLine;
import com.validador.aces.models.ValidationError;

/**
 * Plan 004: el resultado de comparar una aplicación sola ({@code compare}) debe
 * ser idéntico al que produce {@code compareAll} para esa misma aplicación, y
 * los nombres de línea de producto duplicados deben resolverse de forma
 * coherente (la primera gana) en ambos caminos. Sin I/O de archivos.
 */
public final class ComparatorEquivalenceTest {

    private static final Comparator CMP = new Comparator("test-equivalence");

    private ComparatorEquivalenceTest() {}

    // ── Datos ─────────────────────────────────────────────────────────────

    /** "Test Product" (1 requerido, 1 opcional) y "Multi Product" (4 requeridos, 1 opcional). */
    private static Catalog buildCatalog() {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ProductLine multi = new ProductLine("2", "Multi Product", "Cat", "Sub", null);
        multi.addAttribute(new Attribute("Attr1", "Attr1", AttributeRequirement.REQUIRED));
        multi.addAttribute(new Attribute("Attr2", "Attr2", AttributeRequirement.REQUIRED));
        multi.addAttribute(new Attribute("Attr3", "Attr3", AttributeRequirement.REQUIRED));
        multi.addAttribute(new Attribute("Attr4", "Attr4", AttributeRequirement.REQUIRED));
        multi.addAttribute(new Attribute("Opt1",  "Opt1",  AttributeRequirement.OPTIONAL));
        cat.addProductLine(multi);
        return cat;
    }

    private static Application app(String product, String partNumber, String... keyValues) {
        Application a = new Application("Make", "Model", "2000", product);
        a.setPartNumber(partNumber);
        for (int i = 0; i + 1 < keyValues.length; i += 2) a.setAttributeValue(keyValues[i], keyValues[i + 1]);
        return a;
    }

    /** Aplicaciones de todos los casos relevantes, en un orden fijo. */
    private static List<Application> sampleApplications() {
        return Arrays.asList(
            ValidatorTestFixture.appAllPresent(),
            ValidatorTestFixture.appRequiredMissing(),
            ValidatorTestFixture.appOptionalMissing(),
            ValidatorTestFixture.appUnknownProduct(),
            app(null, "PN-NULL"),
            app("   ", "PN-BLANK"),
            app("  tEsT pRoDuCt ", "PN-CASE", "EngineLiters", "5.3"),
            app("Multi Product", "PN-MULTI", "Attr1", "a", "Attr2", "b"),
            app("Multi Product", null, "Attr1", "a", "Attr2", "b", "Attr3", "c", "Attr4", "d", "Opt1", "o"));
    }

    // ── Comparación campo a campo ─────────────────────────────────────────

    private static void assertSameResult(ComparisonResult expected, ComparisonResult actual, String label) {
        Assert.assertEquals(expected.getApplicationName(), actual.getApplicationName(), label + ": applicationName");
        Assert.assertEquals(expected.getTotalAttributes(), actual.getTotalAttributes(), label + ": total");
        Assert.assertEquals(expected.getValidAttributes(), actual.getValidAttributes(), label + ": válidos");
        Assert.assertEquals(expected.getInvalidAttributes(), actual.getInvalidAttributes(), label + ": inválidos");
        Assert.assertEquals(expected.getErrorCount(), actual.getErrorCount(), label + ": nº errores");
        Assert.assertEquals(expected.getWarningCount(), actual.getWarningCount(), label + ": nº advertencias");
        Assert.assertEquals(describe(expected.getErrors()), describe(actual.getErrors()), label + ": errores");
        Assert.assertEquals(describe(expected.getWarnings()), describe(actual.getWarnings()), label + ": advertencias");
        Assert.assertEquals(expected.getPartNumber(), actual.getPartNumber(), label + ": partNumber");
        Assert.assertEquals(expected.getOptionalAttributeNames(), actual.getOptionalAttributeNames(),
            label + ": opcionales");
        Assert.assertEquals(expected.getMissingOptionalAttributes(), actual.getMissingOptionalAttributes(),
            label + ": opcionales faltantes");
    }

    private static List<String> describe(List<ValidationError> errors) {
        List<String> out = new ArrayList<>();
        for (ValidationError e : errors) {
            out.add(e.getSeverity() + "|" + e.getCode() + "|" + e.getAttributeName() + "|" + e.getProductLine());
        }
        return out;
    }

    private static void assertCompareMatchesCompareAll(Application application, String label) {
        Catalog cat = buildCatalog();
        ComparisonResult single = CMP.compare(application, cat);
        ComparisonResult batch = CMP.compareAll(Arrays.asList(application), cat).get(0);
        assertSameResult(single, batch, label);
    }

    // ── Equivalencia compare / compareAll ─────────────────────────────────

    public static void testEquivalence_allRequiredPresent() {
        assertCompareMatchesCompareAll(ValidatorTestFixture.appAllPresent(), "todo presente");
    }

    public static void testEquivalence_requiredMissing() {
        assertCompareMatchesCompareAll(ValidatorTestFixture.appRequiredMissing(), "requerido faltante");
    }

    public static void testEquivalence_optionalMissing() {
        assertCompareMatchesCompareAll(ValidatorTestFixture.appOptionalMissing(), "opcional faltante");
    }

    public static void testEquivalence_unclassifiedProduct() {
        assertCompareMatchesCompareAll(ValidatorTestFixture.appUnknownProduct(), "producto desconocido");
    }

    public static void testEquivalence_blankProduct() {
        assertCompareMatchesCompareAll(app(null, "PN-NULL"), "producto null");
        assertCompareMatchesCompareAll(app("   ", "PN-BLANK"), "producto en blanco");
    }

    public static void testEquivalence_productNameCaseAndPadding() {
        assertCompareMatchesCompareAll(
            app("  tEsT pRoDuCt ", "PN-CASE", "EngineLiters", "5.3"), "mayúsculas y espacios");
    }

    public static void testEquivalence_multiRequired_partial() {
        assertCompareMatchesCompareAll(
            app("Multi Product", "PN-MULTI", "Attr1", "a", "Attr2", "b"), "multi requerido parcial");
    }

    /** El lote completo debe dar, en el mismo orden, lo mismo que comparar una por una. */
    public static void testEquivalence_wholeBatch_sameOrderAndValues() {
        Catalog cat = buildCatalog();
        List<Application> apps = sampleApplications();
        List<ComparisonResult> batch = CMP.compareAll(apps, cat);
        Assert.assertEquals(apps.size(), batch.size(), "1 resultado por aplicación");
        for (int i = 0; i < apps.size(); i++) {
            assertSameResult(CMP.compare(apps.get(i), cat), batch.get(i), "lote[" + i + "]");
        }
    }

    // ── Nombres de línea duplicados: la primera gana, en ambos caminos ────

    /** Primera "Dup": requiere A. Segunda "Dup": requiere A, B y C. */
    private static Catalog catalogWithDuplicateName_firstSmaller() {
        Catalog cat = new Catalog("DUP", "Dup Catalog");
        ProductLine first = new ProductLine("1", "Dup", "Cat", "Sub", null);
        first.addAttribute(new Attribute("A", "A", AttributeRequirement.REQUIRED));
        ProductLine second = new ProductLine("2", "Dup", "Cat", "Sub", null);
        second.addAttribute(new Attribute("A", "A", AttributeRequirement.REQUIRED));
        second.addAttribute(new Attribute("B", "B", AttributeRequirement.REQUIRED));
        second.addAttribute(new Attribute("C", "C", AttributeRequirement.REQUIRED));
        cat.addProductLine(first);
        cat.addProductLine(second);
        return cat;
    }

    /** Primera "Dup": requiere A, B y C. Segunda "Dup": requiere solo A. */
    private static Catalog catalogWithDuplicateName_firstLarger() {
        Catalog cat = new Catalog("DUP2", "Dup Catalog 2");
        ProductLine first = new ProductLine("1", "Dup", "Cat", "Sub", null);
        first.addAttribute(new Attribute("A", "A", AttributeRequirement.REQUIRED));
        first.addAttribute(new Attribute("B", "B", AttributeRequirement.REQUIRED));
        first.addAttribute(new Attribute("C", "C", AttributeRequirement.REQUIRED));
        ProductLine second = new ProductLine("2", "Dup", "Cat", "Sub", null);
        second.addAttribute(new Attribute("A", "A", AttributeRequirement.REQUIRED));
        cat.addProductLine(first);
        cat.addProductLine(second);
        return cat;
    }

    public static void testDuplicateName_firstWins_totalAndInvalidFromSameLine() {
        Catalog cat = catalogWithDuplicateName_firstSmaller();
        Application a = app("dup", "PN-D1", "B", "x", "C", "y"); // falta A

        ComparisonResult single = CMP.compare(a, cat);
        ComparisonResult batch = CMP.compareAll(Arrays.asList(a), cat).get(0);

        // Gana la primera línea ("Dup" con 1 requerido): total 1, falta 1, válidos 0
        Assert.assertEquals(1, single.getTotalAttributes(), "compare: total de la primera línea");
        Assert.assertEquals(1, single.getInvalidAttributes(), "compare: inválidos");
        Assert.assertEquals(0, single.getValidAttributes(), "compare: válidos");
        assertSameResult(single, batch, "duplicado (primera menor)");
    }

    public static void testDuplicateName_firstWins_neverNegativeValid_andMetricsDoNotThrow() {
        Catalog cat = catalogWithDuplicateName_firstLarger();
        Application a = app("DUP", "PN-D2"); // faltan A, B y C

        ComparisonResult single = CMP.compare(a, cat);
        ComparisonResult batch = CMP.compareAll(Arrays.asList(a), cat).get(0);

        Assert.assertEquals(3, batch.getTotalAttributes(), "compareAll: total de la primera línea");
        Assert.assertEquals(3, batch.getInvalidAttributes(), "compareAll: inválidos");
        Assert.assertEquals(0, batch.getValidAttributes(), "compareAll: válidos (nunca negativo)");
        assertSameResult(single, batch, "duplicado (primera mayor)");

        // ComplianceMetrics rechaza satisfechos < 0 o > total: no debe lanzar
        new ComplianceCalculator().calculate(batch);
    }
}
