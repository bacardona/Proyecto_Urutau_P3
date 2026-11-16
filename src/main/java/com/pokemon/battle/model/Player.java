package com.pokemon.battle.model;
import com.pokemon.battle.exceptions.InvalidDataException;

/**
 * Human trainer (table JUGADOR). It keeps the number of battles won and lost.
 */
public class Player extends Trainer {

    private int battlesWon;
    private int battlesLost;
    /** Games played, from func_partidas_jugadas_jugador (-1 = not loaded). */
    private int gamesPlayed = -1;
    /** Win rate in percent, from func_winrate_entrendaor (-1 = not loaded). */
    private double winRate = -1;

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
     * @return games played according to the database function, or -1 if not loaded
     */
    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    /**
     * @return win rate (0-100) according to the database function, or -1 if not loaded
     */
    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
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
