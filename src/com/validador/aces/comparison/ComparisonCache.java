package com.validador.aces.comparison;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.validador.aces.models.ComparisonResult;

/**
 * Caché en memoria de {@link ComparisonResult} previamente calculados,
 * indexados por una clave compuesta de identificador de aplicación e
 * identificador de catálogo.
 *
 * <p>Internamente utiliza un {@link LinkedHashMap} en orden de inserción
 * con una política de evicción FIFO simple: cuando el tamaño excede
 * {@link #getMaxSize()}, la entrada más antigua se elimina automáticamente
 * al insertar una nueva. Esto evita que el caché crezca sin límite cuando
 * se procesan lotes grandes de aplicaciones.</p>
 *
 * <p>Cada entrada registra, además del resultado, el timestamp de
 * inserción. Dicho timestamp no forma parte de la clave de búsqueda; se
 * usa exclusivamente para expiración por antigüedad vía
 * {@link #evictOlderThan(Duration)}.</p>
 */
public class ComparisonCache {

    private static final int DEFAULT_MAX_SIZE = 100;

    private final int maxSize;
    private final Map<String, CacheEntry> cache;

    /** Crea un caché con el tamaño máximo por defecto ({@value #DEFAULT_MAX_SIZE} entradas). */
    public ComparisonCache() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Crea un caché con el tamaño máximo indicado.
     *
     * @param maxSize cantidad máxima de entradas que el caché puede contener simultáneamente
     * @throws IllegalArgumentException si {@code maxSize} no es mayor que cero
     */
    public ComparisonCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize debe ser mayor que cero");
        }
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > ComparisonCache.this.maxSize;
            }
        };
    }

    /**
     * Guarda un resultado de comparación bajo la clave compuesta de
     * aplicación + catálogo. El timestamp de inserción se registra como
     * metadata de la entrada, usada únicamente por {@link #evictOlderThan}.
     *
     * @param applicationKey identificador de la aplicación (por ejemplo, su nombre)
     * @param catalogKey      identificador del catálogo (por ejemplo, su nombre)
     * @param result          resultado de comparación a almacenar
     * @throws IllegalArgumentException si algún parámetro es null, o si las claves están vacías
     */
    public void put(String applicationKey, String catalogKey, ComparisonResult result) {
        validateKey(applicationKey, "applicationKey");
        validateKey(catalogKey, "catalogKey");
        if (result == null) {
            throw new IllegalArgumentException("ComparisonResult no puede ser null");
        }
        cache.put(buildKey(applicationKey, catalogKey), new CacheEntry(result, LocalDateTime.now()));
    }

    /**
     * Recupera el resultado de comparación almacenado bajo la clave
     * compuesta indicada.
     *
     * @param applicationKey identificador de la aplicación
     * @param catalogKey      identificador del catálogo
     * @return el {@link ComparisonResult} almacenado, o null si no existe entrada para esa clave
     * @throws IllegalArgumentException si alguna clave es null o vacía
     */
    public ComparisonResult get(String applicationKey, String catalogKey) {
        validateKey(applicationKey, "applicationKey");
        validateKey(catalogKey, "catalogKey");
        CacheEntry entry = cache.get(buildKey(applicationKey, catalogKey));
        return entry == null ? null : entry.getResult();
    }

    /**
     * Determina si existe una entrada almacenada para la clave compuesta
     * indicada.
     *
     * @param applicationKey identificador de la aplicación
     * @param catalogKey      identificador del catálogo
     * @return true si existe una entrada para esa clave
     * @throws IllegalArgumentException si alguna clave es null o vacía
     */
    public boolean contains(String applicationKey, String catalogKey) {
        validateKey(applicationKey, "applicationKey");
        validateKey(catalogKey, "catalogKey");
        return cache.containsKey(buildKey(applicationKey, catalogKey));
    }

    /**
     * Elimina todas las entradas cuyo timestamp de inserción sea más
     * antiguo que {@code maxAge} respecto al momento actual.
     *
     * @param maxAge antigüedad máxima permitida para conservar una entrada
     * @throws IllegalArgumentException si {@code maxAge} es null
     */
    public void evictOlderThan(Duration maxAge) {
        if (maxAge == null) {
            throw new IllegalArgumentException("maxAge no puede ser null");
        }
        LocalDateTime threshold = LocalDateTime.now().minus(maxAge);
        cache.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(threshold));
    }

    /** Elimina todas las entradas del caché. */
    public void clear() {
        cache.clear();
    }

    /** @return cantidad actual de entradas almacenadas en el caché. */
    public int size() {
        return cache.size();
    }

    /** @return tamaño máximo configurado para este caché. */
    public int getMaxSize() {
        return maxSize;
    }

    private void validateKey(String key, String paramName) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " no puede ser null ni vacío");
        }
    }

    private String buildKey(String applicationKey, String catalogKey) {
        return applicationKey + "::" + catalogKey;
    }

    /**
     * Entrada interna del caché: asocia un {@link ComparisonResult} con el
     * timestamp en que fue insertado.
     */
    private static final class CacheEntry {
        private final ComparisonResult result;
        private final LocalDateTime timestamp;

        CacheEntry(ComparisonResult result, LocalDateTime timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }

        ComparisonResult getResult() {
            return result;
        }

        LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
