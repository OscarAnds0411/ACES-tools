package com.validador.aces.tests;

import java.util.Arrays;
import java.util.List;

import com.validador.aces.comparison.Comparator;
import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;

/**
 * Tests de idempotencia de validación (TASK-045):
 * comparar la misma aplicación dos veces contra el mismo catálogo
 * debe producir resultados idénticos.
 */
public final class ValidationIdempotenceTest {

    private static final Comparator CMP = new Comparator("idempotence-test");

    private ValidationIdempotenceTest() {}

    // ── Test 1: comparar la misma app dos veces → resultados iguales ──────

    public static void testSingleCompare_calledTwice_sameResult() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        Application app = ValidatorTestFixture.appAllPresent();

        ComparisonResult r1 = CMP.compare(app, cat);
        ComparisonResult r2 = CMP.compare(app, cat);

        Assert.assertEquals(r1.getErrorCount(),   r2.getErrorCount(),
            "Error count debe ser idéntico en ambas llamadas");
        Assert.assertEquals(r1.getWarningCount(), r2.getWarningCount(),
            "Warning count debe ser idéntico en ambas llamadas");
        Assert.assertEquals(r1.getCompliancePercentage(), r2.getCompliancePercentage(), 0.001,
            "Compliance % debe ser idéntico en ambas llamadas");
        Assert.assertEquals(r1.getTotalAttributes(), r2.getTotalAttributes(),
            "Total attributes debe ser idéntico en ambas llamadas");
    }

    // ── Test 2: compareAll en la misma lista dos veces → mismos resultados ─

    public static void testCompareAll_calledTwice_sameResults() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<Application> apps = Arrays.asList(
            ValidatorTestFixture.appAllPresent(),
            ValidatorTestFixture.appRequiredMissing(),
            ValidatorTestFixture.appOptionalMissing()
        );

        List<ComparisonResult> run1 = CMP.compareAll(apps, cat);
        List<ComparisonResult> run2 = CMP.compareAll(apps, cat);

        Assert.assertEquals(run1.size(), run2.size(), "Tamaño de resultados debe ser idéntico");
        for (int i = 0; i < run1.size(); i++) {
            Assert.assertEquals(run1.get(i).getErrorCount(), run2.get(i).getErrorCount(),
                "Error count de app[" + i + "] debe ser idéntico");
            Assert.assertEquals(run1.get(i).getCompliancePercentage(),
                run2.get(i).getCompliancePercentage(), 0.001,
                "Compliance % de app[" + i + "] debe ser idéntico");
        }
    }

    // ── Test 3: aplicación NO se modifica tras la validación ──────────────

    public static void testApplicationNotModifiedAfterValidation() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        Application app = ValidatorTestFixture.appAllPresent();

        int attrCountBefore = app.getAttributeCount();
        String makeBefor     = app.getMake();

        CMP.compare(app, cat);

        Assert.assertEquals(attrCountBefore, app.getAttributeCount(),
            "El número de atributos de la aplicación no debe cambiar");
        Assert.assertEquals(makeBefor, app.getMake(),
            "El campo Make de la aplicación no debe cambiar");
    }

    // ── Test 4: catálogo NO se modifica tras la validación ────────────────

    public static void testCatalogNotModifiedAfterValidation() throws Exception {
        Catalog cat = ValidatorTestFixture.buildCatalog();
        int lineCountBefore = cat.getProductLineCount();
        String catalogNameBefore = cat.getName();

        CMP.compare(ValidatorTestFixture.appAllPresent(), cat);

        Assert.assertEquals(lineCountBefore, cat.getProductLineCount(),
            "El número de ProductLines del catálogo no debe cambiar");
        Assert.assertEquals(catalogNameBefore, cat.getName(),
            "El nombre del catálogo no debe cambiar");
    }
}