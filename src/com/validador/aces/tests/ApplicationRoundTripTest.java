package com.validador.aces.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.validador.aces.models.Application;
import com.validador.aces.parsers.ExcelApplicationParser;

/**
 * Tests de round-trip para el archivo ACES (TASK-038):
 * escribe aplicaciones a Excel con fastexcel, las lee de nuevo con
 * {@link ExcelApplicationParser} y verifica equivalencia de estructura.
 */
public final class ApplicationRoundTripTest {

    private ApplicationRoundTripTest() {}

    // ── Test 1: metadata de aplicación preservada ─────────────────────────

    public static void testRoundTrip_applicationMetadataPreserved() throws Exception {
        File tmp = writeTmpAces("roundtrip_app_meta", ExcelApplicationParser.DEFAULT_SHEET_NAME);
        try {
            List<Application> apps = new ExcelApplicationParser().parse(tmp);
            Assert.assertEquals(2, apps.size(), "Debe haber 2 aplicaciones");

            Application a0 = apps.get(0);
            Assert.assertEquals("Chevrolet",     a0.getMake(),    "Make de app[0]");
            Assert.assertEquals("Silverado 1500", a0.getModel(),  "Model de app[0]");
            Assert.assertEquals("2000",           a0.getYear(),   "Year de app[0]");
            Assert.assertEquals("A/C Condenser",  a0.getProductName(), "Product de app[0]");
            Assert.assertEquals("PART-001",       a0.getPartNumber(), "PartNumber de app[0]");
            Assert.assertEquals("Front",          a0.getPosition(),   "Position de app[0]");

            Application a1 = apps.get(1);
            Assert.assertEquals("Ford",    a1.getMake(),  "Make de app[1]");
            Assert.assertEquals("F-150",   a1.getModel(), "Model de app[1]");
        } finally { tmp.delete(); }
    }

    // ── Test 2: valores de atributos técnicos preservados ─────────────────

    public static void testRoundTrip_attributeValuesPreserved() throws Exception {
        File tmp = writeTmpAces("roundtrip_app_attrs", ExcelApplicationParser.DEFAULT_SHEET_NAME);
        try {
            List<Application> apps = new ExcelApplicationParser().parse(tmp);
            Application a0 = apps.get(0);
            Assert.assertEquals("5.3", a0.getAttributeValue("EngineLiters"),
                "EngineLiters de app[0]");
            Assert.assertEquals("SUV", a0.getAttributeValue("VehicleType"),
                "VehicleType de app[0]");
        } finally { tmp.delete(); }
    }

    // ── Test 3: celdas vacías preservadas como null ───────────────────────

    public static void testRoundTrip_emptyCellsPreservedAsNull() throws Exception {
        File tmp = writeTmpAces("roundtrip_app_null", ExcelApplicationParser.DEFAULT_SHEET_NAME);
        try {
            List<Application> apps = new ExcelApplicationParser().parse(tmp);
            Application a1 = apps.get(1);
            // MfrLabel vacío en app[1] → null
            Assert.assertNull(a1.getMfrLabel(),
                "MfrLabel vacío debe preservarse como null");
            // EngineLiters vacío en app[1] → null
            Assert.assertNull(a1.getAttributeValue("EngineLiters"),
                "Atributo técnico vacío debe preservarse como null");
        } finally { tmp.delete(); }
    }

    // ── Test 4: nombre de hoja personalizado ─────────────────────────────

    public static void testRoundTrip_customSheetName_works() throws Exception {
        File tmp = writeTmpAces("roundtrip_app_sheet", "CustomACES");
        try {
            List<Application> apps = new ExcelApplicationParser().parse(tmp, "CustomACES");
            Assert.assertEquals(2, apps.size(),
                "Debe parsear correctamente con hoja personalizada");
        } finally { tmp.delete(); }
    }

    // ── Test 5: columnas por nombre de encabezado, no por posición ────────

    public static void testParse_columnsResolvedByHeaderName() throws Exception {
        File tmp = writeTmpReorderedAces("byname");
        try {
            List<Application> apps = new ExcelApplicationParser().parse(tmp, "Applications");
            Assert.assertEquals(1, apps.size(), "Debe parsear la única aplicación");
            Application a = apps.get(0);
            Assert.assertEquals("Ford", a.getMake(), "Make se resuelve por nombre aunque no esté en col 0");
            Assert.assertEquals("F-150", a.getModel(), "Model resuelto por nombre");
            Assert.assertEquals("5.0", a.getAttributeValue("EngineLiters"), "Atributo resuelto por nombre");
        } finally { tmp.delete(); }
    }

    // ── Test 6: solo se leen los encabezados de interés ───────────────────

