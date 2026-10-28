package com.pokemon.battle.model;

/**
 * A status move that raises or lowers a stat stage of the user or of the opponent.
 */
public class StatChangeMove extends Move {

    private final StatType stat;
    private final int stages;
    private final boolean targetsSelf;

    /**
     * @param id          unique key of the move
     * @param name        display name
     * @param type        elemental type
     * @param accuracy    base accuracy
     * @param pp          power points
     * @param priority    move priority
     * @param stat        stat that changes
     * @param stages      stages to add (negative lowers the stat)
     * @param targetsSelf true when the user is the target, false when it is the opponent
     */
    public StatChangeMove(String id, String name, Type type, int accuracy, int pp, int priority,
                          StatType stat, int stages, boolean targetsSelf) {
        super(id, name, type, MoveCategory.STATUS, 0, accuracy, pp, priority);
        this.stat = stat;
        this.stages = stages;
        this.targetsSelf = targetsSelf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        if (!targetsSelf && isOutOfReach(attacker, defender, context)) {
            return;
        }
        Combatant target = targetsSelf ? attacker : defender;
        context.log(attacker.getName() + " uso " + getName() + ".");
        // Un rival no puede bajar más una estadística que ya está en -6
        if (!targetsSelf && target.getStages().get(stat) < -5) {
            context.log(getName() + " no ha surtido efecto.");
            return;
        }
        int newStage = target.getStages().change(stat, stages);
        context.log(getName() + " modifica " + stat.getLabel() + " de " + target.getName()
                + " a " + newStage + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        String owner = targetsSelf ? "propio" : "del rival";
        String sign = stages > 0 ? "+" : "";
        return describeHeader() + " - " + stat.getLabel() + " " + owner + " " + sign + stages
                + ", precision " + getAccuracy();
    }

    /**
     * @return the stat that changes
     */
    public StatType getStat() {
        return stat;
    }

    /**
     * @return the stages added to the stat
     */
    public int getStages() {
        return stages;
    }

    /**
     * @return true when the user is the target of the change
     */
    public boolean targetsSelf() {
        return targetsSelf;
    }
}
