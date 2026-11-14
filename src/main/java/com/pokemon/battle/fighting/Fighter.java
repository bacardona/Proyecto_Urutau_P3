package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.Objects;

/** Mutable state for one participant in a free fight. */
public class Fighter {

    private static final int MAX_HEALTH = 200;
    private static final double MAX_EVOLUTION_ENERGY = 100.0;
    private static final double EVOLUTION_ANIMATION_DURATION = 1.0;

    private final Pokemon pokemon;
    private final String basePokemonId;
    private final EvolutionProfile evolutionProfile;
    private double x;
    private double y;
    private boolean facingLeft;

    // Reserved for later gameplay stages; no mechanics are applied yet.
    private double velocityX;
    private double velocityY;
    private int health = MAX_HEALTH;
    private double evolutionEnergy;
    private boolean onGround = true;
    private boolean defeated;
    private double sleepSeconds;
    private double attackBoostSeconds;
    private double attackReductionSeconds;
    private double defenseReductionSeconds;
    private boolean evolved;
    private double evolutionAnimationSeconds;
    private boolean returningToBase;
    private int ultimateStunCount;
    private boolean blocking;
    private double normalHitStunSeconds;
    private double blockStunSeconds;
    private double comboStunResetSeconds;
    private int comboStunHitCount;

    public Fighter(String pokemonId, double x, double y, boolean facingLeft) {
        this.pokemon = null;
        this.basePokemonId = Objects.requireNonNull(pokemonId).toUpperCase();
        this.evolutionProfile = EvolutionProfile.forBasePokemon(basePokemonId);
        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
    }

    public Fighter(Pokemon pokemon, double x, double y, boolean facingLeft) {
        this.pokemon = Objects.requireNonNull(pokemon);
        this.basePokemonId = pokemon.getName().toUpperCase();
        this.evolutionProfile = EvolutionProfile.forBasePokemon(basePokemonId);
        this.x = x;
        this.y = y;
        this.facingLeft = facingLeft;
    }

