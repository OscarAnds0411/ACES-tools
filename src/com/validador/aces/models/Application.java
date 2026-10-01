package com.validador.aces.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa una fila de la hoja "Applications" del archivo ACES.
 *
 * <p>Los campos de metadata (make, model, year, product, partNumber, mfrLabel,
 * position) corresponden a las primeras columnas del Excel. El mapa {@code data}
 * cubre los atributos técnicos de las columnas restantes (8-94).</p>
 *
 * <p>El campo {@code product} es la clave de vínculo con el catálogo:
 * debe coincidir con {@code ProductLine.name}.</p>
 */
public class Application implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── Metadata (columnas 1-7 del ACES Excel) ────────────────────────────────
    /** Columna 1: Make (ej: "Chevrolet") */
    private String make;
    /** Columna 2: Model (ej: "Silverado 1500") */
    private String model;
    /** Columna 3: Year (ej: "1999") */
    private String year;
    /** Columna 4: Product — clave de vínculo con ProductLine.name del catálogo */
    private String product;
    /** Columna 5: PartNumber (ej: "KGCA-0003") */
    private String partNumber;
    /** Columna 6: MfrLabel */
    private String mfrLabel;
    /** Columna 7: Position (ej: "Front") */
    private String position;

    // ── Campos heredados ──────────────────────────────────────────────────────
    /** Nombre lógico de la aplicación (puede derivarse de make+model+year o del archivo). */
    private String name;
    /** Versión del archivo ACES fuente. */
    private String version;

    // ── Atributos técnicos (columnas 8-94) ───────────────────────────────────
    private Map<String, Object> data;

    // ── Constructores ─────────────────────────────────────────────────────────

    /**
     * Constructor original — mantiene compatibilidad con código existente.
     *
     * @param name    nombre lógico de la aplicación
     * @param version versión del archivo ACES
     * @throws IllegalArgumentException si name es null
     */
    public Application(String name, String version) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name no puede ser null ni vacío");
        }
        this.name    = name;
        this.version = version;
        this.data    = new HashMap<>();
    }

    /**
     * Constructor para parseo directo desde el Excel ACES.
     *
     * @param make      columna 1: fabricante del vehículo
     * @param model     columna 2: modelo del vehículo
     * @param year      columna 3: año del vehículo
     * @param product   columna 4: producto (vincula con ProductLine.name del catálogo)
     * @throws IllegalArgumentException si make es null
     */
    public Application(String make, String model, String year, String product) {
        if (make == null || make.trim().isEmpty()) {
            throw new IllegalArgumentException("Application make no puede ser null ni vacío");
        }
        this.make    = make;
        this.model   = model;
        this.year    = year;
        this.product = product;
        this.name    = make + " " + model + " " + year;
        this.data    = new HashMap<>();
    }

    // ── Método de vínculo con el catálogo ─────────────────────────────────────

    /**
     * Retorna el nombre del producto que vincula esta aplicación con el catálogo.
     * Equivale al valor de la columna "Product" del ACES, que debe coincidir
     * con {@code ProductLine.name} en el catálogo.
     *
     * @return nombre de producto, ej: "A/C Condenser"
     */
    public String getProductName() {
        return product;
    }

    // ── Acceso a atributos técnicos ───────────────────────────────────────────

    /**
     * Obtiene el valor de un atributo técnico.
     *
     * @param attributeName nombre del atributo (encabezado de columna en el Excel)
     * @return valor del atributo, null si no existe
     */
    public Object getAttributeValue(String attributeName) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            return null;
        }
        return data.get(attributeName);
    }

    /**
     * Establece el valor de un atributo técnico.
     *
     * @param attributeName nombre del atributo
     * @param value         valor del atributo
     * @throws IllegalArgumentException si attributeName es null
     */
    public void setAttributeValue(String attributeName, Object value) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Attribute name no puede ser null ni vacío");
        }
        data.put(attributeName, value);
    }

    /**
     * Obtiene todos los datos de atributos técnicos.
     *
     * @return copia del mapa de atributos
     */
    public Map<String, Object> getData() {
        return new HashMap<>(data);
    }

    /**
     * Reemplaza todos los datos de atributos técnicos.
     *
     * @param data mapa de atributos
     * @throws IllegalArgumentException si data es null
     */
    public void setData(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Data no puede ser null");
        }
        this.data = new HashMap<>(data);
    }

    /**
     * Verifica si existe un atributo técnico.
     *
     * @param attributeName nombre del atributo
     * @return true si el atributo existe en el mapa de datos
     */
    public boolean hasAttribute(String attributeName) {
        if (attributeName == null || attributeName.trim().isEmpty()) {
            return false;
        }
        return data.containsKey(attributeName);
    }

    /** @return número de atributos técnicos en {@code data}. */
    public int getAttributeCount() {
        return data.size();
    }

    /** Limpia todos los datos técnicos. */
    public void clearData() {
        data.clear();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name no puede ser null ni vacío");
        }
        this.name = name;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getMfrLabel() { return mfrLabel; }
    public void setMfrLabel(String mfrLabel) { this.mfrLabel = mfrLabel; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Application that = (Application) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public String toString() {
        return "Application{" +
                "make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", year='" + year + '\'' +
                ", product='" + product + '\'' +
                ", partNumber='" + partNumber + '\'' +
                ", position='" + position + '\'' +
                ", attributeCount=" + data.size() +
                '}';
    }
}
