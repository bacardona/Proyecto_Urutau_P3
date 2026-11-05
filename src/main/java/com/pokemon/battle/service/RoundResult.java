package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;

import java.util.List;

/**
 * Outcome of one round: the messages to show and, if any, who fainted.
 */
public class RoundResult {

    private final List<String> messages;
    private final Combatant fainted;

    /**
     * @param messages messages produced during the round
     * @param fainted  combatant that fainted, or null when both can continue
     */
    public RoundResult(List<String> messages, Combatant fainted) {
        this.messages = messages;
        this.fainted = fainted;
    }

    /**
     * @return the messages produced during the round
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * @return the combatant that fainted, or null
     */
    public Combatant getFainted() {
        return fainted;
    }

    /**
     * @return true when the battle is over
     */
    public boolean isBattleOver() {
        return fainted != null;
    }
}
