package com.pokemon.battle.service;

import com.pokemon.battle.model.BattleContext;
import com.pokemon.battle.model.BattleLog;
import com.pokemon.battle.model.Combatant;
import com.pokemon.battle.model.Move;
import com.pokemon.battle.model.RandomProvider;
import com.pokemon.battle.model.StatChangeMove;
import com.pokemon.battle.model.TypeEffectiveness;

import java.util.ArrayList;
import java.util.List;

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
        List<MoveEvent> events = new ArrayList<>();

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
            return new RoundResult(log.getMessages(), fainted, events);
        }

        context.log("");
        if (executeTurn(first, second, firstMove, context, log, events)) {
            return new RoundResult(log.getMessages(), second, events);
        }
        context.log("");
        if (executeTurn(second, first, secondMove, context, log, events)) {
            return new RoundResult(log.getMessages(), first, events);
        }
        return new RoundResult(log.getMessages(), null, events);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoundResult playSingleAttack(Combatant attacker, Combatant target, Move move) {
        BattleLog log = new BattleLog();
        BattleContext context = new BattleContext(random, effectiveness, log);
        List<MoveEvent> events = new ArrayList<>();

        // Daño residual de veneno y quemadura al inicio del ciclo
        attacker.getStatus().onCycleStart(attacker, context);
        target.getStatus().onCycleStart(target, context);
        if (attacker.isFainted() || target.isFainted()) {
            Combatant fainted = attacker.isFainted() ? attacker : target;
            return new RoundResult(log.getMessages(), fainted, events);
        }

        context.log("");
        executeTurn(attacker, target, move, context, log, events);
        return new RoundResult(log.getMessages(), target.isFainted() ? target : null, events);
    }
    
    // Ademas de ejecutar el ataque, guarda un MoveEvent para que la pantalla lo anime
    private boolean executeTurn(Combatant attacker, Combatant defender, Move move, BattleContext context,
                                BattleLog log, List<MoveEvent> events) {
        if (canAct(attacker, context)) {
            boolean wasCharging = attacker.isCharging();
            int firstMessage = log.getMessages().size();
            int attackerBefore = attacker.getHitPoints().getCurrent();
            int defenderBefore = defender.getHitPoints().getCurrent();

            move.execute(attacker, defender, context);

            MoveEvent.Kind kind = MoveEvent.Kind.NORMAL;
            if (!wasCharging && attacker.isCharging()) {
                kind = MoveEvent.Kind.CHARGE;
            } else if (wasCharging && !attacker.isCharging()) {
                kind = MoveEvent.Kind.RELEASE;
            }
            List<String> newMessages = log.getMessages().subList(firstMessage, log.getMessages().size());
            boolean selfTarget = move instanceof StatChangeMove && ((StatChangeMove) move).isTargetsSelf();
            events.add(new MoveEvent(attacker, defender, move, kind, attacker.isInvulnerable(),
                    missed(newMessages), selfTarget, firstMessage,
                    attackerBefore, attacker.getHitPoints().getCurrent(),
                    defenderBefore, defender.getHitPoints().getCurrent()));
        } else {
            attacker.cancelCharge();
        }
        return defender.isFainted();
    }

    // El ataque fallo, no alcanzo al rival (vuela o esta bajo tierra) o no tuvo efecto
    private boolean missed(List<String> messages) {
        for (String message : messages) {
            String m = message.toLowerCase();
            if (m.contains("ha fallado") || m.contains("no ha alcanzado") || m.contains("no ha surtido efecto")) {
                return true;
            }
        }
        return false;
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
