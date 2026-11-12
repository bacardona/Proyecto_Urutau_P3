package com.pokemon.battle.fighting;

/** Axis-aligned attack area in world coordinates. */
public record Hitbox(double x, double y, double width, double height) {

    public boolean intersects(Fighter fighter) {
        double targetHalfWidth = PhysicsSystem.halfWidthFor(fighter);
        double targetHeight = "PIKACHU".equals(fighter.getPokemonId()) ? 145 : 190;
        double targetLeft = fighter.getX() - targetHalfWidth;
        double targetTop = fighter.getY() - targetHeight;
        return x < targetLeft + targetHalfWidth * 2
                && x + width > targetLeft
                && y < fighter.getY()
                && y + height > targetTop;
    }
}
