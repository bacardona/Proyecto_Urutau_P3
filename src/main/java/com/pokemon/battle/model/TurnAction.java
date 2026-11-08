package com.pokemon.battle.model;

/**
 * What a side does in one turn: attack with a move or switch to another Pokemon
 * of its team. Each side does exactly one action per turn.
 */
public final class TurnAction {

    private final Move move;
    private final int switchIndex;

    private TurnAction(Move move, int switchIndex) {
        this.move = move;
        this.switchIndex = switchIndex;
    }

    /**
     * @param move the move to use
     * @return an action that attacks with that move
     */
    public static TurnAction attack(Move move) {
        return new TurnAction(move, -1);
    }

    /**
     * @param teamIndex position (starting at 0) of the Pokemon that comes in
     * @return an action that switches to that Pokemon
     */
    public static TurnAction switchTo(int teamIndex) {
        return new TurnAction(null, teamIndex);
    }

    /**
     * @return true when the action is a switch, false when it is an attack
     */
    public boolean isSwitch() {
        return move == null;
    }

    /**
     * @return the move to use, or null when the action is a switch
     */
    public Move getMove() {
        return move;
    }

    /**
     * @return the position of the Pokemon that comes in, or -1 when the action is an attack
     */
    public int getSwitchIndex() {
        return switchIndex;
    }
}