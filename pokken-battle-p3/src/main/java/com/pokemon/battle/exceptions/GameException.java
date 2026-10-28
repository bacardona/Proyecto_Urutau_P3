package com.pokemon.battle.exceptions;

/**
 * Root of the custom exception hierarchy of the game.
 * It is unchecked so that service signatures stay clean; the UI layer is
 * responsible for catching each subtype and showing a friendly message.
 */
public abstract class GameException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * Creates an exception with a message and a generic error code.
     *
     * @param message description of the problem
     */
    protected GameException(String message) {
        this(message, "GAME_ERROR");
    }

    /**
     * Creates an exception with a message and a specific error code.
     *
     * @param message   description of the problem
     * @param errorCode machine-readable code of the error
     */
    protected GameException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception that keeps the original cause.
     *
     * @param message   description of the problem
     * @param errorCode machine-readable code of the error
     * @param cause     original exception
     */
    protected GameException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * @return the machine-readable error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
