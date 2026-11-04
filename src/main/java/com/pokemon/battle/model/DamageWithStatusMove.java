package com.pokemon.battle.model;

/**
 * A damaging move that may also inflict a status condition on the opponent.
 */
public class DamageWithStatusMove extends DamageMove {

    private final StatusCondition status;
    private final int effectChance;

    /**
     * @param id           unique key of the move
     * @param name         display name
     * @param type         elemental type
     * @param category     physical or special
     * @param power        base power
     * @param accuracy     base accuracy
     * @param pp           power points
     * @param priority     move priority
     * @param status       condition that may be inflicted
     * @param effectChance chance (1 to 100) of inflicting the condition
     */
    public DamageWithStatusMove(String id, String name, Type type, MoveCategory category, int power,
                                int accuracy, int pp, int priority, StatusCondition status,
                                int effectChance) {
        super(id, name, type, category, power, accuracy, pp, priority);
        this.status = status;
        this.effectChance = effectChance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        if (strike(attacker, defender, context)
                && !defender.getHitPoints().isFainted()
                && context.getRandom().nextInt(1, 100) <= effectChance) {
            tryInflictStatus(status, defender, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return super.getDescription() + ", " + effectChance + "% de causar " + status.getAbbreviation();
    }

    /**
     * @return the condition that may be inflicted
     */
    public StatusCondition getStatus() {
        return status;
    }

    /**
     * @return the chance of inflicting the condition
     */
    public int getEffectChance() {
        return effectChance;
    }
}
