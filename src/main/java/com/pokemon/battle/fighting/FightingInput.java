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
    private boolean quickAttackHeld;
    private boolean thunderShockHeld;
    private boolean quickAttackRequested;
    private boolean thunderShockRequested;
    private boolean emberHeld;
    private boolean tackleHeld;
    private boolean emberRequested;
    private boolean tackleRequested;

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

    public void setQuickAttack(boolean pressed) {
        if (pressed && !quickAttackHeld) quickAttackRequested = true;
        quickAttackHeld = pressed;
    }

    public void setThunderShock(boolean pressed) {
        if (pressed && !thunderShockHeld) thunderShockRequested = true;
        thunderShockHeld = pressed;
    }

    public void setEmber(boolean pressed) {
        if (pressed && !emberHeld) emberRequested = true;
        emberHeld = pressed;
    }

    public void setTackle(boolean pressed) {
        if (pressed && !tackleHeld) tackleRequested = true;
        tackleHeld = pressed;
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

    public boolean consumeQuickAttack() {
        boolean requested = quickAttackRequested;
        quickAttackRequested = false;
        return requested;
    }

    public boolean consumeThunderShock() {
        boolean requested = thunderShockRequested;
        thunderShockRequested = false;
        return requested;
    }

    public boolean consumeEmber() {
        boolean requested = emberRequested;
        emberRequested = false;
        return requested;
    }

    public boolean consumeTackle() {
        boolean requested = tackleRequested;
        tackleRequested = false;
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
        quickAttackHeld = false;
        thunderShockHeld = false;
        quickAttackRequested = false;
        thunderShockRequested = false;
        emberHeld = false;
        tackleHeld = false;
        emberRequested = false;
        tackleRequested = false;
    }
}
