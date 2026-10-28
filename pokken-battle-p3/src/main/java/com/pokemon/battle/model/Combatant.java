package com.pokemon.battle.model;

/**
 * A Pokemon taking part in a battle, with its mutable battle state.
 * Most mutators are package-private: only the model rules (moves and status
 * conditions) change the state, while other layers just read it.
 */
public class Combatant {

    private final Pokemon pokemon;
    private final HitPoints hitPoints;
    private final StatStages stages = new StatStages();

    // Stats que pueden cambiar durante la batalla (por paralisis o quemadura)
    private int attack;
    private int speed;

    private StatusCondition status = StatusCondition.NONE;
    private int statusTurns = 0;
    private Move chargingMove = null;
    private boolean invulnerable = false;
    private boolean flinched = false;

    /**
     * @param pokemon species that enters the battle
     */
    public Combatant(Pokemon pokemon) {
        this.pokemon = pokemon;
        this.hitPoints = new HitPoints(pokemon.getMaxHitPoints());
        this.attack = pokemon.getAttack();
        this.speed = pokemon.getSpeed();
    }

    /**
     * @return the species of this combatant
     */
    public Pokemon getPokemon() {
        return pokemon;
    }

    /**
     * @return the Pokemon name
     */
    public String getName() {
        return pokemon.getName();
    }

    /**
     * @return the hit points of this combatant
     */
    public HitPoints getHitPoints() {
        return hitPoints;
    }

    /**
     * @return true when this combatant has no hit points left
     */
    public boolean isFainted() {
        return hitPoints.isFainted();
    }

    /**
     * @return the stat stages of this combatant
     */
    public StatStages getStages() {
        return stages;
    }

    /**
     * @return attack with the burn penalty and the attack stage applied
     */
    public double getEffectiveAttack() {
        return attack * StatStages.multiplier(stages.get(StatType.ATTACK));
    }

    /**
     * @return defense with the defense stage applied
     */
    public double getEffectiveDefense() {
        return pokemon.getDefense() * StatStages.multiplier(stages.get(StatType.DEFENSE));
    }

    /**
     * @return special attack with the special attack stage applied
     */
    public double getEffectiveSpecialAttack() {
        return pokemon.getSpecialAttack() * StatStages.multiplier(stages.get(StatType.SPECIAL_ATTACK));
    }

    /**
     * @return special defense with the special defense stage applied
     */
    public double getEffectiveSpecialDefense() {
        return pokemon.getSpecialDefense() * StatStages.multiplier(stages.get(StatType.SPECIAL_DEFENSE));
    }

    /**
     * @return multiplier that the accuracy stage applies to the accuracy of its moves
     */
    public float getAccuracyMultiplier() {
        return StatStages.multiplier(stages.get(StatType.ACCURACY));
    }

    /**
     * @return the current speed (may be reduced by paralysis)
     */
    public int getSpeed() {
        return speed;
    }

    void halveAttack() {
        this.attack = (int) (attack * 0.5f);
    }

    void restoreAttack() {
        this.attack = pokemon.getAttack();
    }

    void halveSpeed() {
        this.speed = (int) (speed * 0.5f);
    }

    void restoreSpeed() {
        this.speed = pokemon.getSpeed();
    }

    /**
     * @return the status condition, {@link StatusCondition#NONE} when healthy
     */
    public StatusCondition getStatus() {
        return status;
    }

    void setStatus(StatusCondition status) {
        this.status = status;
    }

    /**
     * @return turns counter used by statuses such as sleep
     */
    public int getStatusTurns() {
        return statusTurns;
    }

    void setStatusTurns(int statusTurns) {
        this.statusTurns = statusTurns;
    }

    /**
     * @return the two-turn move being charged, or null
     */
    public Move getChargingMove() {
        return chargingMove;
    }

    /**
     * @return true when the combatant is charging a two-turn move
     */
    public boolean isCharging() {
        return chargingMove != null;
    }

    void startCharging(Move move, boolean invulnerable) {
        this.chargingMove = move;
        this.invulnerable = invulnerable;
    }

    /**
     * Cancels any charge in progress and removes invulnerability.
     */
    public void cancelCharge() {
        this.chargingMove = null;
        this.invulnerable = false;
    }

    /**
     * @return true when the combatant cannot be hit (flying or underground)
     */
    public boolean isInvulnerable() {
        return invulnerable;
    }

    void flinch() {
        this.flinched = true;
    }

    /**
     * Checks whether the combatant lost its action to a flinch and clears the flinch.
     *
     * @return true when the combatant was flinched
     */
    public boolean consumeFlinch() {
        boolean wasFlinched = flinched;
        flinched = false;
        return wasFlinched;
    }
}
