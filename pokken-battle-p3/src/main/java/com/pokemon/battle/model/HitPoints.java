package com.pokemon.battle.model;

/**
 * Hit points (PS) of a combatant.
 */
public class HitPoints {

    private final int max;
    private int current;

    /**
     * @param max maximum hit points; the combatant starts with all of them
     */
    public HitPoints(int max) {
        this.max = Math.max(max, 0);
        this.current = this.max;
    }

    /**
     * @return the current hit points
     */
    public int getCurrent() {
        return current;
    }

    /**
     * @return the maximum hit points
     */
    public int getMax() {
        return max;
    }

    /**
     * Removes hit points without going below zero. Package-private: only the
     * model rules (moves and status conditions) can hurt a combatant.
     *
     * @param damage amount of hit points to remove
     */
    void subtract(int damage) {
        current = Math.max(0, current - damage);
    }

    /**
     * @return true when no hit points are left
     */
    public boolean isFainted() {
        return current <= 0;
    }
}
