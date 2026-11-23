package com.pokemon.battle.fighting;

import java.util.List;

/** Applies vertical movement, gravity, jumping, and one-way platform collisions. */
public class PhysicsSystem {

    static final double GRAVITY = 1400.0;
    private static final double JUMP_VELOCITY = -650.0;
    /**
     * Salto del bosque: un poco mas bajo (unos 125 px en vez de 150) para no quedar con el cuerpo
     * metido en la plataforma de la derecha al saltar desde el piso. Igual alcanza para subir
     * a las plataformas bajas y de una a otra.
     */
    static final double FOREST_JUMP_VELOCITY = -592.0;
    private double jumpVelocity = JUMP_VELOCITY;
    void setJumpVelocity(double velocity) { jumpVelocity = velocity; }
    /** Al soltar la tecla mientras sube, la velocidad baja a esto: toque corto = salto pequeno. */
    private static final double JUMP_CUT_VELOCITY = -330.0;

    public void update(Fighter fighter, List<Platform> platforms,
                       double halfWidth, boolean jumpRequested, double deltaSeconds) {
        // sin informacion de la tecla: salto completo, como antes
        update(fighter, platforms, halfWidth, jumpRequested, true, deltaSeconds);
    }

    /**
     * Salto variable: mantener la tecla da el salto completo (el mismo de antes); un toque corto
     * da uno pequeno. Si la tecla sigue presionada al caer al suelo, vuelve a saltar solo.
     */
    public void update(Fighter fighter, List<Platform> platforms, double halfWidth,
                       boolean startJump, boolean jumpHeld, double deltaSeconds) {
        if (startJump && fighter.isOnGround()) {
            fighter.setVelocityY(jumpVelocity);
            fighter.setOnGround(false);
            fighter.setPlayerJumpRising(true);
        }
        if (fighter.isPlayerJumpRising()) {
            if (fighter.getVelocityY() >= 0) {
                fighter.setPlayerJumpRising(false);
            } else if (!jumpHeld) {
                fighter.setVelocityY(Math.max(fighter.getVelocityY(), JUMP_CUT_VELOCITY));
                fighter.setPlayerJumpRising(false);
            }
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
            fighter.setPlayerJumpRising(false);
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
        return fighter.getBodyHalfWidth();
    }

    record Position(double x, double y) { }

    static Position positionOf(Fighter fighter) { return new Position(fighter.getX(), fighter.getY()); }

    /** Swept body boxes. Only closing motion is removed; a stationary rival is not pushed. */
    static void resolveBodies(Fighter first, Fighter second, Position firstBefore,
                              Position secondBefore, int arenaWidth) {
        if (first.isDefeated() || second.isDefeated()) return;
        if (first.isDodging() || second.isDodging()) return;   // la esquiva pasa a traves del rival
        first.setX(clampX(first.getX(), first, arenaWidth));
        second.setX(clampX(second.getX(), second, arenaWidth));
        double halfWidths = halfWidthFor(first) + halfWidthFor(second);
        double relativeX = secondBefore.x() - firstBefore.x();
        double relativeY = secondBefore.y() - firstBefore.y();
        double dx = (second.getX() - secondBefore.x()) - (first.getX() - firstBefore.x());
        double dy = (second.getY() - secondBefore.y()) - (first.getY() - firstBefore.y());
        double[] horizontal = overlapInterval(relativeX, dx, -halfWidths, halfWidths);
        double[] vertical = overlapInterval(relativeY, dy, -first.getRenderHeight(), second.getRenderHeight());
        if (horizontal == null || vertical == null) return;
        double entry = Math.max(0, Math.max(horizontal[0], vertical[0]));
        double exit = Math.min(1, Math.min(horizontal[1], vertical[1]));
        if (entry >= exit - 1e-9) return;

        double sideAtContact = relativeX + dx * entry;
        boolean firstOnLeft = Math.abs(sideAtContact) > 1e-7 ? sideAtContact > 0 : relativeX >= 0;
        Fighter left = firstOnLeft ? first : second;
        Fighter right = firstOnLeft ? second : first;
        Position leftBefore = firstOnLeft ? firstBefore : secondBefore;
        Position rightBefore = firstOnLeft ? secondBefore : firstBefore;
        double penetration = halfWidths - (right.getX() - left.getX());
        if (penetration <= 1e-7) return;
        double leftClosing = Math.max(0, left.getX() - leftBefore.x());
        double rightClosing = Math.max(0, rightBefore.x() - right.getX());
        double totalClosing = leftClosing + rightClosing;
        double leftShare;
        if (totalClosing > 1e-7) {
            leftShare = leftClosing / totalClosing;
        } else if (left.isOnGround() != right.isOnGround()) {
            // A descending fighter clears the grounded body; it cannot stand on a rival.
            leftShare = left.isOnGround() ? 0 : 1;
        } else {
            // Spawn/size-change overlap: minimum, symmetric separation.
            leftShare = 0.5;
        }
        left.setX(clampX(left.getX() - penetration * leftShare, left, arenaWidth));
        right.setX(clampX(right.getX() + penetration * (1 - leftShare), right, arenaWidth));
        double remaining = halfWidths - (right.getX() - left.getX());
        if (remaining > 1e-7) {
            double leftRoom = left.getX() - halfWidthFor(left);
            double shift = Math.min(remaining, leftRoom);
            left.setX(left.getX() - shift);
            right.setX(clampX(right.getX() + remaining - shift, right, arenaWidth));
        }
    }

    private static double clampX(double x, Fighter fighter, int arenaWidth) {
        double half = halfWidthFor(fighter);
        return Math.max(half, Math.min(arenaWidth - half, x));
    }

    /** Times during which one relative coordinate lies strictly inside the overlap slab. */
    private static double[] overlapInterval(double value, double delta, double min, double max) {
        if (Math.abs(delta) < 1e-9) {
            return value > min + 1e-7 && value < max - 1e-7
                    ? new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY} : null;
        }
        double a = (min - value) / delta;
        double b = (max - value) / delta;
        return new double[]{Math.min(a, b), Math.max(a, b)};
    }

    static double launchVelocityForDisplacement(double startY, double targetY,
                                                double flightSeconds) {
        return (targetY - startY - 0.5 * GRAVITY * flightSeconds * flightSeconds)
                / flightSeconds;
    }
}
