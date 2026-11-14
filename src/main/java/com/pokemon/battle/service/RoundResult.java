package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;

import java.util.Collections;
import java.util.List;

/**
 * Outcome of one round: the messages to show and, if any, who fainted.
 */
public class RoundResult {

    private final List<String> messages;
    private final Combatant fainted;
    private final List<MoveEvent> events;

    /**
     * @param messages messages produced during the round
     * @param fainted  combatant that fainted, or null when both can continue
     */
    public RoundResult(List<String> messages, Combatant fainted) {
        this(messages, fainted, Collections.emptyList());
    }

    /**
     * @param messages messages of the round
     * @param fainted  combatant that fainted, or null
     * @param events   moves used in the round, in order, for the battle animations
     */
    public RoundResult(List<String> messages, Combatant fainted, List<MoveEvent> events) {
        this.messages = messages;
        this.fainted = fainted;
        this.events = events;
    }

    /**
     * @return the moves used in the round, in the order they happened
     */
    public List<MoveEvent> getEvents() {
        return events;
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
