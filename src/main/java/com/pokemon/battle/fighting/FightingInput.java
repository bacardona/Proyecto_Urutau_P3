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

    public void setPlayerOneLeft(boolean pressed) { playerOneLeft = pressed; }
    public void setPlayerOneRight(boolean pressed) { playerOneRight = pressed; }
    public void setPlayerTwoLeft(boolean pressed) { playerTwoLeft = pressed; }
    public void setPlayerTwoRight(boolean pressed) { playerTwoRight = pressed; }

    public void setPlayerOneJump(boolean pressed) {
        if (pressed && !playerOneJumpHeld) playerOneJumpRequested = true;
        playerOneJumpHeld = pressed;
    }

    public void setPlayerTwoJump(boolean pressed) {
        if (pressed && !playerTwoJumpHeld) playerTwoJumpRequested = true;
        playerTwoJumpHeld = pressed;
    }

    public void setPlayerOneAttackOne(boolean pressed) {
        if (pressed && !playerOneAttackOneHeld) playerOneAttackOneRequested = true;
        playerOneAttackOneHeld = pressed;
    }

    public void setPlayerOneAttackTwo(boolean pressed) {
        if (pressed && !playerOneAttackTwoHeld) playerOneAttackTwoRequested = true;
        playerOneAttackTwoHeld = pressed;
    }

    public void setPlayerOneAttackThree(boolean pressed) {
        if (pressed && !playerOneAttackThreeHeld) playerOneAttackThreeRequested = true;
        playerOneAttackThreeHeld = pressed;
    }

    public void setPlayerOneAttackFour(boolean pressed) {
        if (pressed && !playerOneAttackFourHeld) playerOneAttackFourRequested = true;
        playerOneAttackFourHeld = pressed;
    }

    public void setPlayerTwoAttackOne(boolean pressed) {
        if (pressed && !playerTwoAttackOneHeld) playerTwoAttackOneRequested = true;
        playerTwoAttackOneHeld = pressed;
    }

    public void setPlayerTwoAttackTwo(boolean pressed) {
        if (pressed && !playerTwoAttackTwoHeld) playerTwoAttackTwoRequested = true;
        playerTwoAttackTwoHeld = pressed;
    }

    public void setPlayerTwoAttackThree(boolean pressed) {
        if (pressed && !playerTwoAttackThreeHeld) playerTwoAttackThreeRequested = true;
        playerTwoAttackThreeHeld = pressed;
    }

    public void setPlayerTwoAttackFour(boolean pressed) {
        if (pressed && !playerTwoAttackFourHeld) playerTwoAttackFourRequested = true;
        playerTwoAttackFourHeld = pressed;
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
    }
}
