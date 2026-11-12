package com.pokemon.battle.fighting;

import java.util.Objects;

/** Mutable state for one participant in a free fight. */
public class Fighter {

    private final String pokemonId;
    private double x;
    private double y;
    private boolean facingLeft;

    // Reserved for later gameplay stages; no mechanics are applied yet.
    private double velocityX;
    private double velocityY;
    private int health;
    private double evolution;

    public Fighter(String pokemonId, double x, double y, boolean facingLeft) {
        this.pokemonId = Objects.requireNonNull(pokemonId);
        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
    }

    public String getPokemonId() { return pokemonId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isFacingLeft() { return facingLeft; }

    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public int getHealth() { return health; }
    public double getEvolution() { return evolution; }
}
