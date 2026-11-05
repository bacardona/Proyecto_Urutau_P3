package com.pokemon.battle.exceptions;

/**
 * Thrown when a requested resource (for example a Pokemon) does not exist.
 */
public class ResourceNotFoundException extends GameException {

    private static final long serialVersionUID = 1L;

    /**
     * @param resourceType kind of resource that was searched (shown to the user)
     * @param identifier   identifier that was searched
     */
    public ResourceNotFoundException(String resourceType, String identifier) {
        super("El " + resourceType + " con identificador " + identifier + " no fue encontrado",
                "RESOURCE_NOT_FOUND");
    }
}
