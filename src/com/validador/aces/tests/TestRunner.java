package com.validador.aces.tests;

/**
 * Orquestador de la suite de tests del Validador de Atributos ACES.
 *
 * <p>Ejecuta todas las clases de test y muestra un resumen de resultados.
 * Termina con exit code 0 si todos pasan, o 1 si alguno falla.</p>
 *
 * <p>Para ejecutar desde Ant: {@code ant test}.</p>
 */
public class TestRunner {

    private int passed  = 0;
    private int failed  = 0;
    private final StringBuilder report = new StringBuilder();

    // ── Punto de entrada ──────────────────────────────────────────────────

    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.run();
    }

    private void run() {
        String sep = "═".repeat(66);
        System.out.println(sep);
        System.out.println("  Suite de Tests — Validador de Atributos ACES");
        System.out.println(sep);

        // TASK-035
        runSuite("[TASK-035] CatalogParserTest", CatalogParserTest.class);

        // TASK-036
        runSuite("[TASK-036] ApplicationParserTest", ApplicationParserTest.class);

        // TASK-037
        runSuite("[TASK-037] CatalogRoundTripTest", CatalogRoundTripTest.class);

        // TASK-038
        runSuite("[TASK-038] ApplicationRoundTripTest", ApplicationRoundTripTest.class);

        // TASK-039
        runSuite("[TASK-039] AttributeValidatorTest", AttributeValidatorTest.class);

        // TASK-040
        runSuite("[TASK-040] RangeValidatorTest", RangeValidatorTest.class);

        // TASK-041
        runSuite("[TASK-041] EnumValidatorTest", EnumValidatorTest.class);

        // TASK-042
        runSuite("[TASK-042] CompositeValidatorTest", CompositeValidatorTest.class);

        // TASK-043
        runSuite("[TASK-043] ComparatorTest", ComparatorTest.class);

        // TASK-044
        runSuite("[TASK-044] EndToEndTest", EndToEndTest.class);

        // TASK-045
        runSuite("[TASK-045] ValidationIdempotenceTest", ValidationIdempotenceTest.class);

        // TASK-046
        runSuite("[TASK-046] ExcelReportGeneratorTest", ExcelReportGeneratorTest.class);

        // TASK-047
        runSuite("[TASK-047] ReportCompletenessTest", ReportCompletenessTest.class);

        // Resumen final
        System.out.println();
        System.out.println(sep);
        int total = passed + failed;
        String verdict = failed == 0 ? "✔ TODOS PASAN" : "✘ HAY FALLOS";
        System.out.println(String.format("  %s   %d tests: %d pasados, %d fallidos",
            verdict, total, passed, failed));
        System.out.println(sep);

        if (failed > 0) {
            System.out.println("\nDetalle de fallos:");
            System.out.println(report.toString());
            System.exit(1);
        }
    }

    // ── Descubrimiento de métodos de test ─────────────────────────────────

    private void runSuite(String suiteName, Class<?> suiteClass) {
        System.out.println("\n" + suiteName);
        java.lang.reflect.Method[] methods = suiteClass.getDeclaredMethods();
        // Filtrar métodos que empiecen con "test" y sean públicos + estáticos
        for (java.lang.reflect.Method m : methods) {
            if (!m.getName().startsWith("test")) continue;
            if (!java.lang.reflect.Modifier.isPublic(m.getModifiers()))   continue;
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers()))   continue;
            if (m.getParameterCount() != 0) continue;

            runTest(m.getName(), () -> m.invoke(null));
        }
    }

    private void runTest(String name, Assert.ThrowingRunnable test) {
        try {
            test.run();
            System.out.println("  ✓ " + name);
            passed++;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            String detail = cause != null ? cause.getMessage() : ite.getMessage();
            System.out.println("  ✗ " + name);
            report.append("  ").append(name).append(": ").append(detail).append("\n");
            failed++;
        } catch (Throwable t) {
            System.out.println("  ✗ " + name + " [" + t.getClass().getSimpleName() + "]");
            report.append("  ").append(name).append(": ").append(t.getMessage()).append("\n");
            failed++;
        }
    }
}