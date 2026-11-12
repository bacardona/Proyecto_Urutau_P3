package com.pokemon.battle.fighting;

import java.util.List;

/** Holds the participants and fixed arena data for one free-fight session. */
public class FightingGameSession {

    private static final double PLAYER_SPEED = 320.0;

    private final int width;
    private final int height;
    private final int groundY;
    private final List<Fighter> fighters;
    private final List<Platform> platforms;
    private final PhysicsSystem physicsSystem = new PhysicsSystem();
    private final FightingCombatSystem combatSystem = new FightingCombatSystem();

    public FightingGameSession() {
        this.width = 1280;
        this.height = 720;
        this.groundY = 677;
        this.platforms = List.of(
                new Platform(100, 570, 180, 24),
                new Platform(320, 475, 160, 24),
                new Platform(510, 375, 155, 24),
                new Platform(700, 425, 145, 24),
                new Platform(870, 475, 160, 24),
                new Platform(1060, 570, 170, 24),
                new Platform(0, groundY, width, 24)
        );
        this.fighters = List.of(
                new Fighter("PIKACHU", 230, groundY, false),
                new Fighter("CHARIZARD", 1050, groundY, true)
        );
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getGroundY() { return groundY; }
    public List<Fighter> getFighters() { return fighters; }
    public List<Platform> getPlatforms() { return platforms; }
    public List<FightingProjectile> getProjectiles() { return combatSystem.getProjectiles(); }
    public FightingCombatSystem getCombatSystem() { return combatSystem; }

    public void update(FightingInput input, double deltaSeconds) {
        updateFighter(fighters.get(0), input.getPlayerOneDirection(),
                input.consumePlayerOneJump(), deltaSeconds);
        updateFighter(fighters.get(1), input.getPlayerTwoDirection(),
                input.consumePlayerTwoJump(), deltaSeconds);
        combatSystem.update(fighters, width, input.consumeQuickAttack(),
                input.consumeThunderShock(), input.consumeEmber(), input.consumeTackle(), deltaSeconds);
    }

    private void updateFighter(Fighter fighter, double direction,
                               boolean jumpRequested,
                               double deltaSeconds) {
        double halfWidth = PhysicsSystem.halfWidthFor(fighter);
        fighter.moveHorizontally(direction, PLAYER_SPEED * deltaSeconds);
        double minX = halfWidth;
        double maxX = width - halfWidth;
        fighter.setX(Math.max(minX, Math.min(maxX, fighter.getX())));
        physicsSystem.update(fighter, platforms, halfWidth, jumpRequested, deltaSeconds);
    }
}
