package com.validador.aces.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;
import com.validador.aces.parsers.ExcelCatalogParser;

/**
 * Tests de round-trip para el catálogo ACES (TASK-037):
 * escribe un catálogo en memoria a Excel con fastexcel, lo lee de nuevo
 * con {@link ExcelCatalogParser} y verifica que la estructura es equivalente.
 */
public final class CatalogRoundTripTest {

    private CatalogRoundTripTest() {}

    // ── Test 1: nombres de ProductLine preservados ────────────────────────

    public static void testRoundTrip_productLineNamesPreserved() throws Exception {
        File tmp = writeTmpCatalog("roundtrip_catalog_names", ExcelCatalogParser.DEFAULT_SHEET_NAME,
            new String[]{"Alpha Product", "Beta Product", "Gamma Product"});
        try {
            Catalog catalog = new ExcelCatalogParser().parse(tmp);
            Assert.assertEquals(3, catalog.getProductLineCount(), "Debe haber 3 ProductLines");
            Assert.assertNotNull(catalog.findProductByName("Alpha Product"), "Alpha Product debe existir");
            Assert.assertNotNull(catalog.findProductByName("Beta Product"),  "Beta Product debe existir");
            Assert.assertNotNull(catalog.findProductByName("Gamma Product"), "Gamma Product debe existir");
        } finally { tmp.delete(); }
    }

    // ── Test 2: AttributeRequirement preservado ───────────────────────────

    public static void testRoundTrip_attributeRequirementsPreserved() throws Exception {
        File tmp = writeTmpCatalog("roundtrip_catalog_reqs", ExcelCatalogParser.DEFAULT_SHEET_NAME,
            new String[]{"Test Product"});
        try {
            Catalog catalog = new ExcelCatalogParser().parse(tmp);
            ProductLine pl = catalog.findProductByName("Test Product");
            Assert.assertNotNull(pl, "Test Product debe existir");
            Assert.assertEquals(3, pl.getAttributeCount(), "Debe tener 3 atributos");

            // EngineLiters → Required, VehicleType → Optional, Region → Not Required
            var attrs = pl.getAttributes();
            Assert.assertEquals("Required",     attrs.get(0).getRequirement().getLabel(), "EngineLiters → REQUIRED");
            Assert.assertEquals("Optional",     attrs.get(1).getRequirement().getLabel(), "VehicleType → OPTIONAL");
            Assert.assertEquals("Not Required", attrs.get(2).getRequirement().getLabel(), "Region → NOT_REQUIRED");
        } finally { tmp.delete(); }
    }

    // ── Test 3: Category y SubCategory preservados ────────────────────────

    public static void testRoundTrip_categoryAndSubCategoryPreserved() throws Exception {
        File tmp = writeTmpCatalog("roundtrip_catalog_cat", ExcelCatalogParser.DEFAULT_SHEET_NAME,
            new String[]{"Cat Product"});
        try {
            Catalog catalog = new ExcelCatalogParser().parse(tmp);
            ProductLine pl = catalog.findProductByName("Cat Product");
            Assert.assertNotNull(pl, "Cat Product debe existir");
            Assert.assertEquals("Engine Cooling", pl.getCategory(),    "Category debe preservarse");
            Assert.assertEquals("Condensers",     pl.getSubCategory(), "SubCategory debe preservarse");
        } finally { tmp.delete(); }
    }

    // ── Test 4: nombre de hoja personalizado ─────────────────────────────

    public static void testRoundTrip_customSheetName_works() throws Exception {
        File tmp = writeTmpCatalog("roundtrip_catalog_sheet", "My Custom Sheet",
            new String[]{"Custom Product"});
        try {
            Catalog catalog = new ExcelCatalogParser().parse(tmp, "My Custom Sheet");
            Assert.assertEquals(1, catalog.getProductLineCount(),
                "Debe parsear correctamente con hoja personalizada");
            Assert.assertNotNull(catalog.findProductByName("Custom Product"), "Custom Product debe existir");
        } finally { tmp.delete(); }
    }

    // ── Utilidad: escribe un Excel de catálogo de prueba ─────────────────

    /**
     * Escribe un archivo Excel con la estructura que espera {@link ExcelCatalogParser}:
     * <ul>
     *   <li>Fila 0: encabezados (Product ID, Product Line, Category, Sub Category, + atributos)</li>
     *   <li>Filas 1..N: una fila por nombre de producto en {@code productNames}</li>
     * </ul>
     */
    static File writeTmpCatalog(String prefix, String sheetName, String[] productNames)
            throws Exception {
        File tmp = File.createTempFile(prefix + "_", ".xlsx");
        tmp.deleteOnExit();

        try (OutputStream os = new FileOutputStream(tmp)) {
            Workbook wb = new Workbook(os, "test", "1.0");
            Worksheet ws = wb.newWorksheet(sheetName);

            // Encabezados
            ws.value(0, 0, "Product ID");
            ws.value(0, 1, "Product Line");
            ws.value(0, 2, "Category");
            ws.value(0, 3, "Sub Category");
            ws.value(0, 4, "EngineLiters");
            ws.value(0, 5, "VehicleType");
            ws.value(0, 6, "Region");

            // Filas de datos
            for (int i = 0; i < productNames.length; i++) {
                int row = i + 1;
                ws.value(row, 0, String.valueOf(1000 + i));  // Product ID
                ws.value(row, 1, productNames[i]);            // Product Line
                ws.value(row, 2, "Engine Cooling");           // Category
                ws.value(row, 3, "Condensers");               // Sub Category
                ws.value(row, 4, "Required");                 // EngineLiters
                ws.value(row, 5, "Optional");                 // VehicleType
                ws.value(row, 6, "Not Required");             // Region
            }
            wb.finish();
        }
        return tmp;
    }
}