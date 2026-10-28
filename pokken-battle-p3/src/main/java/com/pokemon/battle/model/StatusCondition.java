package com.pokemon.battle.model;

/**
 * Status conditions. Each constant carries its own behaviour through hooks,
 * so adding a new condition only needs a new constant overriding what it needs.
 */
public enum StatusCondition {

    NONE("   "),

    PARALYSIS("PAR") {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isImmune(Pokemon target) {
            return target.hasType(Type.ELECTRIC);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onApply(Combatant target, BattleContext context) {
            target.halveSpeed();
            context.log(target.getName() + " ha sido paralizado.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCure(Combatant combatant) {
            combatant.restoreSpeed();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean preventsAction(Combatant combatant, BattleContext context) {
            if (context.getRandom().nextDouble(0.0, 1.0) < (1.0 / 4.0)) {
                context.log("");
                context.log("La paralisis no ha dejado moverse a " + combatant.getName() + ".");
                return true;
            }
            return false;
        }
    },

    SLEEP("DOR") {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onApply(Combatant target, BattleContext context) {
            // El contador baja antes de actuar: pierde entre 1 y 3 turnos
            target.setStatusTurns(context.getRandom().nextInt(2, 4));
            context.log(target.getName() + " se ha quedado dormido.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean preventsAction(Combatant combatant, BattleContext context) {
            combatant.setStatusTurns(combatant.getStatusTurns() - 1);
            if (combatant.getStatusTurns() <= 0) {
                cure(combatant);
                context.log("");
                context.log(combatant.getName() + " se ha despertado.");
                return false;
            }
            context.log("");
            context.log(combatant.getName() + " esta profundamente dormido.");
            return true;
        }
    },

    POISON("ENV") {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isImmune(Pokemon target) {
            return target.hasType(Type.POISON) || target.hasType(Type.STEEL);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onApply(Combatant target, BattleContext context) {
            context.log(target.getName() + " ha sido envenenado.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCycleStart(Combatant combatant, BattleContext context) {
            residualDamage(combatant);
            context.log("");
            context.log(combatant.getName() + " ha sufrido dano por el envenenamiento.");
        }
    },

    BURN("QUE") {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isImmune(Pokemon target) {
            return target.hasType(Type.FIRE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onApply(Combatant target, BattleContext context) {
            target.halveAttack();
            context.log(target.getName() + " ha sido quemado.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCure(Combatant combatant) {
            combatant.restoreAttack();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCycleStart(Combatant combatant, BattleContext context) {
            residualDamage(combatant);
            context.log("");
            context.log(combatant.getName() + " ha sufrido dano por la quemadura.");
        }
    },

    FREEZE("CON") {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isImmune(Pokemon target) {
            return target.hasType(Type.ICE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onApply(Combatant target, BattleContext context) {
            context.log(target.getName() + " ha sido congelado.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean preventsAction(Combatant combatant, BattleContext context) {
            if (context.getRandom().nextDouble(0.0, 1.0) < 0.2) {
                cure(combatant);
                context.log("");
                context.log(combatant.getName() + " se ha descongelado.");
                return false;
            }
            context.log("");
            context.log(combatant.getName() + " esta congelado.");
            return true;
        }
    };

    private final String abbreviation;

    StatusCondition(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    /**
     * @return the three-letter abbreviation shown next to the HP bar
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * @param target Pokemon that would receive the condition
     * @return true when the Pokemon cannot suffer this condition
     */
    public boolean isImmune(Pokemon target) {
        return false;
    }

    /**
     * Immediate effect when the condition is contracted.
     *
     * @param target  combatant that contracted it
     * @param context battle collaborators
     */
    public void onApply(Combatant target, BattleContext context) {
    }

    /**
     * Reverts what {@link #onApply} did.
     *
     * @param combatant combatant being cured
     */
    public void onCure(Combatant combatant) {
    }

    /**
     * Residual effect applied once per round before the moves.
     *
     * @param combatant combatant that has the condition
     * @param context   battle collaborators
     */
    public void onCycleStart(Combatant combatant, BattleContext context) {
    }

    /**
     * Asked right before a combatant moves.
     *
     * @param combatant combatant about to move
     * @param context   battle collaborators
     * @return true when the combatant loses its turn
     */
    public boolean preventsAction(Combatant combatant, BattleContext context) {
        return false;
    }

    /**
     * Tries to give this condition to a combatant.
     *
     * @param target  combatant that would receive it
     * @param context battle collaborators
     * @return false when the target already has a condition or is immune
     */
    public final boolean applyTo(Combatant target, BattleContext context) {
        if (this == NONE || target.getStatus() != NONE || isImmune(target.getPokemon())) {
            return false;
        }
        target.setStatus(this);
        onApply(target, context);
        return true;
    }

    /**
     * Removes the condition from a combatant and reverts its effects.
     *
     * @param combatant combatant to cure
     */
    public final void cure(Combatant combatant) {
        onCure(combatant);
        combatant.setStatus(NONE);
        combatant.setStatusTurns(0);
    }

    private static void residualDamage(Combatant combatant) {
        int maxHp = combatant.getHitPoints().getMax();
        combatant.getHitPoints().subtract((int) (maxHp * (1.0 / 16.0)));
    }
}
