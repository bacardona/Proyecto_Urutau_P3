package com.pokemon.battle.fighting;

import java.util.List;

/** Holds the participants and fixed arena data for one free-fight session. */
public class FightingGameSession {

    private final int width;
    private final int height;
    private final int groundY;
    private final List<Fighter> fighters;

    public FightingGameSession() {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.fighters = List.of(
                new Fighter("PIKACHU", 230, groundY, false),
                new Fighter("CHARIZARD", 1050, groundY, true)
        );
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }
    public List<Fighter> getFighters() { return fighters; }
}
