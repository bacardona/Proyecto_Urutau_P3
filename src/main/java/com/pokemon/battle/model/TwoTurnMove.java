package com.pokemon.battle.model;

/**
 * A move that needs one turn to charge and hits on the second turn
 * (for example Fly or Dig). It does not extend {@link DamageMove} because its
 * first turn does no damage; the hit is delegated to a composed {@link DamageMove}.
 */
public class TwoTurnMove extends Move {

    private final String chargeMessage;
    private final boolean invulnerableWhileCharging;
    private final DamageMove hit;

    /**
     * @param id                        unique key of the move
     * @param name                      display name
     * @param type                      elemental type
     * @param category                  physical or special
     * @param power                     base power
     * @param accuracy                  base accuracy
     * @param pp                        power points
     * @param priority                  move priority
     * @param chargeMessage             text shown after the Pokemon name on the charge turn
     * @param invulnerableWhileCharging whether the user cannot be hit while charging
     */
    public TwoTurnMove(String id, String name, Type type, MoveCategory category, int power,
                       int accuracy, int pp, int priority, String chargeMessage,
                       boolean invulnerableWhileCharging) {
        super(id, name, type, category, power, accuracy, pp, priority);
        this.chargeMessage = chargeMessage;
        this.invulnerableWhileCharging = invulnerableWhileCharging;
        this.hit = new DamageMove(id, name, type, category, power, accuracy, pp, priority);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Combatant attacker, Combatant defender, BattleContext context) {
        if (!attacker.isCharging()) {
            attacker.startCharging(this, invulnerableWhileCharging);
            context.log(attacker.getName() + " " + chargeMessage + ".");
        } else {
            attacker.cancelCharge();
            hit.execute(attacker, defender, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return hit.getDescription() + ", en dos turnos";
    }

    /**
     * @return the text shown on the charge turn
     */
    public String getChargeMessage() {
        return chargeMessage;
    }

    /**
     * @return true when the user cannot be hit while charging
     */
    public boolean isInvulnerableWhileCharging() {
        return invulnerableWhileCharging;
    }
}
