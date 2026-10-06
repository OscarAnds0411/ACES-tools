package com.validador.aces.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Sheet;

import com.validador.aces.comparison.Comparator;
import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ComparisonResult;
import com.validador.aces.parsers.ExcelApplicationParser;
import com.validador.aces.parsers.ExcelCatalogParser;
import com.validador.aces.reporting.ExcelReportGenerator;

/**
 * Test de integración end-to-end (TASK-044): carga los archivos reales del
 * proyecto, ejecuta la comparación completa y genera un reporte Excel.
 *
 * <p>Usa solo las primeras 100 aplicaciones del ACES para mantener el tiempo
 * de ejecución del test razonable (~1 s) sin sacrificar cobertura del flujo.</p>
 */
public final class EndToEndTest {

    private static final String CATALOG_PATH =
        "catalogoDb/Atributos ACES por l\u00ednea de producto.xlsx";
    private static final String ACES_PATH =
        "ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx";
    private static final int SAMPLE_SIZE = 100;

    private EndToEndTest() {}

    // ── Test 1: cargar catálogo real ──────────────────────────────────────

    public static void testLoadRealCatalog_succeeds() throws Exception {
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        Assert.assertTrue(catalog.getProductLineCount() > 30_000,
            "El catálogo real debe tener > 30 000 líneas de producto");
    }

    // ── Test 2: cargar ACES real ──────────────────────────────────────────

    public static void testLoadRealAces_succeeds() throws Exception {
        List<Application> apps = new ExcelApplicationParser().parse(ACES_PATH);
        Assert.assertTrue(apps.size() > 80_000,
            "El ACES real debe tener > 80 000 aplicaciones");
    }

    // ── Test 3: ejecutar validación completa ──────────────────────────────

    public static void testRunComparison_completesWithoutException() throws Exception {
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        List<Application> all  = new ExcelApplicationParser().parse(ACES_PATH);
        List<Application> sample = all.subList(0, Math.min(SAMPLE_SIZE, all.size()));

        List<ComparisonResult> results = new Comparator("test-e2e").compareAll(sample, catalog);
        Assert.assertEquals(sample.size(), results.size(),
            "compareAll debe retornar 1 resultado por aplicación");
    }

    // ── Test 4: generar reporte Excel ─────────────────────────────────────

    public static void testGenerateReport_createsValidExcelFile() throws Exception {
        Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
        List<Application> all    = new ExcelApplicationParser().parse(ACES_PATH);
        List<Application> sample = all.subList(0, Math.min(SAMPLE_SIZE, all.size()));

        List<ComparisonResult> results = new Comparator("test-e2e").compareAll(sample, catalog);

        // Agrupar por producto
        Map<String, List<ComparisonResult>> byLine = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            String key = sample.get(i).getProductName();
            if (key == null) key = "(sin producto)";
            byLine.computeIfAbsent(key, k -> new ArrayList<>()).add(results.get(i));
        }

        File reportFile = File.createTempFile("e2e_report_", ".xlsx");
        reportFile.deleteOnExit();
        try {
            new ExcelReportGenerator().generateAndWriteBatch(byLine, reportFile);
            Assert.assertTrue(reportFile.exists() && reportFile.length() > 0,
                "El archivo de reporte debe existir y no estar vacío");

            // Verificar que el Excel contiene la hoja esperada
            try (ReadableWorkbook wb = new ReadableWorkbook(reportFile)) {
                java.util.Optional<Sheet> sheet = wb.findSheet(ExcelReportGenerator.REPORT_SHEET_NAME);
                Assert.assertTrue(sheet.isPresent(),
                    "El reporte debe contener la hoja '" + ExcelReportGenerator.REPORT_SHEET_NAME + "'");
            }
        } finally {
            reportFile.delete();
        }
    }

    // ── Test 5: archivo ACES original no modificado ───────────────────────

    public static void testAcesOriginalNotModified_afterReportGeneration() throws Exception {
        File acesFile = new File(ACES_PATH);
        long sizeBefore = acesFile.length();

        // Generar reporte a un archivo separado
        File reportFile = File.createTempFile("e2e_check_", ".xlsx");
        reportFile.deleteOnExit();
        try {
            Catalog catalog = new ExcelCatalogParser().parse(CATALOG_PATH);
            List<Application> apps = new ExcelApplicationParser()
                .parse(ACES_PATH).subList(0, 5);
            List<ComparisonResult> results = new Comparator().compareAll(apps, catalog);
            Map<String, List<ComparisonResult>> byLine = new LinkedHashMap<>();
            for (int i = 0; i < results.size(); i++) {
                String k = apps.get(i).getProductName();
                if (k == null) k = "(?)";
                byLine.computeIfAbsent(k, x -> new ArrayList<>()).add(results.get(i));
            }
            new ExcelReportGenerator().generateAndWriteBatch(byLine, reportFile);
        } finally {
            reportFile.delete();
        }

        long sizeAfter = acesFile.length();
        Assert.assertEquals(sizeBefore, sizeAfter,
            "El archivo ACES original no debe cambiar de tamaño después de generar el reporte");
    }
}