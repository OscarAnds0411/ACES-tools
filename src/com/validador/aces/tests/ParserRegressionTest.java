package com.validador.aces.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.validador.aces.models.Application;
import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;
import com.validador.aces.parsers.ExcelApplicationParser;
import com.validador.aces.parsers.ExcelCatalogParser;

/**
 * Regresiones de los parsers (plan 003): columnas desplazadas por un encabezado
 * vacío del catálogo, requisitos con otra capitalización, valores de requisito
 * desconocidos y filas ACES descartadas por tener Make vacío.
 * Construye sus propios .xlsx temporales; no depende de datos reales.
 */
public final class ParserRegressionTest {

    private ParserRegressionTest() {}

    // ── Catálogo: un encabezado vacío no debe desplazar las columnas ──────

    public static void testCatalog_blankHeader_doesNotShiftFollowingColumns() throws Exception {
        // Columnas: 4=A, 5=(vacío), 6=C. Bajo la columna vacía hay un valor suelto.
        File tmp = writeCatalog("reg_blank_header",
            new String[]{"Product ID", "Product Line", "Category", "Sub Category", "A", null, "C"},
            new String[][]{{"1000", "Prod", "Cat", "Sub", "Required", "Required", "Optional"}});
        try {
            Catalog catalog = new ExcelCatalogParser().parse(tmp);
            ProductLine pl = catalog.findProductByName("Prod");
            Assert.assertNotNull(pl, "Prod debe existir");
            Assert.assertEquals(2, pl.getAttributeCount(), "La columna sin encabezado no es un atributo");
            Assert.assertEquals(AttributeRequirement.REQUIRED, attr(pl, "A").getRequirement(),
                "A conserva su valor");
            Assert.assertEquals(AttributeRequirement.OPTIONAL, attr(pl, "C").getRequirement(),
                "C debe leer SU columna (Optional), no la de la columna vacía");
        } finally { tmp.delete(); }
    }

    public static void testCatalog_blankHeader_isReportedAsWarning() throws Exception {
        File tmp = writeCatalog("reg_blank_header_warn",
            new String[]{"Product ID", "Product Line", "Category", "Sub Category", "A", null, "C"},
            new String[][]{{"1000", "Prod", "Cat", "Sub", "Required", "Required", "Optional"}});
        try {
            ExcelCatalogParser parser = new ExcelCatalogParser();
            parser.parse(tmp);
            Assert.assertTrue(containsText(parser.getWarnings(), "columna F"),
                "Debe avisar del encabezado vacío en la columna F; avisos: " + parser.getWarnings());
        } finally { tmp.delete(); }
    }

    // ── Catálogo: requisitos sin distinguir mayúsculas ────────────────────

    public static void testCatalog_requirementValues_caseInsensitive() throws Exception {
        File tmp = writeCatalog("reg_req_case",
            new String[]{"Product ID", "Product Line", "Category", "Sub Category", "A", "B", "C", "D"},
            new String[][]{{"1000", "Prod", "Cat", "Sub", "required", "REQUIRED", " Optional ", "not required"}});
        try {
            ExcelCatalogParser parser = new ExcelCatalogParser();
            ProductLine pl = parser.parse(tmp).findProductByName("Prod");
            Assert.assertNotNull(pl, "Prod debe existir");
            Assert.assertEquals(AttributeRequirement.REQUIRED,     attr(pl, "A").getRequirement(), "required");
            Assert.assertEquals(AttributeRequirement.REQUIRED,     attr(pl, "B").getRequirement(), "REQUIRED");
            Assert.assertEquals(AttributeRequirement.OPTIONAL,     attr(pl, "C").getRequirement(), " Optional ");
            Assert.assertEquals(AttributeRequirement.NOT_REQUIRED, attr(pl, "D").getRequirement(), "not required");
            Assert.assertTrue(parser.getWarnings().isEmpty(),
                "Valores reconocidos no deben generar avisos: " + parser.getWarnings());
        } finally { tmp.delete(); }
    }

    public static void testRequirement_isRecognized() {
        Assert.assertTrue(AttributeRequirement.isRecognized("Required"), "Required");
        Assert.assertTrue(AttributeRequirement.isRecognized("  required "), "required con espacios");
        Assert.assertTrue(AttributeRequirement.isRecognized("Not Required"), "Not Required");
        Assert.assertTrue(AttributeRequirement.isRecognized(""), "vacío se trata como Not Required");
        Assert.assertTrue(AttributeRequirement.isRecognized(null), "null se trata como Not Required");
        Assert.assertTrue(!AttributeRequirement.isRecognized("Req"), "Req no es un valor conocido");
        Assert.assertEquals(AttributeRequirement.REQUIRED, AttributeRequirement.fromCellValue("rEqUiReD"),
            "fromCellValue ignora mayúsculas");
        Assert.assertEquals(AttributeRequirement.NOT_REQUIRED, AttributeRequirement.fromCellValue("Req"),
            "valor desconocido sigue siendo NOT_REQUIRED (compatibilidad)");
    }

