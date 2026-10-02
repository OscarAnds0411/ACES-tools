package com.validador.aces.validation;

import java.util.ArrayList;
import java.util.List;

import com.validador.aces.models.Application;
import com.validador.aces.models.Catalog;
import com.validador.aces.models.ErrorSeverity;
import com.validador.aces.models.ValidationError;

/**
 * Implementación del patrón Composite para validadores: ejecuta una cadena
 * de {@link Validator} en el orden en que fueron agregados y agrega todos
 * los {@link ValidationError} producidos en una única lista.
 *
 * <p>Opcionalmente, mediante {@link #setStopOnFirstCriticalError(boolean)},
 * puede configurarse para abortar la ejecución de la cadena en cuanto uno
 * de los validadores produzca al menos un error de severidad
 * {@link ErrorSeverity#ERROR}, sin ejecutar los validadores restantes.</p>
 */
public class CompositeValidator extends Validator {

    private final List<Validator> validators = new ArrayList<>();
    private boolean stopOnFirstCriticalError = false;

    /**
     * Agrega un validador a la cadena. Los validadores se ejecutan en el
     * mismo orden en que fueron agregados.
     *
     * @param validator validador a agregar
     * @throws IllegalArgumentException si {@code validator} es null
     */
    public void addValidator(Validator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("Validator no puede ser null");
        }
        this.validators.add(validator);
    }

    /**
     * Configura si la cadena debe abortar la ejecución en cuanto el
     * validador actual produzca al menos un error crítico.
     *
     * @param stopOnFirstCriticalError true para abortar ante el primer error crítico
     */
    public void setStopOnFirstCriticalError(boolean stopOnFirstCriticalError) {
        this.stopOnFirstCriticalError = stopOnFirstCriticalError;
    }

    /** @return true si la cadena aborta ante el primer error crítico. */
    public boolean isStopOnFirstCriticalError() {
        return stopOnFirstCriticalError;
    }

    @Override
    public List<ValidationError> validate(Application application, Catalog catalog) {
        List<ValidationError> accumulated = new ArrayList<>();

        for (Validator validator : validators) {
            List<ValidationError> result = validator.validate(application, catalog);
            accumulated.addAll(result);

            if (stopOnFirstCriticalError && containsCriticalError(result)) {
                break;
            }
        }

        return accumulated;
    }

    private boolean containsCriticalError(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            if (error.getSeverity() == ErrorSeverity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
