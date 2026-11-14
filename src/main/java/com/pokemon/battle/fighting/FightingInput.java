package com.pokemon.battle.fighting;

/** Tracks held movement keys independently for both players. */
public class FightingInput {

    private boolean playerOneLeft;
    private boolean playerOneRight;
    private boolean playerTwoLeft;
    private boolean playerTwoRight;
    private boolean playerOneJumpHeld;
    private boolean playerTwoJumpHeld;
    private boolean playerOneJumpRequested;
    private boolean playerTwoJumpRequested;
    private boolean playerOneAttackOneHeld;
    private boolean playerOneAttackTwoHeld;
    private boolean playerOneAttackThreeHeld;
    private boolean playerOneAttackFourHeld;
    private boolean playerOneAttackOneRequested;
    private boolean playerOneAttackTwoRequested;
    private boolean playerOneAttackThreeRequested;
    private boolean playerOneAttackFourRequested;
    private boolean playerTwoAttackOneHeld;
    private boolean playerTwoAttackTwoHeld;
    private boolean playerTwoAttackThreeHeld;
    private boolean playerTwoAttackFourHeld;
    private boolean playerTwoAttackOneRequested;
    private boolean playerTwoAttackTwoRequested;
    private boolean playerTwoAttackThreeRequested;
    private boolean playerTwoAttackFourRequested;
    private boolean playerOneEvolutionHeld;
    private boolean playerTwoEvolutionHeld;
    private boolean playerOneEvolutionRequested;
    private boolean playerTwoEvolutionRequested;
    private boolean playerOneUltimateHeld;
    private boolean playerTwoUltimateHeld;
    private boolean playerOneUltimateRequested;
    private boolean playerTwoUltimateRequested;
    private boolean playerOneBlockHeld;
    private boolean playerTwoBlockHeld;
    private int playerOneAttackRequestIndex = -1;
    private int playerTwoAttackRequestIndex = -1;

    public void setPlayerOneLeft(boolean pressed) { playerOneLeft = pressed; }
    public void setPlayerOneRight(boolean pressed) { playerOneRight = pressed; }
    public void setPlayerTwoLeft(boolean pressed) { playerTwoLeft = pressed; }
    public void setPlayerTwoRight(boolean pressed) { playerTwoRight = pressed; }
    public void setPlayerOneBlock(boolean pressed) { playerOneBlockHeld = pressed; }
    public void setPlayerTwoBlock(boolean pressed) { playerTwoBlockHeld = pressed; }
    public boolean isPlayerOneBlockingRequested() { return playerOneBlockHeld; }
    public boolean isPlayerTwoBlockingRequested() { return playerTwoBlockHeld; }

    public void setPlayerOneJump(boolean pressed) {
        if (pressed && !playerOneJumpHeld) playerOneJumpRequested = true;
        playerOneJumpHeld = pressed;
    }

    public void setPlayerTwoJump(boolean pressed) {
        if (pressed && !playerTwoJumpHeld) playerTwoJumpRequested = true;
        playerTwoJumpHeld = pressed;
    }

    public void setPlayerOneAttackOne(boolean pressed) {
        if (pressed && !playerOneAttackOneHeld) { playerOneAttackOneRequested = true; playerOneAttackRequestIndex = 0; }
        playerOneAttackOneHeld = pressed;
    }

    public void setPlayerOneAttackTwo(boolean pressed) {
        if (pressed && !playerOneAttackTwoHeld) { playerOneAttackTwoRequested = true; playerOneAttackRequestIndex = 1; }
        playerOneAttackTwoHeld = pressed;
    }

    public void setPlayerOneAttackThree(boolean pressed) {
        if (pressed && !playerOneAttackThreeHeld) { playerOneAttackThreeRequested = true; playerOneAttackRequestIndex = 2; }
        playerOneAttackThreeHeld = pressed;
    }

    public void setPlayerOneAttackFour(boolean pressed) {
        if (pressed && !playerOneAttackFourHeld) { playerOneAttackFourRequested = true; playerOneAttackRequestIndex = 3; }
        playerOneAttackFourHeld = pressed;
    }

    public void setPlayerTwoAttackOne(boolean pressed) {
        if (pressed && !playerTwoAttackOneHeld) { playerTwoAttackOneRequested = true; playerTwoAttackRequestIndex = 0; }
        playerTwoAttackOneHeld = pressed;
    }

    public void setPlayerTwoAttackTwo(boolean pressed) {
        if (pressed && !playerTwoAttackTwoHeld) { playerTwoAttackTwoRequested = true; playerTwoAttackRequestIndex = 1; }
        playerTwoAttackTwoHeld = pressed;
    }

    public void setPlayerTwoAttackThree(boolean pressed) {
        if (pressed && !playerTwoAttackThreeHeld) { playerTwoAttackThreeRequested = true; playerTwoAttackRequestIndex = 2; }
        playerTwoAttackThreeHeld = pressed;
    }

    public void setPlayerTwoAttackFour(boolean pressed) {
        if (pressed && !playerTwoAttackFourHeld) { playerTwoAttackFourRequested = true; playerTwoAttackRequestIndex = 3; }
        playerTwoAttackFourHeld = pressed;
    }

    public void setPlayerOneEvolution(boolean pressed) {
        if (pressed && !playerOneEvolutionHeld) playerOneEvolutionRequested = true;
        playerOneEvolutionHeld = pressed;
    }

