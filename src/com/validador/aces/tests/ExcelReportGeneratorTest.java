package com.validador.aces.tests;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Assert.assertEquals(4, report.getSections().size(),
            "Reporte batch debe tener 4 secciones: Resumen, Faltantes, Estadísticas, Números de parte");
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

    // ── Plan 005: escritura atómica y comparación de archivos ─────────────

    private static final byte[] ORIGINAL_BYTES =
        "CONTENIDO ORIGINAL QUE NO DEBE PERDERSE".getBytes(StandardCharsets.UTF_8);

    /** Reporte cuyo renderizado falla DESPUÉS de abrir el destino (a mitad de la escritura). */
    private static Report failingReport() {
        return new Report("Reporte que falla") {
            private static final long serialVersionUID = 1L;
            @Override public List<ReportSection> getSections() {
                throw new IllegalStateException("fallo inducido a mitad de la escritura");
            }
        };
    }

    private static Path newTempDir() throws Exception {
        return Files.createTempDirectory("report_save_");
    }

    private static void deleteTree(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    /** Nombres de archivos temporales (.tmp) que quedaron en el directorio. */
    private static List<String> strayTempFiles(Path dir) throws Exception {
        List<String> found = new java.util.ArrayList<>();
        try (java.util.stream.Stream<Path> list = Files.list(dir)) {
            list.forEach(p -> { if (p.getFileName().toString().endsWith(".tmp")) found.add(p.getFileName().toString()); });
        }
        return found;
    }

    private static void assertIsValidReport(File file, String label) throws Exception {
        try (ReadableWorkbook wb = new ReadableWorkbook(file)) {
            Assert.assertTrue(wb.findSheet(ExcelReportGenerator.REPORT_SHEET_NAME).isPresent(),
                label + ": debe contener la hoja '" + ExcelReportGenerator.REPORT_SHEET_NAME + "'");
        }
    }

    public static void testWriteReportToFile_replacesExistingFile() throws Exception {
        Path dir = newTempDir();
        try {
            File target = dir.resolve("reporte.xlsx").toFile();
            Files.write(target.toPath(), ORIGINAL_BYTES);

            ExcelReportGenerator gen = new ExcelReportGenerator();
            gen.writeReportToFile(gen.generate(resultWithError()), target);

            assertIsValidReport(target, "El destino existente se reemplaza por un reporte válido");
            Assert.assertEquals(java.util.Collections.<String>emptyList(), strayTempFiles(dir),
                "No debe quedar ningún archivo temporal");
        } finally { deleteTree(dir); }
    }

    public static void testWriteReportToFile_failedRender_leavesExistingFileIntact() throws Exception {
        Path dir = newTempDir();
        try {
            File target = dir.resolve("reporte.xlsx").toFile();
            Files.write(target.toPath(), ORIGINAL_BYTES);

            Assert.assertThrows(IllegalStateException.class,
                () -> new ExcelReportGenerator().writeReportToFile(failingReport(), target),
                "El fallo de renderizado debe propagarse");

            Assert.assertTrue(java.util.Arrays.equals(ORIGINAL_BYTES, Files.readAllBytes(target.toPath())),
                "Si la escritura falla, el archivo existente debe quedar intacto (no truncado)");
        } finally { deleteTree(dir); }
    }

    public static void testWriteReportToFile_failedRender_leavesNoTempFile() throws Exception {
        Path dir = newTempDir();
        try {
            File target = dir.resolve("reporte.xlsx").toFile();
            Assert.assertThrows(IllegalStateException.class,
                () -> new ExcelReportGenerator().writeReportToFile(failingReport(), target),
                "El fallo de renderizado debe propagarse");

            Assert.assertTrue(!target.exists(), "Si falla, no debe crearse un destino a medias");
            Assert.assertEquals(java.util.Collections.<String>emptyList(), strayTempFiles(dir),
                "Un fallo no debe dejar archivos temporales");
        } finally { deleteTree(dir); }
    }

    public static void testWriteReportToFile_missingDirectory_throwsIOException() throws Exception {
        Path dir = newTempDir();
        try {
            File target = dir.resolve("no_existe").resolve("reporte.xlsx").toFile();
            ExcelReportGenerator gen = new ExcelReportGenerator();
            Report report = gen.generate(resultWithError());
            Assert.assertThrows(java.io.IOException.class,
                () -> gen.writeReportToFile(report, target),
                "Un directorio inexistente debe producir IOException");
            Assert.assertTrue(!dir.resolve("no_existe").toFile().exists(),
                "No debe crear el directorio faltante");
        } finally { deleteTree(dir); }
    }

    /**
     * Con el destino abierto por otro proceso (en Windows impide reemplazarlo) el
     * resultado debe ser siempre limpio: o bien el reporte nuevo y válido, o bien
     * el original intacto — nunca un archivo a medias — y sin temporales.
     */
    public static void testWriteReportToFile_lockedTarget_isAlwaysCleanOutcome() throws Exception {
        Path dir = newTempDir();
        try {
            File target = dir.resolve("reporte.xlsx").toFile();
            Files.write(target.toPath(), ORIGINAL_BYTES);

            ExcelReportGenerator gen = new ExcelReportGenerator();
            Report report = gen.generate(resultWithError());
            boolean replaced;
            try (java.io.FileInputStream holder = new java.io.FileInputStream(target)) {
                try {
                    gen.writeReportToFile(report, target);
                    replaced = true;
                } catch (java.io.IOException expectedOnWindows) {
                    replaced = false;
                }
            }

            if (replaced) {
                assertIsValidReport(target, "Reemplazado");
            } else {
                Assert.assertTrue(java.util.Arrays.equals(ORIGINAL_BYTES, Files.readAllBytes(target.toPath())),
                    "Si no se pudo reemplazar, el original debe quedar intacto");
            }
            Assert.assertEquals(java.util.Collections.<String>emptyList(), strayTempFiles(dir),
                "No debe quedar ningún archivo temporal");
        } finally { deleteTree(dir); }
    }

    // ── Plan 005: isSameFile ──────────────────────────────────────────────

    public static void testIsSameFile_sameFileSpelledTwoWays() throws Exception {
        Path dir = newTempDir();
        try {
            File real = dir.resolve("aces.xlsx").toFile();
            Files.write(real.toPath(), ORIGINAL_BYTES);
            File alias = new File(dir.toFile(), "." + File.separator + "aces.xlsx");
            Assert.assertTrue(ExcelReportGenerator.isSameFile(real, alias),
                "'dir\\.\\aces.xlsx' y 'dir\\aces.xlsx' son el mismo archivo");
            Assert.assertTrue(ExcelReportGenerator.isSameFile(real, real), "Un archivo es igual a sí mismo");
        } finally { deleteTree(dir); }
    }

    public static void testIsSameFile_notYetExistingFile_comparedByPath() throws Exception {
        Path dir = newTempDir();
        try {
            File a = new File(dir.toFile(), "nuevo.xlsx");
            File b = new File(dir.toFile(), "." + File.separator + "nuevo.xlsx");
            Assert.assertTrue(!a.exists(), "Precondición: el archivo no existe");
            Assert.assertTrue(ExcelReportGenerator.isSameFile(a, b),
                "Sin existir, se compara por ruta normalizada");
        } finally { deleteTree(dir); }
    }

    public static void testIsSameFile_differentFilesAndNulls() throws Exception {
        Path dir = newTempDir();
        try {
            File a = dir.resolve("a.xlsx").toFile();
            File b = dir.resolve("b.xlsx").toFile();
            Files.write(a.toPath(), ORIGINAL_BYTES);
            Files.write(b.toPath(), ORIGINAL_BYTES);   // mismo contenido, distinto archivo
            Assert.assertTrue(!ExcelReportGenerator.isSameFile(a, b), "Archivos distintos");
            Assert.assertTrue(!ExcelReportGenerator.isSameFile(a, null), "null no es el mismo archivo");
            Assert.assertTrue(!ExcelReportGenerator.isSameFile(null, a), "null no es el mismo archivo");
            Assert.assertTrue(!ExcelReportGenerator.isSameFile(null, null), "null/null no es 'el mismo'");
        } finally { deleteTree(dir); }
    }
}