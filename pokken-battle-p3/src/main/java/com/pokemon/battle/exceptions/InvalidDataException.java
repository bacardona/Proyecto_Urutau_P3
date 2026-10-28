package com.pokemon.battle.exceptions;

/**
 * Thrown when an input value does not meet format or range criteria.
 */
public class InvalidDataException extends GameException {

    private static final long serialVersionUID = 1L;

    /**
     * @param fieldName   name of the invalid field (shown to the user)
     * @param description what is wrong with the value
     */
    public InvalidDataException(String fieldName, String description) {
        super("El campo '" + fieldName + "' no es valido: " + description, "INVALID_DATA");
    }
}
