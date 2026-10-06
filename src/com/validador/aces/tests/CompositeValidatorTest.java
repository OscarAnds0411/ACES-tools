package com.validador.aces.tests;

import java.util.Arrays;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;
import com.validador.aces.validation.CompositeValidator;
import com.validador.aces.validation.EnumValidator;
import com.validador.aces.validation.RangeValidator;
import com.validador.aces.validation.Validator;

/**
 * Unit tests para {@link CompositeValidator} (TASK-042).
 * Verifica la ejecución en cadena, acumulación de errores y
 * comportamiento de {@code stopOnFirstCriticalError}.
 */
public final class CompositeValidatorTest {

    private CompositeValidatorTest() {}

    // ── Test 1: sin validadores → lista vacía ─────────────────────────────

    public static void testNoValidators_noErrors() throws Exception {
        CompositeValidator cv = new CompositeValidator();
        List<ValidationError> errors = cv.validate(
            ValidatorTestFixture.appAllPresent(), ValidatorTestFixture.buildCatalog());
        Assert.assertEquals(0, errors.size(), "Sin validadores → 0 errores");
    }

    // ── Test 2: un validador sin errores → lista vacía ────────────────────

    public static void testSingleValidator_noErrors_propagatesCorrectly() throws Exception {
        CompositeValidator cv = new CompositeValidator();
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0));
        List<ValidationError> errors = cv.validate(
            ValidatorTestFixture.appWithEngineValue("5.3"), null);
        Assert.assertEquals(0, errors.size(),
            "Un validador que pasa → 0 errores");
    }

    // ── Test 3: múltiples validadores → acumula todos los errores ─────────

    public static void testMultipleValidators_accumulatesAllErrors() throws Exception {
        // RangeValidator: EngineLiters fuera de rango → 1 error
        // EnumValidator:  VehicleType no válido → 1 error
        CompositeValidator cv = new CompositeValidator();
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0));
        cv.addValidator(new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE,
            Arrays.asList("SUV", "Car"), false));

        Application app = new Application("Make", "Model", "2000", ValidatorTestFixture.PRODUCT_NAME);
        app.setAttributeValue(ValidatorTestFixture.ATTR_ENGINE,  "3.0");   // fuera de rango
        app.setAttributeValue(ValidatorTestFixture.ATTR_VEHICLE, "Camión");// no en lista

        List<ValidationError> errors = cv.validate(app, null);
        Assert.assertEquals(2, errors.size(),
            "Dos validadores con error → 2 errores acumulados");
    }

    // ── Test 4: stopOnFirstCriticalError=false → se ejecutan TODOS ────────

    public static void testStopOnFirstCriticalError_false_runsAllValidators() throws Exception {
        CompositeValidator cv = new CompositeValidator();
        cv.setStopOnFirstCriticalError(false);
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0));
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0)); // mismo

        Application app = ValidatorTestFixture.appWithEngineValue("3.0");
        List<ValidationError> errors = cv.validate(app, null);
        Assert.assertEquals(2, errors.size(),
            "Con stopOnFirstCriticalError=false ambos validadores deben ejecutarse");
    }

    // ── Test 5: stopOnFirstCriticalError=true → se para tras el 1er ERROR ─

    public static void testStopOnFirstCriticalError_true_stopsAfterFirstError() throws Exception {
        CompositeValidator cv = new CompositeValidator();
        cv.setStopOnFirstCriticalError(true);
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0)); // falla
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0)); // no llega

        Application app = ValidatorTestFixture.appWithEngineValue("3.0");
        List<ValidationError> errors = cv.validate(app, null);
        Assert.assertEquals(1, errors.size(),
            "Con stopOnFirstCriticalError=true solo debe ejecutarse el primer validador");
    }

    // ── Test 6: AttributeValidator dentro de composite ────────────────────

    public static void testWithAttributeValidator_detectsMissingRequired() throws Exception {
        CompositeValidator cv = new CompositeValidator();
        cv.addValidator(new AttributeValidator());
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = cv.validate(
            ValidatorTestFixture.appRequiredMissing(), cat);
        Assert.assertEquals(1,
            (int) ValidatorTestFixture.countByCode(errors, AttributeValidator.MISSING_REQUIRED_ATTRIBUTE),
            "AttributeValidator en composite debe detectar atributo requerido faltante");
    }

    // ── Test 7: addValidator null → IllegalArgumentException ──────────────

    public static void testAddNullValidator_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new CompositeValidator().addValidator(null),
            "addValidator(null) debe lanzar IllegalArgumentException");
    }

    // ── Test 8: warning NO corta la cadena con stopOnFirstCriticalError ────

    public static void testWarning_doesNotTriggerStopOnFirstCriticalError() throws Exception {
        // AttributeValidator produce WARNING (producto no encontrado), no ERROR
        // El segundo validador debe seguir ejecutándose
        CompositeValidator cv = new CompositeValidator();
        cv.setStopOnFirstCriticalError(true);
        cv.addValidator(new AttributeValidator());  // → WARNING (producto desconocido)
        cv.addValidator(new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 5.0, 10.0)); // → ERROR

        Catalog cat = ValidatorTestFixture.buildCatalog();
        Application app = ValidatorTestFixture.appUnknownProduct();
        app.setAttributeValue(ValidatorTestFixture.ATTR_ENGINE, "3.0"); // fuera de rango

        List<ValidationError> errors = cv.validate(app, cat);
        long warnings = ValidatorTestFixture.countBySeverity(errors, com.validador.aces.models.ErrorSeverity.WARNING);
        long errorsCount = ValidatorTestFixture.countBySeverity(errors, com.validador.aces.models.ErrorSeverity.ERROR);

        Assert.assertEquals(1, (int) warnings, "Debe haber 1 warning (producto no encontrado)");
        Assert.assertEquals(1, (int) errorsCount,
            "WARNING no debe detener la cadena; el segundo validador debe ejecutarse");
    }
}