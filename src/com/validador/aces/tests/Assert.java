package com.validador.aces.tests;

import java.util.Objects;

/**
 * Utilidades de aserción mínimas para la suite de tests.
 * Lanza {@link AssertionError} con un mensaje descriptivo cuando falla.
 */
public final class Assert {

    private Assert() {}

    public static void assertEquals(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual))
            throw new AssertionError(msg + "  esperado=[" + expected + "]  actual=[" + actual + "]");
    }

    public static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual)
            throw new AssertionError(msg + "  esperado=" + expected + "  actual=" + actual);
    }

    public static void assertEquals(double expected, double actual, double delta, String msg) {
        if (Math.abs(expected - actual) > delta)
            throw new AssertionError(msg + "  esperado=" + expected + "  actual=" + actual);
    }

    public static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    public static void assertNotNull(Object obj, String msg) {
        if (obj == null) throw new AssertionError(msg + " — objeto null inesperado");
    }

    public static void assertNull(Object obj, String msg) {
        if (obj != null) throw new AssertionError(msg + " — se esperaba null pero fue [" + obj + "]");
    }

    /**
     * Verifica que el bloque lanza exactamente {@code expectedType} (o un subtipo).
     */
    public static void assertThrows(Class<? extends Throwable> expectedType,
            ThrowingRunnable block, String msg) {
        try {
            block.run();
            throw new AssertionError(msg + " — no se lanzó ninguna excepción (esperaba " + expectedType.getSimpleName() + ")");
        } catch (Throwable t) {
            if (!expectedType.isInstance(t))
                throw new AssertionError(msg + " — excepción incorrecta: " + t.getClass().getSimpleName() + " (esperaba " + expectedType.getSimpleName() + ")");
        }
    }

    /**
     * Omite el test actual (no cuenta como fallo) cuando un archivo de datos
     * reales no existe. Las rutas son relativas al directorio de trabajo
     * (la raíz del repositorio).
     */
    public static void assumeFileExists(String path) {
        assumeFileExists(new java.io.File(path));
    }

    public static void assumeFileExists(java.io.File file) {
        if (!file.isFile())
            throw new TestSkippedException("falta el archivo de datos reales: " + file.getPath());
    }

    /** Señal de test omitido; el {@code TestRunner} la cuenta aparte de los fallos. */
    public static final class TestSkippedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestSkippedException(String reason) {
            super(reason);
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}