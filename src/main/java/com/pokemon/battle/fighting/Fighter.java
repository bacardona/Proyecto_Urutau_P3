package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;

import java.util.Objects;

/** Mutable state for one participant in a free fight. */
public class Fighter {

    enum MotionAnimation { IDLE, WALKING, JUMPING, BLOCKING, BLOCK_RELEASE }

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
    /** True mientras sube por un salto del jugador (se puede cortar al soltar la tecla). */
    private boolean playerJumpRising;
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
    private MotionAnimation motionAnimation = MotionAnimation.IDLE;
    private double motionAnimationSeconds;
    private double defeatedAnimationSeconds;
    private double damageAnimationSeconds;
    private double damageVisualRemaining;
    private double shieldOpacity;
    private double shieldAnimationSeconds;
    private boolean shieldWasBlocking;
    // Parry: cubrirse justo antes del golpe
    static final double PARRY_WINDOW_SECONDS = 0.15;
    static final double PARRY_REARM_SECONDS = 0.45;
    private double blockHeldSeconds;
    // Jefe secreto (Mewtwo): aguanta el doble y en su segunda fase se enfurece
    static final double BOSS_DAMAGE_TAKEN = 0.6;
    static final double ENRAGED_DAMAGE_BONUS = 0.20;
    static final double ENRAGED_SPEED_BONUS = 0.15;
    private boolean boss;
    private boolean enraged;
    // Modo dificil: Mega Mewtwo Y (fase 3) y barrera que refleja proyectiles
    static final double MEGA_DAMAGE_BONUS = 0.15;
    static final double MEGA_SPEED_BONUS = 0.25;
    static final String MEGA_FORM_ID = "MEGA_MEWTWO_Y";
    private boolean megaPending;
    private boolean mega;
    private double barrierSeconds;
    // Mewtwo apunta hacia arriba cuando el rival esta en una plataforma (radianes, 0 = recto)
    private double aimUpAngle;
    // En la revancha dificil aguanta mas
    static final double HARD_BOSS_DAMAGE_TAKEN = 0.55;
    private boolean hardBoss;
    private double parryRearmSeconds;
    private boolean parryArmed;

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
        if (mega) return MEGA_FORM_ID;
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
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(this);
        if (profile != null) return profile.bodyHeight;
        if (evolved && evolutionProfile != null) return evolutionProfile.evolvedRenderHeight();
        return switch (basePokemonId) {
            case "PIKACHU" -> 145;
            case "CHARIZARD" -> 190;
            case "BULBASAUR" -> 130;
            case "CHARMANDER" -> 140;
            case "SQUIRTLE" -> evolved ? evolutionProfile.evolvedRenderHeight() : 75;
            case "EEVEE" -> 125;
            default -> 145;
        };
    }
    public double getBodyHalfWidth() {
        FightingSpriteProfile profile = FightingSpriteProfile.forFighter(this);
        if (profile != null) return profile.bodyHalfWidth;
        return switch (basePokemonId) {
            case "PIKACHU" -> 47;
            case "CHARIZARD" -> 73;
            case "BULBASAUR" -> 48;
            case "CHARMANDER" -> 48;
            case "SQUIRTLE" -> evolved ? 52 : 30;
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
    public boolean isUltimateReady() { return (evolved || boss) && isEvolutionReady(); }
    public double getMovementSpeedMultiplier() {
        if (mega) return 1.0 + MEGA_SPEED_BONUS;
        if (enraged) return 1.0 + ENRAGED_SPEED_BONUS;
        return evolved ? 1.0 + EvolutionProfile.EVOLVED_MOVEMENT_SPEED_BONUS : 1.0;
    }
    public boolean isBoss() { return boss; }
    public boolean isEnraged() { return enraged; }
    void markAsBoss() { boss = true; }
    void markAsHardBoss() { boss = true; hardBoss = true; }
    void enrage() { if (boss) enraged = true; }

    /** Fase 3: se esta transformando (cansado, escena). Durante la escena no hace nada. */
    public boolean isMegaPending() { return megaPending; }
    public boolean isMega() { return mega; }
    void startMega() { if (boss && !mega) megaPending = true; }
    /** La explosion de energia: desde aqui ya es Mega Mewtwo Y. */
    void completeMega() {
        if (!megaPending) return;
        megaPending = false;
        mega = true;
        enraged = true;
    }

    public boolean hasBarrier() { return barrierSeconds > 0; }
    // ===== Esquiva rapida (escudo + direccion) =====
    /** Dura 0,20 s y recorre unos 140 px; atraviesa al rival. */
    static final double DODGE_SECONDS = 0.20;
    /** Solo al inicio cuenta como esquiva: si el golpe llega ahi, falla. Despues pega normal. */
    static final double DODGE_PERFECT_SECONDS = 0.15;
    static final double DODGE_SPEED = 720;
    static final double DODGE_COOLDOWN_SECONDS = 0.80;
    private double dodgeLeft;
    private double dodgeElapsed;
    private double dodgeCooldownLeft;
    private int dodgeDirection;

    public boolean isDodging() { return dodgeLeft > 0; }
    /** Esquiva a tiempo: el golpe que llegue ahora no le hace nada. No hay invulnerabilidad despues. */
    boolean isPerfectDodge() { return dodgeLeft > 0 && dodgeElapsed < DODGE_PERFECT_SECONDS; }
    int getDodgeDirection() { return dodgeDirection; }
    boolean canDodge() {
        return dodgeLeft <= 0 && dodgeCooldownLeft <= 0 && onGround && !defeated
                && !isAsleep() && !isHitStunned();
    }
    void startDodge(int direction) {
        dodgeDirection = direction;
        dodgeLeft = DODGE_SECONDS;
        dodgeElapsed = 0;
        dodgeCooldownLeft = DODGE_COOLDOWN_SECONDS;
    }
    void advanceDodge(double deltaSeconds) {
        dodgeCooldownLeft = Math.max(0, dodgeCooldownLeft - deltaSeconds);
        if (dodgeLeft > 0) {
            dodgeLeft -= deltaSeconds;
            dodgeElapsed += deltaSeconds;
        }
    }

    /** Cae en picada (golpe cuerpo a cuerpo empezado en el aire). */
    private boolean diving;
    public boolean isDiving() { return diving; }
    void setDiving(boolean diving) { this.diving = diving; }

    public double getAimUpAngle() { return aimUpAngle; }
    void setAimUpAngle(double radians) { aimUpAngle = radians; }

    /** Cura sin pasar del maximo (Mega Mewtwo recupera vida al transformarse). */
    void heal(int amount) {
        if (defeated || amount <= 0) return;
        health = Math.min(MAX_HEALTH, health + amount);
    }
    void raiseBarrier(double seconds) { if (boss) barrierSeconds = Math.max(barrierSeconds, seconds); }


    public boolean isOnGround() { return onGround; }
    public boolean isDefeated() { return defeated; }
    public boolean isUltimateStunned() { return ultimateStunCount > 0; }
    public boolean isBlocking() { return blocking; }
    MotionAnimation getMotionAnimation() { return motionAnimation; }
    double getMotionAnimationSeconds() { return motionAnimationSeconds; }
    double getDefeatedAnimationSeconds() { return defeatedAnimationSeconds; }
    double getDamageAnimationSeconds() { return damageAnimationSeconds; }
    boolean isShowingDamage() { return damageVisualRemaining > 0; }
    double getShieldOpacity() { return shieldOpacity; }
    double getShieldAnimationSeconds() { return shieldAnimationSeconds; }

    /** Visual only; never participates in canBlock, damage, or collision geometry. */
    void updateShieldVisual(boolean attacking, double deltaSeconds) {
        if (defeated || isAsleep() || isHitStunned() || isUltimateStunned()
                || isEvolutionAnimating() || attacking) {
            shieldOpacity = 0;
            shieldAnimationSeconds = 0;
            shieldWasBlocking = false;
        } else if (blocking) {
            if (!shieldWasBlocking) shieldAnimationSeconds = 0;
            shieldWasBlocking = true;
            shieldAnimationSeconds += deltaSeconds;
            shieldOpacity = Math.min(1, shieldOpacity + deltaSeconds / 0.10);
        } else {
            shieldWasBlocking = false;
            shieldOpacity = Math.max(0, shieldOpacity - deltaSeconds / 0.14);
        }
    }
    public boolean isHitStunned() { return normalHitStunSeconds > 0 || blockStunSeconds > 0; }
    public boolean isAsleep() { return sleepSeconds > 0; }
    public boolean hasAttackBoost() { return attackBoostSeconds > 0; }
    public boolean hasAttackReduction() { return attackReductionSeconds > 0; }
    public boolean hasDefenseReduction() { return defenseReductionSeconds > 0; }
    public double getOutgoingDamageMultiplier() {
        double multiplier = hasAttackBoost() ? 1.35 : 1.0;
        multiplier *= hasAttackReduction() ? 0.72 : 1.0;
        multiplier *= mega ? 1.0 + MEGA_DAMAGE_BONUS : enraged ? 1.0 + ENRAGED_DAMAGE_BONUS : 1.0;
        return multiplier * (evolved ? 1.0 + EvolutionProfile.EVOLVED_DAMAGE_BONUS : 1.0);
    }
    public double getIncomingDamageMultiplier() {
        double multiplier = hasDefenseReduction() ? 1.30 : 1.0;
        if (!boss) return multiplier;
        return multiplier * (hardBoss ? HARD_BOSS_DAMAGE_TAKEN : BOSS_DAMAGE_TAKEN);
    }

    void updateStatusEffects(double deltaSeconds) {
        barrierSeconds = Math.max(0, barrierSeconds - deltaSeconds);
        damageAnimationSeconds += deltaSeconds;
        damageVisualRemaining = Math.max(0, damageVisualRemaining - deltaSeconds);
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
        if (blocking) blockHeldSeconds += deltaSeconds;
        else parryRearmSeconds = Math.max(0, parryRearmSeconds - deltaSeconds);
    }

    /** True during the first instants of a block (only if the player did not spam the key). */
    boolean isInParryWindow() {
        return blocking && parryArmed && blockHeldSeconds <= PARRY_WINDOW_SECONDS;
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
        if ((evolved || boss) && !defeated) evolutionEnergy = 50;
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
        boolean was = blocking;
        blocking = requested && onGround && !defeated && !isAsleep()
                && !isHitStunned() && !isUltimateStunned();
        if (blocking && !was) {
            // Apretar y soltar muy seguido no da parry: hay que esperar un poquito
            blockHeldSeconds = 0;
            parryArmed = parryRearmSeconds <= 0;
        } else if (!blocking && was) {
            parryRearmSeconds = PARRY_REARM_SECONDS;
        }
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
        // El jefe se duerme la mitad de tiempo
        sleepSeconds = boss ? durationSeconds * 0.5 : durationSeconds;
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
        shieldOpacity = 0;
        shieldAnimationSeconds = 0;
        shieldWasBlocking = false;
        damageAnimationSeconds = 0;
        damageVisualRemaining = 0;
        sleepSeconds = 0;
        attackBoostSeconds = 0;
        attackReductionSeconds = 0;
        defenseReductionSeconds = 0;
        evolutionAnimationSeconds = 0;
        returningToBase = false;
        ultimateStunCount = 0;
        blocking = false;
        blockHeldSeconds = 0;
        parryRearmSeconds = 0;
        parryArmed = false;
        normalHitStunSeconds = 0;
        blockStunSeconds = 0;
        comboStunResetSeconds = 0;
        comboStunHitCount = 0;
        resetMotionAnimation();
    }

    void advanceDefeatedAnimation(double deltaSeconds) {
        if (defeated) defeatedAnimationSeconds += Math.max(0, deltaSeconds);
    }

    /** Render-only locomotion state, advanced by the combat simulation clock. */
    void updateMotionAnimation(MotionAnimation next, double deltaSeconds) {
        if (next == MotionAnimation.BLOCKING) {
            if (motionAnimation != MotionAnimation.BLOCKING) {
                motionAnimation = MotionAnimation.BLOCKING;
                motionAnimationSeconds = 0;
            } else {
                motionAnimationSeconds += deltaSeconds;
            }
            return;
        }

        if (motionAnimation == MotionAnimation.BLOCKING && next == MotionAnimation.IDLE) {
            motionAnimation = MotionAnimation.BLOCK_RELEASE;
            motionAnimationSeconds = 0;
            return;
        }
        if (motionAnimation == MotionAnimation.BLOCK_RELEASE && next == MotionAnimation.IDLE) {
            motionAnimationSeconds += deltaSeconds;
            if (motionAnimationSeconds < 0.20) return;
        }

        if (motionAnimation != next) {
            motionAnimation = next;
            motionAnimationSeconds = 0;
        } else {
            motionAnimationSeconds += deltaSeconds;
        }
    }

    private void resetMotionAnimation() {
        motionAnimation = MotionAnimation.IDLE;
        motionAnimationSeconds = 0;
    }

    void moveHorizontally(double direction, double distance) {
        if (direction == 0) return;
        facingLeft = direction < 0;
        x += direction * distance;
    }

    void setX(double x) { this.x = x; }

    // Retroceso al recibir un golpe: empuja hacia atras y se frena solo
    private double knockbackVelocity;

    /** Pushes the fighter back; a stronger push replaces a weaker one. */
    void applyKnockback(double direction, double speed) {
        if (defeated || speed <= 0 || direction == 0) return;
        double push = Math.signum(direction) * speed;
        if (Math.abs(push) >= Math.abs(knockbackVelocity)) knockbackVelocity = push;
    }

    void advanceKnockback(double deltaSeconds, double arenaWidth, double halfWidth) {
        if (knockbackVelocity == 0) return;
        x = Math.max(halfWidth, Math.min(arenaWidth - halfWidth, x + knockbackVelocity * deltaSeconds));
        knockbackVelocity *= Math.exp(-7 * deltaSeconds);
        if (Math.abs(knockbackVelocity) < 12) knockbackVelocity = 0;
    }

    public double getKnockbackVelocity() { return knockbackVelocity; }
    void setY(double y) { this.y = y; }
    void setVelocityY(double velocityY) { this.velocityY = velocityY; }
    void setOnGround(boolean onGround) { this.onGround = onGround; }
    boolean isPlayerJumpRising() { return playerJumpRising; }
    void setPlayerJumpRising(boolean rising) { this.playerJumpRising = rising; }

    void receiveDamage(int damage) {
        int actualDamage = Math.max(0, damage);
        if (actualDamage > 0) {
            sleepSeconds = 0;
            damageAnimationSeconds = 0;
            damageVisualRemaining = 0.20;
        }
        health = Math.max(0, health - actualDamage);
        if (health == 0) {
            defeated = true;
            clearStatusEffects();
            defeatedAnimationSeconds = 0;
        }
    }

    void markDefeated() {
        health = 0;
        defeated = true;
        clearStatusEffects();
        defeatedAnimationSeconds = 0;
    }

    void addEvolutionEnergy(double amount) {
        evolutionEnergy = Math.max(0,
                Math.min(MAX_EVOLUTION_ENERGY, evolutionEnergy + amount));
    }
}
