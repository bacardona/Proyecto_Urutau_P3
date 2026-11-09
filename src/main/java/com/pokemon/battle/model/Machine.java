package com.pokemon.battle.model;

/**
 * Trainer controlled by the computer (table MAQUINA).
 */
public class Machine extends Trainer {

    private String rank;

    /**
     * @param id   trainer identifier
     * @param name trainer name
     * @param rank rank of the machine (RANGO)
     */
    public Machine(int id, String name, String rank) {
        super(id, name);
        this.rank = rank;
    }

    /**
     * @return the rank of the machine
     */
    public String getRank() {
        return rank;
    }

    /**
     * @param rank the new rank of the machine
     */
    public void setRank(String rank) {
        this.rank = rank;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getName() + " (rango: " + rank + ")";
    }
}
