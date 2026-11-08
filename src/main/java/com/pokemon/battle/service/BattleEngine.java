package com.pokemon.battle.service;

import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;

/**
 * What the controller needs from the battle rules.
 */
public interface BattleEngine {

    /**
     * Plays one full round: residual damage and then both moves in order.
     *
     * @param c1 first player's combatant
     * @param c2 second player's combatant
     * @param m1 move chosen by the first player
     * @param m2 move chosen by the second player
     * @return the messages of the round and who fainted, if anyone
     */
    RoundResult playRound(Combatant c1, Combatant c2, Move m1, Move m2);
    
    /**
     * Plays a round in which only one side attacks because the other side
     * switched Pokemon. The target is the Pokemon that came in.
     *
     * @param attacker the Pokemon that attacks
     * @param target   the Pokemon that receives the attack
     * @param move     the move used by the attacker
     * @return the messages of the round and the Pokemon that fainted, if any
     */
    RoundResult playSingleAttack(Combatant attacker, Combatant target, Move move);
}
