package com.validador.aces.tests;

import java.util.Arrays;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Attribute;
import com.validador.aces.models.AttributeRequirement;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ProductLine;
import com.validador.aces.models.ValidationError;
import com.validador.aces.models.ErrorSeverity;

/**
 * Datos de prueba compartidos entre los tests del Componente 8.
 * Todos los métodos son estáticos para poder usarse desde cualquier test.
 */
final class ValidatorTestFixture {

    static final String PRODUCT_NAME    = "Test Product";
    static final String ATTR_ENGINE     = "EngineLiters";   // REQUIRED
    static final String ATTR_VEHICLE    = "VehicleType";    // OPTIONAL
    static final String ATTR_REGION     = "Region";         // NOT_REQUIRED

    private ValidatorTestFixture() {}

    // ── Catálogo de prueba ────────────────────────────────────────────────

    /** Catálogo con un único ProductLine que tiene 3 atributos con distintos requirements. */
    static Catalog buildCatalog() {
        Catalog catalog = new Catalog("TEST", "Test Catalog");
        ProductLine pl = new ProductLine("1", PRODUCT_NAME, "Category", "SubCat", null);
        pl.addAttribute(new Attribute(ATTR_ENGINE,  ATTR_ENGINE,  AttributeRequirement.REQUIRED));
        pl.addAttribute(new Attribute(ATTR_VEHICLE, ATTR_VEHICLE, AttributeRequirement.OPTIONAL));
        pl.addAttribute(new Attribute(ATTR_REGION,  ATTR_REGION,  AttributeRequirement.NOT_REQUIRED));
        catalog.addProductLine(pl);
        return catalog;
    }

    // ── Aplicaciones de prueba ────────────────────────────────────────────

    /** Aplicación con todos los atributos presentes (100 % compliant). */
    static Application appAllPresent() {
        Application app = new Application("Make", "Model", "2000", PRODUCT_NAME);
        app.setAttributeValue(ATTR_ENGINE,  "5.3");
        app.setAttributeValue(ATTR_VEHICLE, "SUV");
        app.setAttributeValue(ATTR_REGION,  "North");
        return app;
    }

    /** Aplicación con atributo REQUIRED faltante (EngineLiters = null). */
    static Application appRequiredMissing() {
        Application app = new Application("Make", "Model", "2000", PRODUCT_NAME);
        // EngineLiters no se setea → null
        app.setAttributeValue(ATTR_VEHICLE, "SUV");
        return app;
    }

    /** Aplicación con atributo OPTIONAL faltante (VehicleType = null). */
    static Application appOptionalMissing() {
        Application app = new Application("Make", "Model", "2000", PRODUCT_NAME);
        app.setAttributeValue(ATTR_ENGINE,  "5.3");
        // VehicleType no se setea → null
        return app;
    }

    /** Aplicación cuyo nombre de producto NO existe en el catálogo. */
    static Application appUnknownProduct() {
        return new Application("Make", "Model", "2000", "Producto Inexistente XYZ");
    }

    /** Aplicación con valor numérico en ATTR_ENGINE. */
    static Application appWithEngineValue(Object value) {
        Application app = new Application("Make", "Model", "2000", PRODUCT_NAME);
        app.setAttributeValue(ATTR_ENGINE,  value);
        app.setAttributeValue(ATTR_VEHICLE, "SUV");
        return app;
    }

    /** Aplicación con valor de ATTR_VEHICLE. */
    static Application appWithVehicleValue(Object value) {
        Application app = new Application("Make", "Model", "2000", PRODUCT_NAME);
        app.setAttributeValue(ATTR_ENGINE,  "5.3");
        app.setAttributeValue(ATTR_VEHICLE, value);
        return app;
    }

    // ── Utilidades de assertion para ValidationError ──────────────────────

    static long countByCode(List<ValidationError> errors, String code) {
        return errors.stream().filter(e -> code.equals(e.getCode())).count();
    }

    static long countBySeverity(List<ValidationError> errors, ErrorSeverity severity) {
        return errors.stream().filter(e -> severity == e.getSeverity()).count();
    }
}