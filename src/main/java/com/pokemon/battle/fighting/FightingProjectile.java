package com.pokemon.battle.fighting;

import java.awt.Color;

/** Reusable moving effect with a damaging collision area. */
public final class FightingProjectile {

    private double x;
    private final double y;
    private final double velocityX;
    private final double size;
    private final int damage;
    private double remainingSeconds;
    private final Color color;
    private final int ownerIndex;

    FightingProjectile(double x, double y, double velocityX, double size,
                       int damage, double lifetime, Color color, int ownerIndex) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.size = size;
        this.damage = damage;
        this.remainingSeconds = lifetime;
        this.color = color;
        this.ownerIndex = ownerIndex;
    }

    void update(double deltaSeconds) {
        x += velocityX * deltaSeconds;
        remainingSeconds -= deltaSeconds;
    }

    boolean isExpired(int arenaWidth) {
        return remainingSeconds <= 0 || x < -size || x > arenaWidth + size;
    }

    public Hitbox hitbox() {
        return new Hitbox(x - size / 2, y - size / 2, size, size);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getSize() { return size; }
    public int getDamage() { return damage; }
    public Color getColor() { return color; }
    public int getOwnerIndex() { return ownerIndex; }
}
