package com.pokemon.battle.fighting;

import java.util.List;

/** Applies vertical movement, gravity, jumping, and one-way platform collisions. */
public class PhysicsSystem {

    private static final double GRAVITY = 1400.0;
    private static final double JUMP_VELOCITY = -650.0;
    private static final double PIKACHU_HALF_WIDTH = 47.0;
    private static final double CHARIZARD_HALF_WIDTH = 73.0;

    public void update(Fighter fighter, List<Platform> platforms,
                       double halfWidth, boolean jumpRequested, double deltaSeconds) {
        if (jumpRequested && fighter.isOnGround()) {
            fighter.setVelocityY(JUMP_VELOCITY);
            fighter.setOnGround(false);
        }

        double previousFeetY = fighter.getY();
        double nextVelocityY = fighter.getVelocityY() + GRAVITY * deltaSeconds;
        double nextFeetY = previousFeetY + nextVelocityY * deltaSeconds;
        Platform landing = findLanding(fighter, platforms, halfWidth,
                previousFeetY, nextFeetY, nextVelocityY);

        if (landing != null) {
            fighter.setY(landing.getTop());
            fighter.setVelocityY(0);
            fighter.setOnGround(true);
        } else {
            fighter.setY(nextFeetY);
            fighter.setVelocityY(nextVelocityY);
            fighter.setOnGround(false);
        }
    }

    private Platform findLanding(Fighter fighter, List<Platform> platforms,
                                 double halfWidth, double previousFeetY,
                                 double nextFeetY, double nextVelocityY) {
        if (nextVelocityY < 0) return null;

        Platform landing = null;
        for (Platform platform : platforms) {
            boolean crossedTop = previousFeetY <= platform.getTop()
                    && nextFeetY >= platform.getTop();
            if (crossedTop && platform.overlapsHorizontally(fighter.getX(), halfWidth)
                    && (landing == null || platform.getTop() < landing.getTop())) {
                landing = platform;
            }
        }
        return landing;
    }

    public static double halfWidthFor(Fighter fighter) {
        return "PIKACHU".equals(fighter.getPokemonId())
                ? PIKACHU_HALF_WIDTH : CHARIZARD_HALF_WIDTH;
    }
}
