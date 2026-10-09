package com.validador.aces.tests;

import java.io.File;
import java.util.List;

import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;
import com.validador.aces.parsers.ExcelCatalogParser;
import com.validador.aces.parsers.ParseException;

/**
 * Tests de integración para {@link ExcelCatalogParser} (TASK-035).
 * Usan el archivo real del proyecto como fuente de datos.
 */
public final class CatalogParserTest {

    private static final String CATALOG_PATH =
        "catalogoDb/Atributos ACES por l\u00ednea de producto.xlsx";

    private static final int EXPECTED_MIN_PRODUCT_LINES = 38_000;
    private static final int EXPECTED_ATTRS_PER_LINE    = 42;

    private CatalogParserTest() {}

    // ── Test 1: parsear archivo válido ────────────────────────────────────

    public static void testParseValidFile_returnsNonEmptyCatalog() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        Assert.assertNotNull(catalog, "El catálogo no debe ser null");
        Assert.assertTrue(catalog.getProductLineCount() >= EXPECTED_MIN_PRODUCT_LINES,
            "Se esperaban ≥ " + EXPECTED_MIN_PRODUCT_LINES + " ProductLines, obtuvo: "
                + catalog.getProductLineCount());
    }

    // ── Test 2: lanzar excepción con archivo inexistente ──────────────────

    public static void testParseNonExistentFile_throwsParseException() {
        Assert.assertThrows(ParseException.class,
            () -> new ExcelCatalogParser().parse("no_existe_en_absoluto.xlsx"),
            "Debe lanzar ParseException para archivo inexistente");
    }

    // ── Test 3: lanzar excepción con hoja incorrecta ──────────────────────

    public static void testParseWrongSheetName_throwsParseException() {
        Assert.assumeFileExists(CATALOG_PATH);
        Assert.assertThrows(ParseException.class,
            () -> new ExcelCatalogParser().parse(new File(CATALOG_PATH), "HojaQueNoExiste"),
            "Debe lanzar ParseException para hoja inexistente");
    }

    // ── Test 4: validar estructura — número de atributos por línea ────────

    public static void testStructure_attributeCountPerProductLine() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        ProductLine first = catalog.getProductLines().get(0);
        Assert.assertEquals(EXPECTED_ATTRS_PER_LINE, first.getAttributeCount(),
            "Se esperaban " + EXPECTED_ATTRS_PER_LINE + " atributos en la primera ProductLine");
    }

    // ── Test 5: validar estructura — todos los atributos tienen requirement ─

    public static void testStructure_allAttributesHaveValidRequirement() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        // Verificar las primeras 100 líneas para no tardar demasiado
        int checked = 0;
        for (ProductLine pl : catalog.getProductLines()) {
            for (Attribute attr : pl.getAttributes()) {
                Assert.assertNotNull(attr.getRequirement(),
                    "Atributo sin requirement: " + attr.getName() + " en " + pl.getName());
                Assert.assertTrue(
                    attr.getRequirement() == AttributeRequirement.REQUIRED
                    || attr.getRequirement() == AttributeRequirement.OPTIONAL
                    || attr.getRequirement() == AttributeRequirement.NOT_REQUIRED,
                    "Requirement inválido: " + attr.getRequirement());
            }
            if (++checked >= 100) break;
        }
    }

    // ── Test 6: readSheetNames incluye la hoja esperada ───────────────────

    public static void testReadSheetNames_includesExpectedSheet() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        List<String> sheets = ExcelCatalogParser.readSheetNames(new File(CATALOG_PATH));
        Assert.assertTrue(sheets.contains(ExcelCatalogParser.DEFAULT_SHEET_NAME),
            "La lista de hojas debe contener '" + ExcelCatalogParser.DEFAULT_SHEET_NAME + "'");
    }

    // ── Test 7: parse con nombre de hoja explícito ────────────────────────

    public static void testParseWithExplicitSheetName_works() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        Catalog catalog = new ExcelCatalogParser()
            .parse(new File(CATALOG_PATH), ExcelCatalogParser.DEFAULT_SHEET_NAME);
        Assert.assertTrue(catalog.getProductLineCount() >= EXPECTED_MIN_PRODUCT_LINES,
            "Parse con hoja explícita debe devolver el mismo catálogo completo");
    }

    // ── Test 8: findProductByName funciona correctamente ─────────────────

    public static void testCatalog_findProductByName_caseInsensitive() throws Exception {
        Assert.assumeFileExists(CATALOG_PATH);
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        ProductLine first = catalog.getProductLines().get(0);
        String name = first.getName();
        ProductLine found = catalog.findProductByName(name.toUpperCase());
        Assert.assertNotNull(found, "findProductByName debe ser case-insensitive");
        Assert.assertEquals(name, found.getName(), "El nombre debe coincidir");
    }
}