    /** Current visual species, used by sprite lookup and team indicators. */
    public String getPokemonId() {
        return evolved && evolutionProfile != null
                ? evolutionProfile.evolvedPokemonId() : basePokemonId;
    }
    /** Original species, kept stable so evolved fighters retain their move profile. */
    public String getBasePokemonId() { return basePokemonId; }
    public String getName() {
        if (evolved && evolutionProfile != null) return evolutionProfile.evolvedName();
        return pokemon != null ? pokemon.getName() : basePokemonId;
    }
    public Pokemon getPokemon() { return pokemon; }
    public int getRenderHeight() {
        if (evolved && evolutionProfile != null) return evolutionProfile.evolvedRenderHeight();
        return switch (basePokemonId) {
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
        return switch (basePokemonId) {
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
    void setFacingLeft(boolean facingLeft) { this.facingLeft = facingLeft; }

    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return MAX_HEALTH; }
    public double getEvolution() { return evolutionEnergy; }
    public double getEvolutionEnergy() { return evolutionEnergy; }
    public boolean isEvolutionReady() { return evolutionEnergy >= MAX_EVOLUTION_ENERGY; }
    public boolean isEvolved() { return evolved; }
    public boolean isEvolutionAnimating() { return evolutionAnimationSeconds > 0; }
    public boolean isReturningToBase() { return returningToBase; }
    public double getEvolutionAnimationProgress() {
        return 1.0 - evolutionAnimationSeconds / EVOLUTION_ANIMATION_DURATION;
    }
    public EvolutionProfile getEvolutionProfile() { return evolutionProfile; }
    public boolean isUltimateReady() { return evolved && isEvolutionReady(); }
    public double getMovementSpeedMultiplier() {
        return evolved ? 1.0 + EvolutionProfile.EVOLVED_MOVEMENT_SPEED_BONUS : 1.0;
    }
    public boolean isOnGround() { return onGround; }
    public boolean isDefeated() { return defeated; }
    public boolean isUltimateStunned() { return ultimateStunCount > 0; }
    public boolean isBlocking() { return blocking; }
    public boolean isHitStunned() { return normalHitStunSeconds > 0 || blockStunSeconds > 0; }
    public boolean isAsleep() { return sleepSeconds > 0; }
    public boolean hasAttackBoost() { return attackBoostSeconds > 0; }
    public boolean hasAttackReduction() { return attackReductionSeconds > 0; }
    public boolean hasDefenseReduction() { return defenseReductionSeconds > 0; }
    public double getOutgoingDamageMultiplier() {
        double multiplier = hasAttackBoost() ? 1.35 : 1.0;
        multiplier *= hasAttackReduction() ? 0.72 : 1.0;
        return multiplier * (evolved ? 1.0 + EvolutionProfile.EVOLVED_DAMAGE_BONUS : 1.0);
    }
    public double getIncomingDamageMultiplier() {
        return hasDefenseReduction() ? 1.30 : 1.0;
    }

    void updateStatusEffects(double deltaSeconds) {
        sleepSeconds = Math.max(0, sleepSeconds - deltaSeconds);
        attackBoostSeconds = Math.max(0, attackBoostSeconds - deltaSeconds);
        attackReductionSeconds = Math.max(0, attackReductionSeconds - deltaSeconds);
        defenseReductionSeconds = Math.max(0, defenseReductionSeconds - deltaSeconds);
        evolutionAnimationSeconds = Math.max(0, evolutionAnimationSeconds - deltaSeconds);
        if (evolutionAnimationSeconds == 0) returningToBase = false;
        normalHitStunSeconds = Math.max(0, normalHitStunSeconds - deltaSeconds);
        blockStunSeconds = Math.max(0, blockStunSeconds - deltaSeconds);
        comboStunResetSeconds = Math.max(0, comboStunResetSeconds - deltaSeconds);
        if (comboStunResetSeconds == 0) comboStunHitCount = 0;
    }

    boolean canEvolve() {
        return !evolved && evolutionProfile != null && isEvolutionReady()
                && !defeated && !isAsleep() && !blocking && !isHitStunned()
                && !isUltimateStunned();
    }

    boolean evolve() {
        if (!canEvolve()) return false;
        evolved = true;
        evolutionEnergy = 0;
        evolutionAnimationSeconds = EVOLUTION_ANIMATION_DURATION;
        returningToBase = false;
        return true;
    }

    boolean consumeUltimateEnergy() {
        if (!isUltimateReady() || defeated || isAsleep() || isUltimateStunned()) return false;
        evolutionEnergy = 0;
        return true;
    }

    void refundInterruptedUltimate() {
        if (evolved && !defeated) evolutionEnergy = 50;
    }

    void returnToBaseAfterUltimate() {
        if (!evolved || evolutionProfile == null) return;
        evolved = false;
        evolutionEnergy = 0;
        evolutionAnimationSeconds = EVOLUTION_ANIMATION_DURATION;
        returningToBase = true;
    }

    void addUltimateStun() {
        if (!defeated) ultimateStunCount++;
    }

    void removeUltimateStun() {
        ultimateStunCount = Math.max(0, ultimateStunCount - 1);
    }

    void setBlocking(boolean requested) {
        blocking = requested && onGround && !defeated && !isAsleep()
                && !isHitStunned() && !isUltimateStunned();
    }

    int recordNormalComboHit(double resetAfterSeconds) {
        comboStunHitCount = comboStunResetSeconds > 0 ? comboStunHitCount + 1 : 1;
        comboStunResetSeconds = resetAfterSeconds;
        return comboStunHitCount;
    }

    void applyNormalHitStun(double seconds) {
        if (!defeated && !isUltimateStunned()) {
            normalHitStunSeconds = Math.max(0, seconds);
            blocking = false;
        }
    }

    void applyBlockStun(double seconds) {
        if (!defeated) {
            blockStunSeconds = Math.max(blockStunSeconds, seconds);
            blocking = false;
        }
    }

    boolean applySleep(double durationSeconds) {
        if (isAsleep() || defeated) return false;
        sleepSeconds = durationSeconds;
        return true;
    }

    boolean applyAttackBoost(double durationSeconds) {
        if (defeated || hasAttackBoost()) return false;
        attackBoostSeconds = durationSeconds;
        return true;
    }

    boolean applyAttackReduction(double durationSeconds) {
        if (defeated || hasAttackReduction()) return false;
        attackReductionSeconds = durationSeconds;
        return true;
    }

    boolean applyDefenseReduction(double durationSeconds) {
        if (defeated || hasDefenseReduction()) return false;
        defenseReductionSeconds = durationSeconds;
        return true;
    }

    void clearStatusEffects() {
        sleepSeconds = 0;
        attackBoostSeconds = 0;
        attackReductionSeconds = 0;
        defenseReductionSeconds = 0;
        evolutionAnimationSeconds = 0;
        returningToBase = false;
        ultimateStunCount = 0;
        blocking = false;
        normalHitStunSeconds = 0;
        blockStunSeconds = 0;
        comboStunResetSeconds = 0;
        comboStunHitCount = 0;
    }

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
        int actualDamage = Math.max(0, damage);
        if (actualDamage > 0) sleepSeconds = 0;
        health = Math.max(0, health - actualDamage);
        if (health == 0) {
            defeated = true;
            clearStatusEffects();
        }
    }

    void markDefeated() {
        health = 0;
        defeated = true;
        clearStatusEffects();
    }

    void addEvolutionEnergy(double amount) {
        evolutionEnergy = Math.max(0,
                Math.min(MAX_EVOLUTION_ENERGY, evolutionEnergy + amount));
    }
}
