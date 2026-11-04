package com.pokemon.battle.model;

/**
 * A damaging move that may make the opponent lose its next action.
 */
public class DamageWithFlinchMove extends DamageMove {

    private final int flinchChance;

    /**
     * @param id           unique key of the move
     * @param name         display name
     * @param type         elemental type
     * @param category     physical or special
     * @param power        base power
     * @param accuracy     base accuracy
     * @param pp           power points
     * @param priority     move priority
     * @param flinchChance chance (1 to 100) of making the opponent flinch
     */
    public DamageWithFlinchMove(String id, String name, Type type, MoveCategory category, int power,
                                int accuracy, int pp, int priority, int flinchChance) {
        super(id, name, type, category, power, accuracy, pp, priority);
        this.flinchChance = flinchChance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        if (strike(attacker, defender, context)
                && context.getRandom().nextInt(1, 100) > 100 - flinchChance) {
            context.log("");
            context.log(getName() + " ha estremecido a " + defender.getName() + ".");
            defender.flinch();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return super.getDescription() + ", " + flinchChance + "% de estremecer";
    }

    /**
     * @return the chance of making the opponent flinch
     */
    public int getFlinchChance() {
        return flinchChance;
    }
}
