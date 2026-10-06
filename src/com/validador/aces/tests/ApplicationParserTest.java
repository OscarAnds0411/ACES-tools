package com.validador.aces.tests;

import java.io.File;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.parsers.ExcelApplicationParser;
import com.validador.aces.parsers.ParseException;

/**
 * Tests de integración para {@link ExcelApplicationParser} (TASK-036).
 * Usan el archivo ACES real del proyecto como fuente de datos.
 */
public final class ApplicationParserTest {

    private static final String ACES_PATH =
        "ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx";

    private static final int EXPECTED_MIN_APPS = 81_000;

    private ApplicationParserTest() {}

    // ── Test 1: parsear archivo válido ────────────────────────────────────

    public static void testParseValidFile_returnsNonEmptyList() throws Exception {
        List<Application> apps = new ExcelApplicationParser().parse(ACES_PATH);
        Assert.assertNotNull(apps, "La lista no debe ser null");
        Assert.assertTrue(apps.size() >= EXPECTED_MIN_APPS,
            "Se esperaban ≥ " + EXPECTED_MIN_APPS + " aplicaciones, obtuvo: " + apps.size());
    }

    // ── Test 2: lanzar excepción con archivo inexistente ──────────────────

    public static void testParseNonExistentFile_throwsParseException() {
        Assert.assertThrows(ParseException.class,
            () -> new ExcelApplicationParser().parse("archivo_inexistente.xlsx"),
            "Debe lanzar ParseException para archivo inexistente");
    }

    // ── Test 3: lanzar excepción con hoja incorrecta ──────────────────────

    public static void testParseWrongSheetName_throwsParseException() {
        Assert.assertThrows(ParseException.class,
            () -> new ExcelApplicationParser().parse(new File(ACES_PATH), "HojaFantasma"),
            "Debe lanzar ParseException para hoja inexistente");
    }

    // ── Test 4: metadata de la primera aplicación ─────────────────────────

    public static void testFirstApplication_hasMakeAndModel() throws Exception {
        List<Application> apps = new ExcelApplicationParser().parse(ACES_PATH);
        Application first = apps.get(0);
        Assert.assertNotNull(first.getMake(),  "Make no debe ser null");
        Assert.assertNotNull(first.getModel(), "Model no debe ser null");
        Assert.assertNotNull(first.getYear(),  "Year no debe ser null");
        Assert.assertNotNull(first.getProduct(), "Product (clave de catálogo) no debe ser null");
    }

    // ── Test 5: celdas vacías preservadas como null ───────────────────────

    public static void testEmptyCells_preservedAsNull() throws Exception {
        List<Application> apps = new ExcelApplicationParser().parse(ACES_PATH);
        // Buscar alguna aplicación que tenga al menos un atributo null
        boolean foundNull = false;
        for (int i = 0; i < Math.min(1000, apps.size()); i++) {
            Application app = apps.get(i);
            for (Object val : app.getData().values()) {
                if (val == null) { foundNull = true; break; }
            }
            if (foundNull) break;
        }
        Assert.assertTrue(foundNull,
            "Debe existir al menos una celda vacía preservada como null entre las primeras 1000 apps");
    }

    // ── Test 6: readSheetNames incluye la hoja esperada ───────────────────

    public static void testReadSheetNames_includesApplicationsSheet() throws Exception {
        List<String> sheets = ExcelApplicationParser.readSheetNames(new File(ACES_PATH));
        Assert.assertTrue(sheets.contains(ExcelApplicationParser.DEFAULT_SHEET_NAME),
            "La lista de hojas debe contener '" + ExcelApplicationParser.DEFAULT_SHEET_NAME + "'");
    }

    // ── Test 7: parse con hoja explícita ─────────────────────────────────

    public static void testParseWithExplicitSheetName_works() throws Exception {
        List<Application> apps = new ExcelApplicationParser()
            .parse(new File(ACES_PATH), ExcelApplicationParser.DEFAULT_SHEET_NAME);
        Assert.assertTrue(apps.size() >= EXPECTED_MIN_APPS,
            "Parse con hoja explícita debe devolver la misma cantidad de aplicaciones");
    }

    // ── Test 8: número de atributos técnicos razonable ───────────────────

    public static void testApplication_hasReasonableAttributeCount() throws Exception {
        List<Application> apps = new ExcelApplicationParser().parse(ACES_PATH);
        Application first = apps.get(0);
        // El ACES real tiene 94 - 7 = 87 columnas de atributos técnicos
        Assert.assertTrue(first.getAttributeCount() >= 80,
            "Debe tener ≥ 80 atributos técnicos, tiene: " + first.getAttributeCount());
    }
}