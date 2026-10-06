package com.validador.aces.tests;

import java.util.List;

import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.RangeValidator;

/**
 * Unit tests para {@link RangeValidator} (TASK-040).
 * Cubre valores dentro/fuera de rango, nulos, no numéricos y límites exactos.
 */
public final class RangeValidatorTest {

    private RangeValidatorTest() {}

    // ── Test 1: valor dentro del rango → sin errores ──────────────────────

    public static void testValueInRange_noErrors() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        List<ValidationError> errors = v.validate(
            ValidatorTestFixture.appWithEngineValue("5.3"), null);
        Assert.assertEquals(0, errors.size(), "Valor 5.3 en [1.0, 10.0] → sin errores");
    }

    // ── Test 2: valor por debajo del mínimo → ERROR ───────────────────────

    public static void testValueBelowMin_generatesError() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        List<ValidationError> errors = v.validate(
            ValidatorTestFixture.appWithEngineValue("0.5"), null);
        Assert.assertEquals(1, errors.size(), "Valor 0.5 debajo del mínimo → 1 error");
        Assert.assertEquals(RangeValidator.VALUE_OUT_OF_RANGE, errors.get(0).getCode(),
            "Código debe ser VALUE_OUT_OF_RANGE");
    }

    // ── Test 3: valor por encima del máximo → ERROR ───────────────────────

    public static void testValueAboveMax_generatesError() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        List<ValidationError> errors = v.validate(
            ValidatorTestFixture.appWithEngineValue("15.0"), null);
        Assert.assertEquals(1, errors.size(), "Valor 15.0 sobre el máximo → 1 error");
        Assert.assertEquals(RangeValidator.VALUE_OUT_OF_RANGE, errors.get(0).getCode(),
            "Código debe ser VALUE_OUT_OF_RANGE");
    }

    // ── Test 4: valor null → sin errores ─────────────────────────────────

    public static void testNullValue_noErrors() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        List<ValidationError> errors = v.validate(
            ValidatorTestFixture.appWithEngineValue(null), null);
        Assert.assertEquals(0, errors.size(), "Valor null → sin errores (el atributo no aplica)");
    }

    // ── Test 5: valor no numérico → sin errores ───────────────────────────

    public static void testNonNumericValue_noErrors() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        List<ValidationError> errors = v.validate(
            ValidatorTestFixture.appWithEngineValue("desconocido"), null);
        Assert.assertEquals(0, errors.size(), "Valor no numérico → sin errores (no aplica este validador)");
    }

    // ── Test 6: límites exactos (min y max) → sin errores ─────────────────

    public static void testBoundaryValues_exactMinAndMax_noErrors() throws Exception {
        RangeValidator v = new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0);
        Assert.assertEquals(0,
            v.validate(ValidatorTestFixture.appWithEngineValue("1.0"), null).size(),
            "Valor exactamente en el límite inferior → sin errores");
        Assert.assertEquals(0,
            v.validate(ValidatorTestFixture.appWithEngineValue("10.0"), null).size(),
            "Valor exactamente en el límite superior → sin errores");
    }

    // ── Test 7: rango inválido (min > max) → IllegalArgumentException ─────

    public static void testInvalidRange_minGreaterThanMax_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 10.0, 1.0),
            "min > max debe lanzar IllegalArgumentException");
    }

    // ── Test 8: null application → IllegalArgumentException ───────────────

    public static void testNullApplication_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new RangeValidator(ValidatorTestFixture.ATTR_ENGINE, 1.0, 10.0).validate(null, null),
            "Application null debe lanzar IllegalArgumentException");
    }
}