    public void setPlayerTwoEvolution(boolean pressed) {
        if (pressed && !playerTwoEvolutionHeld) playerTwoEvolutionRequested = true;
        playerTwoEvolutionHeld = pressed;
    }

    public void setPlayerOneUltimate(boolean pressed) {
        if (pressed && !playerOneUltimateHeld) playerOneUltimateRequested = true;
        playerOneUltimateHeld = pressed;
    }

    public void setPlayerTwoUltimate(boolean pressed) {
        if (pressed && !playerTwoUltimateHeld) playerTwoUltimateRequested = true;
        playerTwoUltimateHeld = pressed;
    }

    public double getPlayerOneDirection() {
        return (playerOneRight ? 1 : 0) - (playerOneLeft ? 1 : 0);
    }

    public double getPlayerTwoDirection() {
        return (playerTwoRight ? 1 : 0) - (playerTwoLeft ? 1 : 0);
    }

    public boolean consumePlayerOneJump() {
        boolean requested = playerOneJumpRequested;
        playerOneJumpRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoJump() {
        boolean requested = playerTwoJumpRequested;
        playerTwoJumpRequested = false;
        return requested;
    }

    public boolean consumePlayerOneAttackOne() {
        boolean requested = playerOneAttackOneRequested;
        playerOneAttackOneRequested = false;
        return requested;
    }

    public boolean consumePlayerOneAttackTwo() {
        boolean requested = playerOneAttackTwoRequested;
        playerOneAttackTwoRequested = false;
        return requested;
    }

    public boolean consumePlayerOneAttackThree() {
        boolean requested = playerOneAttackThreeRequested;
        playerOneAttackThreeRequested = false;
        return requested;
    }

    public boolean consumePlayerOneAttackFour() {
        boolean requested = playerOneAttackFourRequested;
        playerOneAttackFourRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoAttackOne() {
        boolean requested = playerTwoAttackOneRequested;
        playerTwoAttackOneRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoAttackTwo() {
        boolean requested = playerTwoAttackTwoRequested;
        playerTwoAttackTwoRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoAttackThree() {
        boolean requested = playerTwoAttackThreeRequested;
        playerTwoAttackThreeRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoAttackFour() {
        boolean requested = playerTwoAttackFourRequested;
        playerTwoAttackFourRequested = false;
        return requested;
    }

    /** Returns the most recently pressed attack slot for the player, if any. */
    public int consumePlayerOneAttackRequest() {
        int request = playerOneAttackRequestIndex;
        playerOneAttackRequestIndex = -1;
        playerOneAttackOneRequested = playerOneAttackTwoRequested = false;
        playerOneAttackThreeRequested = playerOneAttackFourRequested = false;
        return request;
    }

    public int consumePlayerTwoAttackRequest() {
        int request = playerTwoAttackRequestIndex;
        playerTwoAttackRequestIndex = -1;
        playerTwoAttackOneRequested = playerTwoAttackTwoRequested = false;
        playerTwoAttackThreeRequested = playerTwoAttackFourRequested = false;
        return request;
    }

    public boolean consumePlayerOneEvolution() {
        boolean requested = playerOneEvolutionRequested;
        playerOneEvolutionRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoEvolution() {
        boolean requested = playerTwoEvolutionRequested;
        playerTwoEvolutionRequested = false;
        return requested;
    }

    public boolean consumePlayerOneUltimate() {
        boolean requested = playerOneUltimateRequested;
        playerOneUltimateRequested = false;
        return requested;
    }

    public boolean consumePlayerTwoUltimate() {
        boolean requested = playerTwoUltimateRequested;
        playerTwoUltimateRequested = false;
        return requested;
    }

    public void clear() {
        playerOneLeft = false;
        playerOneRight = false;
        playerTwoLeft = false;
        playerTwoRight = false;
        playerOneJumpHeld = false;
        playerTwoJumpHeld = false;
        playerOneJumpRequested = false;
        playerTwoJumpRequested = false;
        playerOneAttackOneHeld = false;
        playerOneAttackTwoHeld = false;
        playerOneAttackThreeHeld = false;
        playerOneAttackFourHeld = false;
        playerOneAttackOneRequested = false;
        playerOneAttackTwoRequested = false;
        playerOneAttackThreeRequested = false;
        playerOneAttackFourRequested = false;
        playerTwoAttackOneHeld = false;
        playerTwoAttackTwoHeld = false;
        playerTwoAttackThreeHeld = false;
        playerTwoAttackFourHeld = false;
        playerTwoAttackOneRequested = false;
        playerTwoAttackTwoRequested = false;
        playerTwoAttackThreeRequested = false;
        playerTwoAttackFourRequested = false;
        playerOneEvolutionHeld = false;
        playerTwoEvolutionHeld = false;
        playerOneEvolutionRequested = false;
        playerTwoEvolutionRequested = false;
        playerOneUltimateHeld = false;
        playerTwoUltimateHeld = false;
        playerOneUltimateRequested = false;
        playerTwoUltimateRequested = false;
        playerOneBlockHeld = false;
        playerTwoBlockHeld = false;
        playerOneAttackRequestIndex = -1;
        playerTwoAttackRequestIndex = -1;
    }
}