    public static void testParse_headersOfInterest_ignoresOtherColumns() throws Exception {
        File tmp = writeTmpReorderedAces("interest");
        try {
            java.util.Set<String> interest = new java.util.HashSet<>();
            interest.add("enginelitERS"); // coincide sin distinguir mayúsculas
            List<Application> apps = new ExcelApplicationParser().parse(tmp, "Applications", interest);
            Application a = apps.get(0);
            Assert.assertEquals("5.0", a.getAttributeValue("enginelitERS"),
                "La columna de interés se guarda con el nombre canónico del catálogo");
            Assert.assertTrue(!a.hasAttribute("Color"), "Columnas fuera de interés se ignoran");
            Assert.assertEquals(1, a.getAttributeCount(), "Solo 1 atributo de interés");
        } finally { tmp.delete(); }
    }

    // ── Test 7: solo se procesa la hoja elegida ───────────────────────────

    public static void testParse_otherSheetsIgnored() throws Exception {
        File tmp = writeTmpReorderedAces("sheets");
        try {
            // La hoja "Otra" tiene un formato inválido; no debe afectar si no se elige
            List<Application> apps = new ExcelApplicationParser().parse(tmp, "Applications");
            Assert.assertEquals(1, apps.size(), "La hoja elegida se parsea sin tocar las demás");
        } finally { tmp.delete(); }
    }

    // ── Test 8: hoja sin encabezados obligatorios se rechaza ──────────────

    public static void testParse_missingRequiredHeader_throws() throws Exception {
        File tmp = writeTmpReorderedAces("badsheet");
        try {
            boolean threw = false;
            try { new ExcelApplicationParser().parse(tmp, "Otra"); }
            catch (com.validador.aces.parsers.ParseException e) { threw = true; }
            Assert.assertTrue(threw, "Una hoja sin Make/Model/Year/Product debe rechazarse");
        } finally { tmp.delete(); }
    }

    /** Excel con columnas desordenadas, columnas extra y una segunda hoja con formato inválido. */
    static File writeTmpReorderedAces(String prefix) throws Exception {
        File tmp = File.createTempFile(prefix + "_", ".xlsx");
        tmp.deleteOnExit();
        try (OutputStream os = new FileOutputStream(tmp)) {
            Workbook wb = new Workbook(os, "test", "1.0");
            Worksheet ws = wb.newWorksheet("Applications");
            String[] h = {"Notas", "Color", "Year", "EngineLiters", "Make", "Product", "Model"};
            String[] v = {"x", "Rojo", "2005", "5.0", "Ford", "Belt Drive", "F-150"};
            for (int i = 0; i < h.length; i++) { ws.value(0, i, h[i]); ws.value(1, i, v[i]); }
            Worksheet other = wb.newWorksheet("Otra");
            other.value(0, 0, "Algo");
            other.value(0, 1, "Distinto");
            other.value(1, 0, "1");
            wb.finish();
        }
        return tmp;
    }

    // ── Utilidad: escribe un Excel ACES de prueba ─────────────────────────

    /**
     * Escribe un Excel con la estructura que espera {@link ExcelApplicationParser}:
     * <ul>
     *   <li>Fila 0: Make, Model, Year, Product, PartNumber, MfrLabel, Position, Atributos...</li>
     *   <li>Fila 1: Chevrolet, Silverado (con todos los campos)</li>
     *   <li>Fila 2: Ford, F-150 (con MfrLabel y EngineLiters vacíos)</li>
     * </ul>
     */
    static File writeTmpAces(String prefix, String sheetName) throws Exception {
        File tmp = File.createTempFile(prefix + "_", ".xlsx");
        tmp.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tmp)) {
            Workbook wb = new Workbook(os, "test", "1.0");
            Worksheet ws = wb.newWorksheet(sheetName);

            // Encabezados (columnas 0-8)
            ws.value(0, 0, "Make");
            ws.value(0, 1, "Model");
            ws.value(0, 2, "Year");
            ws.value(0, 3, "Product");
            ws.value(0, 4, "PartNumber");
            ws.value(0, 5, "MfrLabel");
            ws.value(0, 6, "Position");
            ws.value(0, 7, "EngineLiters");   // primer atributo técnico
            ws.value(0, 8, "VehicleType");    // segundo atributo técnico

            // Fila 1: Chevrolet — todos los campos presentes
            ws.value(1, 0, "Chevrolet");
            ws.value(1, 1, "Silverado 1500");
            ws.value(1, 2, "2000");
            ws.value(1, 3, "A/C Condenser");
            ws.value(1, 4, "PART-001");
            ws.value(1, 5, "LabelA");
            ws.value(1, 6, "Front");
            ws.value(1, 7, "5.3");            // EngineLiters
            ws.value(1, 8, "SUV");            // VehicleType

            // Fila 2: Ford — MfrLabel (col 5) y EngineLiters (col 7) intencionalmente vacíos
            ws.value(2, 0, "Ford");
            ws.value(2, 1, "F-150");
            ws.value(2, 2, "2005");
            ws.value(2, 3, "Belt Drive");
            ws.value(2, 4, "PART-002");
            // col 5 (MfrLabel) vacío → null al parsear
            ws.value(2, 6, "Rear");
            // col 7 (EngineLiters) vacío → null al parsear
            ws.value(2, 8, "Truck");          // VehicleType

            wb.finish();
        }
        return tmp;
    }
}