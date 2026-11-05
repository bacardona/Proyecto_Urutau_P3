package com.pokemon.battle.model;

/**
 * A move that hurts the opponent.
 */
public class DamageMove extends Move {

    /**
     * @param id       unique key of the move
     * @param name     display name
     * @param type     elemental type
     * @param category physical or special
     * @param power    base power
     * @param accuracy base accuracy
     * @param pp       power points
     * @param priority move priority
     */
    public DamageMove(String id, String name, Type type, MoveCategory category, int power,
                      int accuracy, int pp, int priority) {
        super(id, name, type, category, power, accuracy, pp, priority);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        strike(attacker, defender, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        String kind = getCategory() == MoveCategory.PHYSICAL ? "fisico" : "especial";
        return describeHeader() + " - Dano " + kind + ", poder " + getPower()
                + ", precision " + getAccuracy();
    }

    /**
     * Resolves the hit: invulnerability, accuracy, damage and messages.
     *
     * @param attacker combatant using the move
     * @param defender opponent
     * @param context  battle collaborators
     * @return true when the move hit the opponent
     */
    protected boolean strike(Combatant attacker, Combatant defender, BattleContext context) {
        if (isOutOfReach(attacker, defender, context)) {
            return false;
        }
        if (!rollAccuracy(attacker, context)) {
            context.log("El ataque de " + attacker.getName() + " ha fallado.");
            return false;
        }
        double effectiveness = context.getEffectiveness().multiplier(getType(), defender.getPokemon());
        int damage = calculateDamage(attacker, defender, effectiveness, context);
        defender.getHitPoints().subtract(damage);

        context.log(attacker.getName() + " uso " + getName() + ".");
        if (effectiveness >= 2) {
            context.log("El ataque ha sido super efectivo.");
        } else if (effectiveness < 1) {
            context.log("El ataque no ha sido muy efectivo.");
        }
        return true;
    }

    private int calculateDamage(Combatant attacker, Combatant defender, double effectiveness,
                                BattleContext context) {
        double attackStat;
        double defenseStat;
        if (getCategory() == MoveCategory.PHYSICAL) {
            attackStat = attacker.getEffectiveAttack();
            defenseStat = defender.getEffectiveDefense();
        } else {
            attackStat = attacker.getEffectiveSpecialAttack();
            defenseStat = defender.getEffectiveSpecialDefense();
        }
        int level = attacker.getPokemon().getLevel();
        int damage = (int) ((((2.0 * level / 5.0 + 2.0) * getPower() * (attackStat / defenseStat)) / 50.0) + 2.0);

        double modifier = 1.0;
        // STAB: el tipo del ataque coincide con el tipo principal del atacante
        if (attacker.getPokemon().getPrimaryType() == getType()) {
            modifier *= 1.5;
        }
        // Golpe crítico: 10 % de probabilidad
        if (context.getRandom().nextInt(1, 100) <= 10) {
            modifier *= context.getRandom().nextInt(1, 100) > 50 ? 2.0 : 1.5;
        }
        modifier *= context.getRandom().nextDouble(0.85, 1.0);
        modifier *= effectiveness;
        return (int) (damage * modifier);
    }
}
