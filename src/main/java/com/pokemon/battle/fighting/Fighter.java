package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.Objects;

/** Mutable state for one participant in a free fight. */
public class Fighter {

    private static final int MAX_HEALTH = 100;
    private static final double MAX_EVOLUTION_ENERGY = 100.0;

    private final Pokemon pokemon;
    private final String pokemonId;
    private double x;
    private double y;
    private boolean facingLeft;

    // Reserved for later gameplay stages; no mechanics are applied yet.
    private double velocityX;
    private double velocityY;
    private int health = MAX_HEALTH;
    private double evolutionEnergy;
    private boolean onGround = true;

    public Fighter(String pokemonId, double x, double y, boolean facingLeft) {
        this.pokemon = null;
        this.pokemonId = Objects.requireNonNull(pokemonId);
        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
    }

    public Fighter(Pokemon pokemon, double x, double y, boolean facingLeft) {
        this.pokemon = Objects.requireNonNull(pokemon);
        this.pokemonId = pokemon.getName().toUpperCase();
        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
    }

    public String getPokemonId() { return pokemonId; }
    public String getName() { return pokemon != null ? pokemon.getName() : pokemonId; }
    public Pokemon getPokemon() { return pokemon; }
    public int getRenderHeight() {
        return switch (pokemonId) {
            case "PIKACHU" -> 145;
            case "CHARIZARD" -> 190;
            case "BULBASAUR" -> 130;
            case "CHARMANDER" -> 140;
            case "SQUIRTLE" -> 132;
            case "EEVEE" -> 125;
            default -> 145;
        };
    }
    public double getBodyHalfWidth() {
        return switch (pokemonId) {
            case "PIKACHU" -> 47;
            case "CHARIZARD" -> 73;
            case "BULBASAUR" -> 48;
            case "CHARMANDER" -> 48;
            case "SQUIRTLE" -> 52;
            case "EEVEE" -> 44;
            default -> 50;
        };
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isFacingLeft() { return facingLeft; }

    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return MAX_HEALTH; }
    public double getEvolution() { return evolutionEnergy; }
    public double getEvolutionEnergy() { return evolutionEnergy; }
    public boolean isEvolutionReady() { return evolutionEnergy >= MAX_EVOLUTION_ENERGY; }
    public boolean isOnGround() { return onGround; }

    void moveHorizontally(double direction, double distance) {
        if (direction == 0) return;
        facingLeft = direction < 0;
        x += direction * distance;
    }

    void setX(double x) { this.x = x; }
    void setY(double y) { this.y = y; }
    void setVelocityY(double velocityY) { this.velocityY = velocityY; }
    void setOnGround(boolean onGround) { this.onGround = onGround; }

    void receiveDamage(int damage) {
        health = Math.max(0, health - Math.max(0, damage));
    }

    void addEvolutionEnergy(double amount) {
        evolutionEnergy = Math.max(0,
                Math.min(MAX_EVOLUTION_ENERGY, evolutionEnergy + amount));
    }
}
