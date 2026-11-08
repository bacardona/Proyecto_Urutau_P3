package com.pokemon.battle.model;

/**
 * A battle between two sides. It is over when one of the teams has no Pokemon
 * able to fight.
 */
public class Battle {

    private final BattleSide first;
    private final BattleSide second;

    /**
     * @param first  the first side (always a player)
     * @param second the second side (a machine or another player)
     */
    public Battle(BattleSide first, BattleSide second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @return the first side
     */
    public BattleSide getFirst() {
        return first;
    }

    /**
     * @return the second side
     */
    public BattleSide getSecond() {
        return second;
    }

    /**
     * @return true when the second side is a machine
     */
    public boolean isVsMachine() {
        return second.isMachine();
    }

    /**
     * @return true when at least one team has no Pokemon able to fight
     */
    public boolean isOver() {
        return !first.getTeam().hasAvailable() || !second.getTeam().hasAvailable();
    }

    /**
     * Finds the winner. In the very unlikely case that the last Pokemon of both
     * teams faint in the same turn (poison or burn), the first side wins.
     *
     * @return the winning side, or null while the battle is not over
     */
    public BattleSide getWinner() {
        if (!isOver()) {
            return null;
        }
        boolean secondWins = second.getTeam().hasAvailable() && !first.getTeam().hasAvailable();
        return secondWins ? second : first;
    }

    /**
     * @return the losing side, or null while the battle is not over
     */
    public BattleSide getLoser() {
        BattleSide winner = getWinner();
        if (winner == null) {
            return null;
        }
        return winner == first ? second : first;
    }
}