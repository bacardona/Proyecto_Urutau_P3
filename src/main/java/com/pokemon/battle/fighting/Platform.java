package com.pokemon.battle.fighting;

/** Rectangular platform whose top surface can support fighters. */
public final class Platform {

    private final int x;
    private final int top;
    private final int width;
    private final int height;

    public Platform(int x, int top, int width, int height) {
        this.x = x;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    public int getX() { return x; }
    public int getTop() { return top; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean overlapsHorizontally(double centerX, double halfWidth) {
        return centerX + halfWidth > x && centerX - halfWidth < x + width;
    }

    /** One-way top-surface collision for a horizontally travelling projectile. */
    public boolean blocksProjectile(Hitbox projectile) {
        return projectile.y() < top && projectile.y() + projectile.height() > top
                && projectile.x() < x + width && projectile.x() + projectile.width() > x;
    }
}
