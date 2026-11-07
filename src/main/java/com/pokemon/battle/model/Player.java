package com.pokemon.battle.model;
import com.pokemon.battle.exceptions.InvalidDataException;

/**
 * Human trainer (table JUGADOR). It keeps the number of battles won and lost.
 */
public class Player extends Trainer {

    private int battlesWon;
    private int battlesLost;

    /**
     * @param id          trainer identifier
     * @param name        trainer name
     * @param battlesWon  battles won so far (BATALLAS_GANADAS)
     * @param battlesLost battles lost so far (BATALLAS_PERDIDAS)
     */
    public Player(int id, String name, int battlesWon, int battlesLost) {
        super(id, name);
        this.battlesWon = battlesWon;
        this.battlesLost = battlesLost;
    }

    /**
     * @return the battles won
     */
    public int getBattlesWon() {
        return battlesWon;
    }

    /**
     * @return the battles lost
     */
    public int getBattlesLost() {
        return battlesLost;
    }

    /**
     * increment in one the Battles won counter
     */
    public void incrementBattlesWon() {
        this.battlesWon++;
    }

    /**
     * increment in one the Battles lost counter
     */
    public void incrementBattlesLost() {
        this.battlesLost++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getName() + " (ganadas: " + battlesWon + ", perdidas: " + battlesLost + ")";
    }
}
