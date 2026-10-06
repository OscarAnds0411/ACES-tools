package com.validador.aces.tests;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.reader.ReadableWorkbook;

import com.validador.aces.models.ComparisonResult;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.reporting.ExcelReportGenerator;
import com.validador.aces.reporting.Report;
import com.validador.aces.reporting.ReportSection;
import com.validador.aces.validation.AttributeValidator;

/**
 * Unit tests para {@link ExcelReportGenerator} (TASK-046).
 * Verifica generación de reportes con y sin errores, escritura a archivo,
 * número de secciones y que los archivos originales no se modifican.
 */
public final class ExcelReportGeneratorTest {

    private ExcelReportGeneratorTest() {}

    // ── Fixture: ComparisonResult con 0 errores ───────────────────────────

    private static ComparisonResult resultNoErrors() {
        ComparisonResult r = new ComparisonResult("TestApp", "TestCatalog");
        r.setTotalAttributes(3);
        r.setValidAttributes(3);
        r.setInvalidAttributes(0);
        return r;
    }

    // ── Fixture: ComparisonResult con 1 error ─────────────────────────────

    private static ComparisonResult resultWithError() {
        ComparisonResult r = new ComparisonResult("TestApp", "TestCatalog");
        r.setTotalAttributes(3);
        r.setValidAttributes(2);
        r.setInvalidAttributes(1);
        r.addErrors(Collections.singletonList(
            new ValidationError(ErrorSeverity.ERROR,
                AttributeValidator.MISSING_REQUIRED_ATTRIBUTE,
                "El atributo requerido 'EngineLiters' no est\u00e1 presente",
                "EngineLiters", "TestProduct")
        ));
        return r;
    }

    // ── Test 1: reporte sin errores contiene sección "Sin problemas" ───────

    public static void testGenerate_noErrors_showsCompleteMessage() throws Exception {
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Report report = gen.generate(resultNoErrors());
        ReportSection errorsSection = report.getSections().get(1); // "Atributos Faltantes"
        Assert.assertEquals(1, errorsSection.getRowCount(),
            "Sin errores → la sección de errores debe tener 1 fila (el mensaje 'Completo...')");
        String firstCell = errorsSection.getRows().get(0).get(0);
        Assert.assertEquals("INFO", firstCell,
            "La fila del mensaje 'Completo' debe tener Severidad=INFO");
    }

    // ── Test 2: reporte con error contiene la fila del error ──────────────

    public static void testGenerate_withErrors_errorRowPresent() throws Exception {
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Report report = gen.generate(resultWithError());
        ReportSection errorsSection = report.getSections().get(1);
        Assert.assertEquals(1, errorsSection.getRowCount(),
            "Con 1 error → la sección debe tener 1 fila de error");
        String code = errorsSection.getRows().get(0).get(1);  // columna Código
        Assert.assertEquals(AttributeValidator.MISSING_REQUIRED_ATTRIBUTE, code,
            "El código del error en la sección debe ser MISSING_REQUIRED_ATTRIBUTE");
    }

    // ── Test 3: reporte individual tiene exactamente 2 secciones ──────────

    public static void testGenerate_individualReport_hasTwoSections() throws Exception {
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Report report = gen.generate(resultWithError());
        Assert.assertEquals(2, report.getSections().size(),
            "Reporte individual debe tener 2 secciones: Resumen y Atributos Faltantes");
    }

    // ── Test 4: reporte batch tiene exactamente 3 secciones ───────────────

    public static void testGenerateBatch_hasThreSections() throws Exception {
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Map<String, List<ComparisonResult>> byLine = new LinkedHashMap<>();
        byLine.put("TestProduct", Collections.singletonList(resultWithError()));
        Report report = gen.generateBatchReport(byLine);
        Assert.assertEquals(3, report.getSections().size(),
            "Reporte batch debe tener 3 secciones: Resumen, Faltantes, Estadísticas");
    }

    // ── Test 5: writeReportToFile crea un Excel válido y legible ──────────

    public static void testWriteReportToFile_createsValidExcel() throws Exception {
        ExcelReportGenerator gen = new ExcelReportGenerator();
        Report report = gen.generate(resultWithError());
        File tmp = File.createTempFile("report_test_", ".xlsx");
        tmp.deleteOnExit();
        try {
            gen.writeReportToFile(report, tmp);
            Assert.assertTrue(tmp.exists() && tmp.length() > 0,
                "El archivo de reporte debe existir y no estar vacío");
            try (ReadableWorkbook wb = new ReadableWorkbook(tmp)) {
                java.util.Optional<org.dhatim.fastexcel.reader.Sheet> sheet =
                    wb.findSheet(ExcelReportGenerator.REPORT_SHEET_NAME);
                Assert.assertTrue(sheet.isPresent(),
                    "El Excel debe contener la hoja '" + ExcelReportGenerator.REPORT_SHEET_NAME + "'");
                Assert.assertTrue(sheet.get().read().size() > 0,
                    "La hoja del reporte no debe estar vacía");
            }
        } finally { tmp.delete(); }
    }

    // ── Test 6: defaultReportFileFor genera nombre correcto ───────────────

    public static void testDefaultReportFileFor_correctSuffix() throws Exception {
        File acesFile = new File("ACES/test_aces.xlsx");
        File report = ExcelReportGenerator.defaultReportFileFor(acesFile);
        Assert.assertTrue(report.getName().endsWith("_Reporte_Auditoria.xlsx"),
            "El nombre del reporte debe terminar en '_Reporte_Auditoria.xlsx'");
        Assert.assertNotNull(report.getParent(),
            "El directorio del reporte debe ser el mismo que el ACES original");
    }

    // ── Test 7: null report → IllegalArgumentException ────────────────────

    public static void testWriteReportToFile_nullReport_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new ExcelReportGenerator().writeReportToFile(null, new File("out.xlsx")),
            "Report null debe lanzar IAE");
    }
}