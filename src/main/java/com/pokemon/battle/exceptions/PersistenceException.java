package com.pokemon.battle.exceptions;

/**
 * Thrown when a file cannot be read or written, or its content is corrupt.
 */
public class PersistenceException extends GameException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description including file name and operation
     * @param cause   original exception, kept for traceability
     */
    public PersistenceException(String message, Throwable cause) {
        super(message, "PERSISTENCE", cause);
    }
}
