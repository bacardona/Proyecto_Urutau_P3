package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;

/**
 * One move used during a round. The battle screen uses it to play the
 * animation of the attack at the right moment, before the messages that come
 * after it.
 */
public class MoveEvent {

    /**
     * What the move did this time.
     */
    public enum Kind {
        /** A normal move. */
        NORMAL,
        /** First turn of a two-turn move: the user gets ready (for example, flies up). */
        CHARGE,
        /** Second turn of a two-turn move: the user comes back and hits. */
        RELEASE
    }

    private final Combatant attacker;
    private final Combatant defender;
    private final Move move;
    private final Kind kind;
    private final boolean invulnerable;
    private final boolean missed;
    private final boolean selfTarget;
    private final int messageIndex;
    private final int attackerHpBefore;
    private final int attackerHpAfter;
    private final int defenderHpBefore;
    private final int defenderHpAfter;

    /**
     * @param attacker         who used the move
     * @param defender         the rival
     * @param move             the move used
     * @param kind             normal, charge turn or release turn
     * @param invulnerable     true when the user cannot be hit while charging (Fly, Dig)
     * @param missed           true when the move failed or did not reach the rival
     * @param selfTarget       true when the move acts on the user (for example, Growth)
     * @param messageIndex     position of the first message of this move in the round
     * @param attackerHpBefore HP of the user before the move
     * @param attackerHpAfter  HP of the user after the move
     * @param defenderHpBefore HP of the rival before the move
     * @param defenderHpAfter  HP of the rival after the move
     */
    public MoveEvent(Combatant attacker, Combatant defender, Move move, Kind kind, boolean invulnerable,
                     boolean missed, boolean selfTarget, int messageIndex, int attackerHpBefore,
                     int attackerHpAfter, int defenderHpBefore, int defenderHpAfter) {
        this.attacker = attacker;
        this.defender = defender;
        this.move = move;
        this.kind = kind;
        this.invulnerable = invulnerable;
        this.missed = missed;
        this.selfTarget = selfTarget;
        this.messageIndex = messageIndex;
        this.attackerHpBefore = attackerHpBefore;
        this.attackerHpAfter = attackerHpAfter;
        this.defenderHpBefore = defenderHpBefore;
        this.defenderHpAfter = defenderHpAfter;
    }

    /**
     * @param offset how many messages go before the ones of the round
     * @return the same event with its message position moved by offset
     */
    public MoveEvent shifted(int offset) {
        return new MoveEvent(attacker, defender, move, kind, invulnerable, missed, selfTarget,
                messageIndex + offset, attackerHpBefore, attackerHpAfter, defenderHpBefore, defenderHpAfter);
    }

    public Combatant getAttacker() {
        return attacker;
    }

    public Combatant getDefender() {
        return defender;
    }

    public Move getMove() {
        return move;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * @return true when the user hides while charging (flies up or digs)
     */
    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isMissed() {
        return missed;
    }

    public boolean isSelfTarget() {
        return selfTarget;
    }

    public int getMessageIndex() {
        return messageIndex;
    }

    public int getAttackerHpBefore() {
        return attackerHpBefore;
    }

    public int getAttackerHpAfter() {
        return attackerHpAfter;
    }

    public int getDefenderHpBefore() {
        return defenderHpBefore;
    }

    public int getDefenderHpAfter() {
        return defenderHpAfter;
    }
}
