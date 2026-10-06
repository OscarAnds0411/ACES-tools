package com.validador.aces.tests;

import java.util.Arrays;
import java.util.List;

import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.EnumValidator;

/**
 * Unit tests para {@link EnumValidator} (TASK-041).
 * Cubre coincidencia exacta, case-insensitive, valor ausente y valores nulos.
 */
public final class EnumValidatorTest {

    private static final List<String> VEHICLE_VALUES =
        Arrays.asList("SUV", "Truck", "Car", "Van");

    private EnumValidatorTest() {}

    // ── Test 1: valor en la lista permitida → sin errores ─────────────────

    public static void testValueInAllowedList_noErrors() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false);
        Assert.assertEquals(0,
            v.validate(ValidatorTestFixture.appWithVehicleValue("SUV"), null).size(),
            "Valor SUV en lista permitida → sin errores");
    }

    // ── Test 2: valor NO en la lista → genera ERROR ───────────────────────

    public static void testValueNotInAllowedList_generatesError() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false);
        List<ValidationError> errors =
            v.validate(ValidatorTestFixture.appWithVehicleValue("Minivan"), null);
        Assert.assertEquals(1, errors.size(),
            "Valor Minivan fuera de lista → 1 error");
        Assert.assertEquals(EnumValidator.VALUE_NOT_IN_ALLOWED_LIST, errors.get(0).getCode(),
            "Código debe ser VALUE_NOT_IN_ALLOWED_LIST");
    }

    // ── Test 3: case-insensitive → coincide con minúsculas ────────────────

    public static void testCaseInsensitiveMatch_noErrors() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, true);
        Assert.assertEquals(0,
            v.validate(ValidatorTestFixture.appWithVehicleValue("suv"), null).size(),
            "Con caseInsensitive=true, 'suv' debe coincidir con 'SUV'");
    }

    // ── Test 4: case-sensitive, minúsculas NO coinciden ───────────────────

    public static void testCaseSensitiveMismatch_generatesError() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false);
        Assert.assertEquals(1,
            v.validate(ValidatorTestFixture.appWithVehicleValue("suv"), null).size(),
            "Con caseInsensitive=false, 'suv' NO debe coincidir con 'SUV'");
    }

    // ── Test 5: valor null → sin errores ─────────────────────────────────

    public static void testNullValue_noErrors() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false);
        Assert.assertEquals(0,
            v.validate(ValidatorTestFixture.appWithVehicleValue(null), null).size(),
            "Valor null → sin errores (sin valor que validar)");
    }

    // ── Test 6: todos los valores de la lista pasan ───────────────────────

    public static void testAllAllowedValues_allPass() throws Exception {
        EnumValidator v = new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false);
        for (String val : VEHICLE_VALUES) {
            List<ValidationError> errors =
                v.validate(ValidatorTestFixture.appWithVehicleValue(val), null);
            Assert.assertEquals(0, errors.size(),
                "Valor '" + val + "' en lista → sin errores");
        }
    }

    // ── Test 7: lista de valores permitidos vacía → IllegalArgumentException ──

    public static void testEmptyAllowedValues_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE,
                java.util.Collections.emptyList(), false),
            "Lista vacía de valores permitidos debe lanzar IllegalArgumentException");
    }

    // ── Test 8: null application → IllegalArgumentException ───────────────

    public static void testNullApplication_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new EnumValidator(ValidatorTestFixture.ATTR_VEHICLE, VEHICLE_VALUES, false)
                .validate(null, null),
            "Application null debe lanzar IllegalArgumentException");
    }
}