    // ── Catálogo: valor desconocido => Not Required + aviso ───────────────

    public static void testCatalog_unknownRequirement_isNotRequiredAndWarns() throws Exception {
        File tmp = writeCatalog("reg_req_unknown",
            new String[]{"Product ID", "Product Line", "Category", "Sub Category", "EngineLiters"},
            new String[][]{{"1000", "Brake Pad", "Cat", "Sub", "Req"}});
        try {
            ExcelCatalogParser parser = new ExcelCatalogParser();
            ProductLine pl = parser.parse(tmp).findProductByName("Brake Pad");
            Assert.assertEquals(AttributeRequirement.NOT_REQUIRED, attr(pl, "EngineLiters").getRequirement(),
                "El valor desconocido se trata como Not Required");
            List<String> warnings = parser.getWarnings();
            Assert.assertTrue(containsText(warnings, "Req"), "El aviso incluye el valor: " + warnings);
            Assert.assertTrue(containsText(warnings, "EngineLiters"), "El aviso incluye el atributo: " + warnings);
            Assert.assertTrue(containsText(warnings, "Brake Pad"), "El aviso incluye la línea: " + warnings);
        } finally { tmp.delete(); }
    }

    // ── ACES: filas con Make vacío ────────────────────────────────────────

    public static void testApplications_blankMake_rowsSkippedAndCounted() throws Exception {
        File tmp = writeAces("reg_blank_make", new String[][]{
            {"Ford",  "F-150", "2005", "Belt"},
            {null,    "X",     "2006", "Belt"},
            {"Dodge", "Ram",   "2007", "Belt"},
            {"",      "Y",     "2008", "Belt"},
            {"Honda", "Civic", "2009", "Belt"}});
        try {
            ExcelApplicationParser parser = new ExcelApplicationParser();
            List<Application> apps = parser.parse(tmp);
            Assert.assertEquals(3, apps.size(), "Solo las filas con Make se cargan");
            Assert.assertEquals(2, parser.getSkippedRowCount(), "Debe contar las 2 filas omitidas");
        } finally { tmp.delete(); }
    }

    public static void testApplications_skippedCount_resetsBetweenParses() throws Exception {
        File withBlank = writeAces("reg_skip_a", new String[][]{{null, "X", "2006", "Belt"}, {"Ford", "F", "2005", "Belt"}});
        File clean = writeAces("reg_skip_b", new String[][]{{"Ford", "F", "2005", "Belt"}});
        try {
            ExcelApplicationParser parser = new ExcelApplicationParser();
            parser.parse(withBlank);
            Assert.assertEquals(1, parser.getSkippedRowCount(), "Primer parse: 1 omitida");
            parser.parse(clean);
            Assert.assertEquals(0, parser.getSkippedRowCount(), "El contador se reinicia en cada parse");
        } finally { withBlank.delete(); clean.delete(); }
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private static Attribute attr(ProductLine pl, String name) {
        for (Attribute a : pl.getAttributes()) {
            if (a.getName().equals(name)) return a;
        }
        throw new AssertionError("No existe el atributo '" + name + "' en " + pl.getName());
    }

    private static boolean containsText(List<String> messages, String text) {
        for (String m : messages) if (m != null && m.contains(text)) return true;
        return false;
    }

    /** Escribe un catálogo; un encabezado null deja esa celda vacía. */
    private static File writeCatalog(String prefix, String[] headers, String[][] rows) throws Exception {
        File tmp = File.createTempFile(prefix + "_", ".xlsx");
        tmp.deleteOnExit();
        try (OutputStream os = new FileOutputStream(tmp)) {
            Workbook wb = new Workbook(os, "test", "1.0");
            Worksheet ws = wb.newWorksheet(ExcelCatalogParser.DEFAULT_SHEET_NAME);
            for (int c = 0; c < headers.length; c++) {
                if (headers[c] != null) ws.value(0, c, headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) ws.value(r + 1, c, rows[r][c]);
                }
            }
            wb.finish();
        }
        return tmp;
    }

    /** Escribe un ACES mínimo (Make, Model, Year, Product); una celda null/"" queda vacía. */
    private static File writeAces(String prefix, String[][] rows) throws Exception {
        File tmp = File.createTempFile(prefix + "_", ".xlsx");
        tmp.deleteOnExit();
        try (OutputStream os = new FileOutputStream(tmp)) {
            Workbook wb = new Workbook(os, "test", "1.0");
            Worksheet ws = wb.newWorksheet(ExcelApplicationParser.DEFAULT_SHEET_NAME);
            String[] headers = {"Make", "Model", "Year", "Product"};
            for (int c = 0; c < headers.length; c++) ws.value(0, c, headers[c]);
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length; c++) {
                    String v = rows[r][c];
                    if (v != null && !v.isEmpty()) ws.value(r + 1, c, v);
                }
            }
            wb.finish();
        }
        return tmp;
    }
}
