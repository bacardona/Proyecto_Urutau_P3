package com.pokemon.battle.model;

/**
 * A status move whose only purpose is to inflict a condition on the opponent.
 */
public class InflictStatusMove extends Move {

    private final StatusCondition status;

    /**
     * @param id       unique key of the move
     * @param name     display name
     * @param type     elemental type
     * @param accuracy base accuracy
     * @param pp       power points
     * @param priority move priority
     * @param status   condition to inflict
     */
    public InflictStatusMove(String id, String name, Type type, int accuracy, int pp, int priority,
                             StatusCondition status) {
        super(id, name, type, MoveCategory.STATUS, 0, accuracy, pp, priority);
        this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        if (isOutOfReach(attacker, defender, context)) {
            return;
        }
        if (!rollAccuracy(attacker, context)) {
            context.log("El ataque de " + attacker.getName() + " ha fallado.");
            return;
        }
        context.log(attacker.getName() + " uso " + getName() + ".");
        if (!tryInflictStatus(status, defender, context)) {
            context.log("No ha surtido efecto en " + defender.getName() + ".");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return describeHeader() + " - Causa " + status.getAbbreviation() + ", precision " + getAccuracy();
    }

    /**
     * @return the condition this move inflicts
     */
    public StatusCondition getStatus() {
        return status;
    }
}
