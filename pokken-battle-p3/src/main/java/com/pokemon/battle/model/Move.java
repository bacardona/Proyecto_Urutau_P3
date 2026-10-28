package com.pokemon.battle.model;

/**
 * Base class of every move. Each subclass defines what the move does in
 * {@link #execute}, so the rest of the system never needs to know the concrete type.
 */
public abstract class Move {

    private final String id;
    private final String name;
    private final Type type;
    private final MoveCategory category;
    private final int power;
    private final int accuracy;
    private final int pp;
    private final int priority;

    /**
     * @param id       unique key of the move (used in the data files)
     * @param name     display name
     * @param type     elemental type
     * @param category physical, special or status
     * @param power    base power (0 for moves that do not hurt)
     * @param accuracy base accuracy from 1 to 100
     * @param pp       power points
     * @param priority moves with higher priority act first
     */
    protected Move(String id, String name, Type type, MoveCategory category, int power,
                   int accuracy, int pp, int priority) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.pp = pp;
        this.priority = priority;
    }

    /**
     * Executes the move.
     *
     * @param attacker combatant that uses the move
     * @param defender combatant that is the opponent
     * @param context  battle collaborators (random, type chart, log)
     */
    public abstract void execute(Combatant attacker, Combatant defender, BattleContext context);

    /**
     * Builds a short description of the move to show it to the player. Each
     * subclass implements it in its own way (polymorphism).
     *
     * @return a textual description of the move
     */
    public abstract String getDescription();

    /**
     * Common beginning of every description: name and type.
     *
     * @return the name of the move followed by its type between brackets
     */
    protected String describeHeader() {
        return name + " [" + type + "]";
    }

    /**
     * @return the unique key of the move
     */
    public String getId() {
        return id;
    }

    /**
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the elemental type
     */
    public Type getType() {
        return type;
    }

    /**
     * @return the category of the move
     */
    public MoveCategory getCategory() {
        return category;
    }

    /**
     * @return the base power
     */
    public int getPower() {
        return power;
    }

    /**
     * @return the base accuracy
     */
    public int getAccuracy() {
        return accuracy;
    }

    /**
     * @return the power points
     */
    public int getPp() {
        return pp;
    }

    /**
     * @return the priority; higher values act first
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Checks whether the opponent cannot be reached (flying or underground).
     * When that happens the failure message is written in the log.
     *
     * @param attacker combatant using the move
     * @param defender opponent
     * @param context  battle collaborators
     * @return true when the move misses because of invulnerability
     */
    protected boolean isOutOfReach(Combatant attacker, Combatant defender, BattleContext context) {
        if (!defender.isInvulnerable()) {
            return false;
        }
        context.log(attacker.getName() + " uso " + name + ".");
        context.log("El ataque no ha alcanzado a " + defender.getName() + ".");
        return true;
    }

    /**
     * Rolls the accuracy of the move taking the attacker's accuracy stage into account.
     *
     * @param attacker combatant using the move
     * @param context  battle collaborators
     * @return true when the move hits
     */
    protected boolean rollAccuracy(Combatant attacker, BattleContext context) {
        int adjusted = (int) (accuracy * attacker.getAccuracyMultiplier());
        return context.getRandom().nextInt(1, 100) <= adjusted;
    }

    /**
     * Tries to inflict a status condition on the opponent.
     *
     * @param status   condition to inflict
     * @param defender opponent
     * @param context  battle collaborators
     * @return true when the condition was applied
     */
    protected boolean tryInflictStatus(StatusCondition status, Combatant defender, BattleContext context) {
        if (status == null || context.getEffectiveness().multiplier(type, defender.getPokemon()) == 0) {
            return false;
        }
        return status.applyTo(defender, context);
    }
}
