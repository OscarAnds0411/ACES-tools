package com.validador.aces.tests;

import java.util.Arrays;
import java.util.List;

import com.validador.aces.comparison.Comparator;
import com.validador.aces.models.Application;
import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ProductLine;
import com.validador.aces.validation.AttributeValidator;

/**
 * Unit tests para {@link Comparator} (TASK-043).
 * Construye datos en memoria; sin I/O de archivo.
 */
public final class ComparatorTest {

    private static final Comparator CMP = new Comparator("test");

    private ComparatorTest() {}

    // ── Catálogo con 4 atributos requeridos (para tests de compliance %) ──

    private static Catalog buildMultiRequiredCatalog() {
        Catalog cat = new Catalog("MULTI", "Multi Required");
        ProductLine pl = new ProductLine("1", "Multi Product", "Cat", "Sub", null);
        pl.addAttribute(new Attribute("Attr1", "Attr1", AttributeRequirement.REQUIRED));
        pl.addAttribute(new Attribute("Attr2", "Attr2", AttributeRequirement.REQUIRED));
        pl.addAttribute(new Attribute("Attr3", "Attr3", AttributeRequirement.REQUIRED));
        pl.addAttribute(new Attribute("Attr4", "Attr4", AttributeRequirement.REQUIRED));
        cat.addProductLine(pl);
        return cat;
    }

    private static Application appWithAttrs(String product, String... keyValues) {
        Application app = new Application("Make", "Model", "2000", product);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            app.setAttributeValue(keyValues[i], keyValues[i + 1]);
        }
        return app;
    }

    // ── Test 1: aplicación válida contra catálogo → 0 errores ─────────────

    public static void testCompareValidApplication_noErrors() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ComparisonResult result = CMP.compare(ValidatorTestFixture.appAllPresent(), cat);
        Assert.assertEquals(0, result.getErrorCount(),
            "Aplicación válida → 0 errores");
        Assert.assertEquals(0, result.getWarningCount(),
            "Aplicación válida → 0 advertencias");
    }

    // ── Test 2: atributo requerido faltante → detecta error ───────────────

    public static void testMissingRequiredAttribute_detected() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ComparisonResult result = CMP.compare(ValidatorTestFixture.appRequiredMissing(), cat);
        Assert.assertEquals(1, result.getErrorCount(),
            "Un atributo requerido faltante → 1 error");
        Assert.assertEquals(AttributeValidator.MISSING_REQUIRED_ATTRIBUTE,
            result.getErrors().get(0).getCode(),
            "Código del error debe ser MISSING_REQUIRED_ATTRIBUTE");
    }

    // ── Test 3: compliance 100 % ───────────────────────────────────────────

    public static void testCompliancePercentage_100percent() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ComparisonResult result = CMP.compare(ValidatorTestFixture.appAllPresent(), cat);
        Assert.assertEquals(100.0, result.getCompliancePercentage(), 0.001,
            "Con todo presente → compliance 100 %");
    }

    // ── Test 4: compliance 0 % ────────────────────────────────────────────

    public static void testCompliancePercentage_0percent() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ComparisonResult result = CMP.compare(ValidatorTestFixture.appRequiredMissing(), cat);
        Assert.assertEquals(0.0, result.getCompliancePercentage(), 0.001,
            "Atributo requerido faltante → compliance 0 %");
    }

    // ── Test 5: compliance 50 % ───────────────────────────────────────────

    public static void testCompliancePercentage_50percent() throws Exception {
        Catalog cat = buildMultiRequiredCatalog();
        // Solo Attr1 y Attr2 presentes (2 de 4)
        Application app = appWithAttrs("Multi Product",
            "Attr1", "val1", "Attr2", "val2");
        ComparisonResult result = CMP.compare(app, cat);
        Assert.assertEquals(50.0, result.getCompliancePercentage(), 0.001,
            "2 de 4 atributos requeridos → compliance 50 %");
    }

    // ── Test 6: compareAll en modo lote ───────────────────────────────────

    public static void testCompareAll_batchMode() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<Application> apps = Arrays.asList(
            ValidatorTestFixture.appAllPresent(),
            ValidatorTestFixture.appRequiredMissing(),
            ValidatorTestFixture.appOptionalMissing()
        );
        List<ComparisonResult> results = CMP.compareAll(apps, cat);
        Assert.assertEquals(3, results.size(),
            "compareAll debe retornar 1 resultado por aplicación");
        Assert.assertEquals(0, results.get(0).getErrorCount(),
            "App[0] válida → 0 errores");
        Assert.assertEquals(1, results.get(1).getErrorCount(),
            "App[1] con faltante → 1 error");
        Assert.assertEquals(0, results.get(2).getErrorCount(),
            "App[2] solo opcional faltante → 0 errores");
    }

    // ── Test 7: aplicación sin clasificar → WARNING ───────────────────────

    public static void testUnclassifiedApplication_generatesWarning() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        ComparisonResult result = CMP.compare(ValidatorTestFixture.appUnknownProduct(), cat);
        Assert.assertEquals(0, result.getErrorCount(),
            "Producto desconocido → 0 errores (solo warning)");
        Assert.assertEquals(1, result.getWarningCount(),
            "Producto desconocido → 1 warning PRODUCT_LINE_NOT_FOUND");
        Assert.assertEquals(AttributeValidator.PRODUCT_LINE_NOT_FOUND,
            result.getWarnings().get(0).getCode(),
            "Código del warning debe ser PRODUCT_LINE_NOT_FOUND");
    }

    // ── Test 8: compare con null app → IllegalArgumentException ───────────

    public static void testNullApplication_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new Comparator().compare(null, ValidatorTestFixture.buildCatalog()),
            "Application null debe lanzar IAE");
    }

    // ── Test 9: compare con null catalog → IllegalArgumentException ────────

    public static void testNullCatalog_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new Comparator().compare(ValidatorTestFixture.appAllPresent(), null),
            "Catalog null debe lanzar IAE");
    }
}