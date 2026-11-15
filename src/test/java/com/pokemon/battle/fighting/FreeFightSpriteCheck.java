package com.pokemon.battle.fighting;

import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.SpriteLoader;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/** Database-free integration checks. Run main after mvn test-compile; no test library needed. */
public final class FreeFightSpriteCheck {
    private static final double DT = 1.0 / 60;
    private static final String[] SPECIES = {"BULBASAUR", "CHARMANDER", "EEVEE", "SQUIRTLE"};
    private static int checks;
    private static final class Loader extends SpriteLoader {
        final List<String> requests = new ArrayList<>();
        @Override public GifAnimation getOrLoad(String name, boolean back, Runnable ready) {
            requests.add(name);
            return front(name);
        }
    }
    private static final Loader LOADER = new Loader();
    private static final FightingRenderer RENDERER = new FightingRenderer(LOADER);
    private static final BufferedImage CANVAS = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
    private static Method drawPokemon;
    private static Method attackFor;
    private static Method attackHitbox;
    private static Method drawSprite;

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static FightingGameSession session(String species) throws Exception {
        var ctor = FightingGameSession.class.getDeclaredConstructor(
                String.class, List.class, String.class, List.class, boolean.class);
        ctor.setAccessible(true);
        Pokemon pokemon = new Pokemon(1, species, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
        return ctor.newInstance("P1", List.of(pokemon, pokemon, pokemon),
                "P2", List.of(pokemon, pokemon, pokemon), false);
    }
    private static AttackDefinition definition(String species, int slot) throws Exception {
        return (AttackDefinition) attackFor.invoke(new FightingCombatSystem(), species, slot);
    }
    private static FightingCombatSystem.AttackVisual visual(AttackDefinition def, String phase, double p) {
        return new FightingCombatSystem.AttackVisual(def, phase, p, p, false, 1);
    }
    private static void draw(Fighter f, FightingCombatSystem combat, List<Fighter> fighters) throws Exception {
        Graphics2D graphics = CANVAS.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, CANVAS.getWidth(), CANVAS.getHeight());
        graphics.setComposite(AlphaComposite.SrcOver);
        drawPokemon.invoke(RENDERER, graphics, f, f.getRenderHeight(), 0,
                (Runnable) () -> {}, 677, combat, fighters);
        graphics.dispose();
    }
    private static boolean belongs(SpriteCollabAnimationSet.Animation animation,
                                   SpriteCollabAnimationSet.Frame frame, boolean left) {
        for (int i = 0; i < animation.frameCount(); i++)
            if (frame == animation.frame(left, i)) return true;
        return false;
    }
    private static void stateChecks(String species) throws Exception {
        var profile = FightingSpriteProfile.forSpecies(species);
        var sprites = profile.sprites();
        var used = Collections.newSetFromMap(new IdentityHashMap<SpriteCollabAnimationSet.Animation, Boolean>());
        for (String name : profile.attacks.values()) {
            check(sprites.get(name) != null, species + " missing " + name);
            used.add(sprites.get(name));
        }
        check(used.size() == 4, species + " reuses a sheet for different attacks");
        check(profile.scale == 4, species + " integer pixel scale");
        if (species.equals("CHARMANDER")) {
            check(sprites.get("Shoot") == sprites.get("Charge"), "Charmander Shoot alias not resolved");
            check(sprites.get("Strike") != sprites.get("Attack"), "Real Strike incorrectly overwritten");
        }
        for (boolean left : new boolean[]{false, true}) {
            Fighter f = new Fighter(species, 600, 677, left);
            var combat = new FightingCombatSystem();
            var pair = List.of(f, new Fighter("SQUIRTLE", 1100, 677, true));
            for (String action : new String[]{"Idle", "Walk", "Hop", profile.blockAnimation, "Hurt", "Faint"}) {
                check(sprites.get(action) != null, species + " missing state " + action);
            }
            check(belongs(sprites.get("Idle"), RENDERER.spriteFrame(f, profile, null), left), "Idle");
            f.updateMotionAnimation(Fighter.MotionAnimation.WALKING, 0);
            f.updateMotionAnimation(Fighter.MotionAnimation.WALKING, 0.23);
            check(belongs(sprites.get("Walk"), RENDERER.spriteFrame(f, profile, null), left), "Walk");
            check(sprites.get("Walk").frame(false, 0) != sprites.get("Walk").frame(true, 0), "Native directions");
            f.setOnGround(false);
            for (double vy : new double[]{-650, -300, 0, 300, 650}) {
                f.setVelocityY(vy);
                check(belongs(sprites.get("Hop"), RENDERER.spriteFrame(f, profile, null), left), "Jump");
            }
            f.setVelocityY(0);
            draw(f, combat, pair);
            int topPixel = CANVAS.getHeight();
            for (int y = 0; y < CANVAS.getHeight(); y++) {
                for (int x = 0; x < CANVAS.getWidth(); x++) {
                    if ((CANVAS.getRGB(x, y) >>> 24) != 0) { topPixel = y; break; }
                }
                if (topPixel != CANVAS.getHeight()) break;
            }
            check(topPixel >= f.getY() - f.getRenderHeight() - 20, "Hop adds a second jump displacement");
            f.setOnGround(true);
            f.setVelocityY(0);
            f.setBlocking(true);
            f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING, 0);
            f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING, 1);
            check(belongs(sprites.get(profile.blockAnimation), RENDERER.spriteFrame(f, profile, null), left), "Block");
            f.setBlocking(false);
            f.updateMotionAnimation(Fighter.MotionAnimation.IDLE, 0);
            f.updateMotionAnimation(Fighter.MotionAnimation.IDLE, 0.3);
            for (int slot = 0; slot < 4; slot++) {
                var def = definition(species, slot);
                for (String phase : new String[]{"STARTUP", "ACTIVE", "RECOVERY"}) {
                    for (double p : new double[]{0, 0.5, 1}) {
                        var frame = RENDERER.spriteFrame(f, profile, visual(def, phase, p));
                        boolean statusRecovery = phase.equals("RECOVERY")
                                && (def.kind() == AttackDefinition.Kind.SELF_BUFF || def.kind() == AttackDefinition.Kind.GRUNT);
                        boolean tackleRecovery = phase.equals("RECOVERY") && def.moveId().equals("TACKLE")
                                && !species.equals("SQUIRTLE");
                        check(belongs(sprites.get(profile.attacks.get(def.moveId())), frame, left)
                                        || (statusRecovery || tackleRecovery) && belongs(sprites.get("Idle"), frame, left),
                                species + " " + def.moveId() + " " + phase);
                    }
                }
            }
            f.receiveDamage(1);
            var staleAttack = visual(definition(species, 0), "ACTIVE", 0.5);
            check(belongs(sprites.get("Hurt"), RENDERER.spriteFrame(f, profile, staleAttack), left), "Damage priority");
            draw(f, combat, pair);
            f.markDefeated();
            check(belongs(sprites.get("Faint"), RENDERER.spriteFrame(f, profile, staleAttack), left), "KO priority");
            draw(f, combat, pair);
            check(f.getX() == 600 && f.getY() == 677, "Drawing moves the fighter");
        }
        System.out.println("OK states and four unique attacks: " + species);
    }
    private static void combatChecks(String species) throws Exception {
        for (boolean left : new boolean[]{false, true}) {
            for (int slot = 0; slot < 4; slot++) {
                var session = session(species);
                var f = session.getFighters().get(0);
                var opponent = session.getFighters().get(1);
                f.setFacingLeft(left);
                f.setX(left ? 1000 : 230);
                opponent.setX(left ? 230 : 1000);
                if (definition(species, slot).moveId().equals("FLY")) opponent.setX(left ? 730 : 500);
                var input = new FightingInput();
                switch (slot) {
                    case 0 -> input.setPlayerOneAttackOne(true);
                    case 1 -> input.setPlayerOneAttackTwo(true);
                    case 2 -> input.setPlayerOneAttackThree(true);
                    case 3 -> input.setPlayerOneAttackFour(true);
                }
                session.update(input, DT);
                var visual = session.getCombatSystem().getAttackVisual(f, session.getFighters());
                check(visual != null && visual.definition().moveId().equals(definition(species, slot).moveId()),
                        species + " attack did not start " + slot);
                double initialX = f.getX();
                boolean sawActive = false;
                for (int tick = 0; tick < 480; tick++) {
                    session.update(input, DT);
                    visual = session.getCombatSystem().getAttackVisual(f, session.getFighters());
                    if (visual == null) break;
                    sawActive |= visual.phase().equals("ACTIVE");
                    RENDERER.spriteFrame(f, FightingSpriteProfile.forSpecies(species), visual);
                    if (slot == 0) check(f.getX() == initialX, "Placaje is moving " + species);
                }
                check(sawActive && visual == null, "Attack does not complete " + species + "/" + slot);
                check(f.getMaxHealth() == 200, "HP changed");
            }
            var session = session(species);
            var f = session.getFighters().get(0);
            var target = session.getFighters().get(1);
            f.setX(600); f.setFacingLeft(left);
            target.setX(600 + (left ? -1 : 1) * (f.getBodyHalfWidth() + target.getBodyHalfWidth() + 12));
            var input = new FightingInput(); input.setPlayerOneAttackOne(true);
            for (int i = 0; i < 20 && target.getHealth() == 200; i++) session.update(input, DT);
            check(target.getHealth() == 192, species + " tackle contact or damage incorrect");
            check(session.getCombatSystem().getAttackVisual(f, session.getFighters()).hitConfirmed(), "No hit confirm");
            input.setPlayerOneAttackTwo(true);
            session.update(input, DT);
            check(session.getCombatSystem().getAttackVisual(f, session.getFighters()).definition().moveId()
                    .equals(definition(species, 1).moveId()), species + " combo cancellation failed");

            var blocked = session(species);
            var a = blocked.getFighters().get(0); var b = blocked.getFighters().get(1);
            a.setX(600); a.setFacingLeft(left);
            b.setX(600 + (left ? -1 : 1) * (a.getBodyHalfWidth() + b.getBodyHalfWidth() + 12));
            b.setFacingLeft(!left);
            var blockInput = new FightingInput(); blockInput.setPlayerTwoBlock(true);
            blocked.update(blockInput, DT);
            blockInput.setPlayerOneAttackOne(true);
            for (int i = 0; i < 15; i++) blocked.update(blockInput, DT);
            check(b.getHealth() == 198 && a.isHitStunned(), species + " shield must let 20% of Placaje through");
        }
        System.out.println("OK combat, hit/miss, both sides, combo and blocking: " + species);
    }
    private static void geometryAndSessionChecks(String species) throws Exception {
        var session = session(species);
        var f = session.getFighters().get(0);
        var profile = FightingSpriteProfile.forSpecies(species);
        double half = f.getBodyHalfWidth(); int height = f.getRenderHeight();
        check(!new Hitbox(f.getX()-1, f.getY()-height-5, 2, 2).intersects(f), "Transparent padding collision");
        for (int slot = 0; slot < 4; slot++) {
            var def = definition(species, slot);
            var hitbox = (Hitbox) attackHitbox.invoke(session.getCombatSystem(), f, def);
            if (def.kind() == AttackDefinition.Kind.HITBOX || def.kind() == AttackDefinition.Kind.DASH
                    || def.kind() == AttackDefinition.Kind.LUNGE || def.kind() == AttackDefinition.Kind.FLY) {
                check(hitbox.height() > 0 && hitbox.width() > 0, "Invalid contact geometry");
            }
            if (def.kind() == AttackDefinition.Kind.PROJECTILE) {
                var origin = profile.projectileOrigin(f, def.moveId());
                check(origin.y() < f.getY()-8 && origin.y() >= f.getY()-height-8, "Muzzle height");
                f.setFacingLeft(true);
                var reverse = profile.projectileOrigin(f, def.moveId());
                check(Math.abs(origin.x()+reverse.x()-2*f.getX()) <= 4, "Muzzle directions");
                f.setFacingLeft(false);
            }
        }
        var input = new FightingInput(); input.setPlayerOneRight(true);
        for (int i = 0; i < 10; i++) session.update(input, DT);
        check(f.getX() > 230, "Movement failed");
        session.pause();
        var frame = RENDERER.spriteFrame(f, profile, null);
        double time = f.getMotionAnimationSeconds();
        session.update(input, 2);
        check(time == f.getMotionAnimationSeconds() && frame == RENDERER.spriteFrame(f, profile, null), "Pause advances sprite");
        session.resume();
        input.clear(); input.setPlayerOneJump(true);
        double feet = f.getY(); session.update(input, DT);
        check(f.getY() < feet, "Jump failed");
        // Soltar la tecla: si se mantiene, vuelve a saltar solo al tocar el suelo
        input.setPlayerOneJump(false);
        for (int i = 0; i < 150; i++) session.update(input, DT);
        check(f.isOnGround(), "Platform landing failed");
        check(f.getBodyHalfWidth() == half && f.getRenderHeight() == height, "Body box varies by action");
        session.debugDefeatActiveFighter(0);
        check(session.getFighters().get(0) == f, "Replacement skipped Faint");
        session.pause(); double faintTime = f.getDefeatedAnimationSeconds(); session.update(input, 2);
        check(faintTime == f.getDefeatedAnimationSeconds(), "Paused Faint advances");
        session.resume();
        for (int i = 0; i < 120; i++) session.update(input, DT);
        check(session.getFighters().get(0) != f && session.getPlayerOneTeam().getActiveIndex() == 1,
                "Replacement after Faint failed");
        Graphics2D graphics = CANVAS.createGraphics();
        RENDERER.draw(graphics, session, 0, () -> {});
        graphics.dispose();
        for (int slot = 0; slot < 4; slot++) if (definition(species, slot).kind() == AttackDefinition.Kind.PROJECTILE) {
            for (boolean left : new boolean[]{false, true}) {
                var projectileSession = session(species);
                var shooter = projectileSession.getFighters().get(0);
                var victim = projectileSession.getFighters().get(1);
                shooter.setX(600); shooter.setFacingLeft(left); victim.setX(left ? 400 : 800);
                var shotInput = new FightingInput();
                if (slot == 1) shotInput.setPlayerOneAttackTwo(true);
                else if (slot == 2) shotInput.setPlayerOneAttackThree(true);
                for (int t = 0; t < 90; t++) projectileSession.update(shotInput, DT);
                check(victim.getHealth() == 200 - definition(species, slot).damage(), "Projectile does not connect");
                if (definition(species, slot).moveId().equals("SLEEP_POWDER")) check(victim.isAsleep(), "Sleep effect lost");
            }
        }
        System.out.println("OK body, muzzle, platforms, pause and replacement: " + species);
    }

    private static void ultimateReplacementCheck(String species) throws Exception {
        var session = session(species);
        var target = session.getFighters().get(0); target.receiveDamage(190);
        var owner = session.getFighters().get(1);
        owner.addEvolutionEnergy(100); owner.evolve(); owner.updateStatusEffects(1.1); owner.addEvolutionEnergy(100);
        var input = new FightingInput(); input.setPlayerTwoUltimate(true);
        for (int i = 0; i < 240 && !target.isDefeated(); i++) session.update(input, DT);
        check(target.isDefeated() && session.getFighters().get(0) == target, "Ulti KO skipped Faint");
        for (int i = 0; i < 120; i++) session.update(input, DT);
        check(session.getFighters().get(0) != target && session.getPlayerOneTeam().getActiveIndex() == 1,
                "Ulti KO replacement failed");
    }
    private static void ultimateChecks(String attackerSpecies, String targetSpecies, boolean lethal) throws Exception {
        var session = session(targetSpecies);
        // Use a real evolved owner without changing the receiving base form.
        Fighter owner = new Fighter(attackerSpecies, 250, 677, false);
        owner.addEvolutionEnergy(100); check(owner.evolve(), "Cannot evolve");
        owner.updateStatusEffects(1.1); owner.addEvolutionEnergy(100);
        var target = new Fighter(targetSpecies, 450, 677, true);
        if (lethal) target.receiveDamage(190);
        var pair = List.of(owner, target);
        var combat = new FightingCombatSystem();
        combat.beginUltimateRequests(pair, true, false);
        boolean hit = false;
        int before = target.getHealth();
        for (int i = 0; i < 360; i++) {
            owner.updateStatusEffects(DT); target.updateStatusEffects(DT);
            combat.update(pair, 1280, List.of(), -1, -1, DT);
            hit |= target.getHealth() < before;
            draw(target, combat, pair);
            draw(owner, combat, pair);
            if (target.isDefeated()) {
                check(belongs(FightingSpriteProfile.forSpecies(targetSpecies).sprites().get("Faint"),
                        RENDERER.spriteFrame(target, FightingSpriteProfile.forSpecies(targetSpecies), null), true), "Ulti KO");
            }
        }
        check(hit && target.isDefeated() == lethal, "Ultimate damage/KO");
        check(!target.isUltimateStunned(), "Ultimate stun stuck");
        check(!owner.isEvolved() && FightingSpriteProfile.forFighter(owner) != null, "Return to base failed");
        var evolvedProfile = FightingSpriteProfile.forSpecies(owner.getEvolutionProfile().evolvedPokemonId());
        check(evolvedProfile != null && evolvedProfile.isEvolution()
                        && evolvedProfile.sprites() != FightingSpriteProfile.forFighter(owner).sprites(),
                "Evolved form uses base sheet");
        check(!LOADER.requests.contains(targetSpecies), "Old base GIF requested in ultimate: " + targetSpecies);
    }
    private static void rematchCheck() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var panel = new FightingGamePanel(() -> {});
                var sessionField = FightingGamePanel.class.getDeclaredField("session"); sessionField.setAccessible(true);
                var old = (FightingGameSession) sessionField.get(panel);
                var input = new FightingInput();
                for (int i = 0; i < 3; i++) {
                    old.debugDefeatActiveFighter(0);
                    for (int t = 0; t < 120; t++) old.update(input, DT);
                }
                check(old.getState() == FightingGameSession.State.FINISHED, "Match does not finish");
                var rematch = FightingGamePanel.class.getDeclaredMethod("activateResultOption");
                rematch.setAccessible(true); rematch.invoke(panel);
                var fresh = (FightingGameSession) sessionField.get(panel);
                check(fresh != old && fresh.getState() == FightingGameSession.State.RUNNING, "Rematch failed");
                check(fresh.getFighters().stream().allMatch(f -> f.getHealth() == 200), "Rematch HP");
                var stop = FightingGamePanel.class.getDeclaredMethod("stopGameTimersAndClearInput");
                stop.setAccessible(true); stop.invoke(panel);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
    private static void preview() throws Exception {
        BufferedImage out = new BufferedImage(1650, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics(); g.setColor(new Color(0x293746)); g.fillRect(0,0,1650,1000);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int row = 0;
        for (String species : SPECIES) for (boolean left : new boolean[]{false,true}) {
            var profile = FightingSpriteProfile.forSpecies(species);
            for (int col = 0; col < 10; col++) {
                Fighter f = new Fighter(species, 70+col*165, 104+row*125, left);
                FightingCombatSystem.AttackVisual visual = null;
                String label;
                if (col == 0) label = "Idle";
                else if (col == 1) {label="Walk";f.updateMotionAnimation(Fighter.MotionAnimation.WALKING, 0); f.updateMotionAnimation(Fighter.MotionAnimation.WALKING, .2);}
                else if (col == 2) {label="Hop";f.setOnGround(false);}
                else if (col == 3) {label="Block";f.setBlocking(true);f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING, 0);}
                else if (col < 8) {var def=definition(species,col-4);label=def.moveId();visual=visual(def,"ACTIVE",.3);}
                else if (col == 8) {label="Hurt";f.receiveDamage(1);}
                else {label="Faint";f.markDefeated();f.advanceDefeatedAnimation(.5);}
                g.setColor(Color.WHITE);g.drawString(species+" "+(left?"W":"E")+" "+label,col*165+3,row*125+13);
                g.setColor(new Color(0x667788));g.drawLine(col*165,(int)f.getY(),(col+1)*165,(int)f.getY());
                g.setClip(col*165, row*125+15, 165, 110);
                drawSprite.invoke(RENDERER, g, f, profile, visual, f.getX());
                g.setClip(null);
            }
            row++;
        }
        g.dispose(); ImageIO.write(out,"png",new File("target/free-fight-sprite-preview.png"));
    }
    public static void main(String[] args) throws Exception {
        drawPokemon = FightingRenderer.class.getDeclaredMethod("drawPokemon", Graphics2D.class,
                Fighter.class, int.class, int.class, Runnable.class, int.class,
                FightingCombatSystem.class, List.class); drawPokemon.setAccessible(true);
        attackFor = FightingCombatSystem.class.getDeclaredMethod("attackFor",String.class,int.class);
        attackFor.setAccessible(true);
        attackHitbox = FightingCombatSystem.class.getDeclaredMethod("attackHitbox", Fighter.class, AttackDefinition.class);
        attackHitbox.setAccessible(true);
        drawSprite = FightingRenderer.class.getDeclaredMethod("drawSpriteAnimation", Graphics2D.class,
                Fighter.class, FightingSpriteProfile.class, FightingCombatSystem.AttackVisual.class, double.class);
        drawSprite.setAccessible(true);
        for (String species : SPECIES) {
            stateChecks(species); combatChecks(species); geometryAndSessionChecks(species);
            ultimateReplacementCheck(species);
            for (String owner : SPECIES) for (boolean lethal : new boolean[]{false,true})
                ultimateChecks(owner,species,lethal);
        }
        rematchCheck(); preview();
        check(LOADER.requests.stream().noneMatch(s -> Arrays.asList(SPECIES).contains(s)), "Base GIF fallback");
        System.out.println("PASS: "+checks+" checks; 32 ultimate scenarios; preview in target/free-fight-sprite-preview.png");
    }
}
