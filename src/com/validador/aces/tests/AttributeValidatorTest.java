package com.validador.aces.tests;

import java.util.List;

import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;
import com.validador.aces.validation.AttributeValidator;

/**
 * Unit tests para {@link AttributeValidator} (TASK-039).
 * Construye los datos de prueba en memoria sin I/O de archivo.
 */
public final class AttributeValidatorTest {

    private AttributeValidatorTest() {}

    // ── Test 1: atributo requerido presente → sin errores ─────────────────

    public static void testRequiredAttributePresent_noErrors() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = v.validate(ValidatorTestFixture.appAllPresent(), cat);
        Assert.assertEquals(0, errors.size(),
            "Con todos los atributos requeridos presentes no debe haber errores");
    }

    // ── Test 2: atributo requerido faltante → genera ERROR ────────────────

    public static void testRequiredAttributeMissing_generatesError() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = v.validate(ValidatorTestFixture.appRequiredMissing(), cat);
        Assert.assertEquals(1, errors.size(),
            "Un atributo requerido faltante debe generar exactamente 1 error");
        Assert.assertEquals(AttributeValidator.MISSING_REQUIRED_ATTRIBUTE,
            errors.get(0).getCode(), "Código del error debe ser MISSING_REQUIRED_ATTRIBUTE");
        Assert.assertEquals(ErrorSeverity.ERROR, errors.get(0).getSeverity(),
            "Severidad debe ser ERROR");
        Assert.assertEquals(ValidatorTestFixture.ATTR_ENGINE,
            errors.get(0).getAttributeName(), "Atributo reportado debe ser EngineLiters");
    }

    // ── Test 3: atributo opcional faltante → sin errores ─────────────────

    public static void testOptionalAttributeMissing_noErrors() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = v.validate(ValidatorTestFixture.appOptionalMissing(), cat);
        Assert.assertEquals(0, errors.size(),
            "Un atributo opcional faltante NO debe generar error");
    }

    // ── Test 4: producto no encontrado en catálogo → genera WARNING ────────

    public static void testProductLineNotFound_generatesWarning() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = v.validate(ValidatorTestFixture.appUnknownProduct(), cat);
        Assert.assertEquals(1, errors.size(),
            "Producto no encontrado debe generar 1 entrada");
        Assert.assertEquals(ErrorSeverity.WARNING, errors.get(0).getSeverity(),
            "Producto no encontrado debe ser WARNING, no ERROR");
        Assert.assertEquals(AttributeValidator.PRODUCT_LINE_NOT_FOUND,
            errors.get(0).getCode(), "Código debe ser PRODUCT_LINE_NOT_FOUND");
    }

    // ── Test 5: atributo NOT_REQUIRED faltante → sin errores ──────────────

    public static void testNotRequiredAttributeMissing_noErrors() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        // Solo engine presente; Region (NOT_REQUIRED) y VehicleType (OPTIONAL) ausentes
        com.validador.aces.models.Application app =
            new com.validador.aces.models.Application("Make", "Model", "2000",
                ValidatorTestFixture.PRODUCT_NAME);
        app.setAttributeValue(ValidatorTestFixture.ATTR_ENGINE, "5.3");
        List<ValidationError> errors = v.validate(app, cat);
        Assert.assertEquals(0, errors.size(),
            "Atributos OPTIONAL y NOT_REQUIRED faltantes no deben generar errores");
    }

    // ── Test 6: null application → IllegalArgumentException ───────────────

    public static void testNullApplication_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new AttributeValidator().validate(null, ValidatorTestFixture.buildCatalog()),
            "Application null debe lanzar IllegalArgumentException");
    }

    // ── Test 7: null catalog → IllegalArgumentException ───────────────────

    public static void testNullCatalog_throwsException() {
        Assert.assertThrows(IllegalArgumentException.class,
            () -> new AttributeValidator().validate(ValidatorTestFixture.appAllPresent(), null),
            "Catalog null debe lanzar IllegalArgumentException");
    }

    // ── Test 8: todos los required presentes → compliance 100 % ──────────

    public static void testAllRequiredPresent_zeroMissingRequired() throws Exception {
        AttributeValidator v = new AttributeValidator();
        Catalog cat = ValidatorTestFixture.buildCatalog();
        List<ValidationError> errors = v.validate(ValidatorTestFixture.appAllPresent(), cat);
        long missing = ValidatorTestFixture.countByCode(
            errors, AttributeValidator.MISSING_REQUIRED_ATTRIBUTE);
        Assert.assertEquals(0, (int) missing,
            "Con todos los required presentes no debe haber MISSING_REQUIRED_ATTRIBUTE");
    }
}