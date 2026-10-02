package com.validador.aces.validation;

import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ValidationError;

/**
 * Contrato base de todas las estrategias de validación del sistema.
 *
 * <p>Cada implementación concreta de {@code Validator} encapsula una regla
 * (o conjunto de reglas) de validación específica aplicada a una
 * {@link Application} dentro del contexto de un {@link Catalog} (algunas
 * implementaciones pueden ignorar el catálogo si no lo necesitan, por
 * ejemplo las utilidades genéricas configurables por constructor como
 * {@code RangeValidator}, {@code EnumValidator} y
 * {@code DateFormatValidator}).</p>
 *
 * <p>Las implementaciones deben acumular <strong>todos</strong> los
 * {@link ValidationError} encontrados, sin detenerse ante el primer
 * problema detectado. Si no se encuentra ningún problema, deben retornar
 * una lista vacía; el contrato de este método nunca retorna {@code null}.</p>
 */
public abstract class Validator {

    /**
     * Ejecuta la regla de validación representada por esta estrategia
     * sobre la aplicación dada.
     *
     * @param application aplicación ACES a validar
     * @param catalog     catálogo maestro de referencia (puede ser ignorado
     *                    por implementaciones que no lo necesiten)
     * @return lista de errores/advertencias encontrados; lista vacía si no
     *         hay problemas, nunca {@code null}
     */
    public abstract List<ValidationError> validate(Application application, Catalog catalog);
}
