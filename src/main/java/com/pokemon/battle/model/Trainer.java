package com.pokemon.battle.model;

/**
 * Someone who takes part in a battle (table ENTRENADOR). It is never created
 * on its own: every trainer is either a {@link Player} or a {@link Machine}.
 */
public abstract class Trainer {

    private final int id;
    private String name;

    /**
     * @param id   trainer identifier (ID_ENTRENADOR)
     * @param name trainer name (NOMBRE)
     */
    public Trainer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return the trainer identifier
     */
    public int getId() {
        return id;
    }

    /**
     * @return the trainer name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the new trainer name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Builds a short description of the trainer to show it in a menu.
     *
     * @return the description
     */
    public abstract String getDescription();
}
