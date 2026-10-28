package com.pokemon.battle.service;

import com.pokemon.battle.model.BattleContext;
import com.pokemon.battle.model.BattleLog;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.RandomProvider;
import com.pokemon.battle.model.TypeEffectiveness;

/**
 * Rules of a battle round: who moves first, residual damage and turn execution.
 */
public class BattleService implements BattleEngine {

    private final RandomProvider random;
    private final TypeEffectiveness effectiveness;

    /**
     * @param random        source of randomness (injected)
     * @param effectiveness type chart (injected)
     */
    public BattleService(RandomProvider random, TypeEffectiveness effectiveness) {
        this.random = random;
        this.effectiveness = effectiveness;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoundResult playRound(Combatant c1, Combatant c2, Move m1, Move m2) {
        BattleLog log = new BattleLog();
        BattleContext context = new BattleContext(random, effectiveness, log);

        boolean c1First = goesFirst(m1, m2, c1, c2);
        Combatant first = c1First ? c1 : c2;
        Combatant second = c1First ? c2 : c1;
        Move firstMove = c1First ? m1 : m2;
        Move secondMove = c1First ? m2 : m1;

        // Daño residual de veneno y quemadura al inicio del ciclo
        first.getStatus().onCycleStart(first, context);
        second.getStatus().onCycleStart(second, context);
        if (first.isFainted() || second.isFainted()) {
            Combatant fainted = first.isFainted() ? first : second;
            return new RoundResult(log.getMessages(), fainted);
        }

        context.log("");
        if (executeTurn(first, second, firstMove, context)) {
            return new RoundResult(log.getMessages(), second);
        }
        context.log("");
        if (executeTurn(second, first, secondMove, context)) {
            return new RoundResult(log.getMessages(), first);
        }
        return new RoundResult(log.getMessages(), null);
    }

    private boolean executeTurn(Combatant attacker, Combatant defender, Move move, BattleContext context) {
        if (canAct(attacker, context)) {
            move.execute(attacker, defender, context);
        } else {
            attacker.cancelCharge();
        }
        return defender.isFainted();
    }

    // Se evalua justo antes de mover: un estado contraido en este mismo ciclo ya cuenta
    private boolean canAct(Combatant combatant, BattleContext context) {
        if (combatant.isFainted()) {
            return false;
        }
        if (combatant.consumeFlinch()) {
            context.log("");
            context.log(combatant.getName() + " no pudo moverse.");
            return false;
        }
        return !combatant.getStatus().preventsAction(combatant, context);
    }

    private boolean goesFirst(Move m1, Move m2, Combatant c1, Combatant c2) {
        if (m1.getPriority() != m2.getPriority()) {
            return m1.getPriority() > m2.getPriority();
        }
        if (c1.getSpeed() != c2.getSpeed()) {
            return c1.getSpeed() > c2.getSpeed();
        }
        return random.nextInt(1, 100) > 50;
    }
}
