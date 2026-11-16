package com.pokemon.battle.fighting;

import com.pokemon.battle.MainGUI;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.SpriteLoader;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/** Gameplay checks, plus --window: real application startup, Swing bindings and live timers. */
public final class FreeFightGameplayCheck {
    private static final String[] SPECIES = {"BULBASAUR", "CHARMANDER", "SQUIRTLE", "EEVEE"};
    private static final double DT = 1.0 / 60;
    private static int checks;

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static Pokemon pokemon(String name) {
        return new Pokemon(1, name, 50, 50, 50, 50, 50, 50, Type.NORMAL, Type.NONE, List.of());
    }
    private static FreeFightSetup setup(String first, String second) {
        var setup = new FreeFightSetup();
        setup.setPlayerOne(new Player(1, "J1", 0, 0), List.of(pokemon(first), pokemon("EEVEE"), pokemon("BULBASAUR")));
        setup.setPlayerTwo(new Player(2, "J2", 0, 0), List.of(pokemon(second), pokemon("SQUIRTLE"), pokemon("CHARMANDER")));
        return setup;
    }
    private static boolean verticalOverlap(Fighter a, Fighter b) {
        return a.getY() > b.getY()-b.getRenderHeight()+1e-7
                && b.getY() > a.getY()-a.getRenderHeight()+1e-7;
    }
    private static void separate(Fighter a, Fighter b, String message) {
        check(!verticalOverlap(a,b) || Math.abs(a.getX()-b.getX())+1e-6 >= a.getBodyHalfWidth()+b.getBodyHalfWidth(), message);
    }
    private static void ticks(FightingGameSession s, FightingInput input, int count) {
        for (int i=0;i<count;i++) s.update(input,DT);
    }
    private static void collisions() {
        for (String one:SPECIES) for (String two:SPECIES) for(boolean reverse:new boolean[]{false,true}) {
            var s=new FightingGameSession(setup(one,two)); var a=s.getFighters().get(0); var b=s.getFighters().get(1);
            a.setX(reverse?900:380);b.setX(reverse?600:680);
            double stationary=b.getX();var input=new FightingInput();
            if(reverse)input.setPlayerOneLeft(true);else input.setPlayerOneRight(true);
            ticks(s,input,180);separate(a,b,"Walk-through "+one+"/"+two);
            check(Math.abs(b.getX()-stationary)<1e-6,"Stationary rival pushed");
            check((reverse?a.getX()>b.getX():a.getX()<b.getX()),"Ground side exchanged");
            double ax=a.getX(),bx=b.getX();
            if(reverse)input.setPlayerTwoRight(true);else input.setPlayerTwoLeft(true);
            for(int i=0;i<120;i++){
                s.update(input,DT); separate(a,b,"Overlap at contact");
                check(Math.abs(a.getX()-ax)<1e-6 && Math.abs(b.getX()-bx)<1e-6,"Contact drifts/jitters");
            }
            check(a.getHealth()==200 && b.getHealth()==200,"Contact damage");
        }
        for(String one:SPECIES)for(String two:SPECIES) {
            Fighter a=new Fighter(one,200,677,false),b=new Fighter(two,700,677,true);
            var a0=PhysicsSystem.positionOf(a);var b0=PhysicsSystem.positionOf(b);
            a.setX(1100);PhysicsSystem.resolveBodies(a,b,a0,b0,1280);
            separate(a,b,"Fast movement tunnels");check(a.getX()<b.getX() && b.getX()==700,"Sweep pushes/crosses");
            a.setX(500);b.setX(500);a0=PhysicsSystem.positionOf(a);b0=PhysicsSystem.positionOf(b);
            PhysicsSystem.resolveBodies(a,b,a0,b0,1280);separate(a,b,"Spawn overlap unresolved");
            a.setX(a.getBodyHalfWidth());b.setX(b.getBodyHalfWidth());
            PhysicsSystem.resolveBodies(a,b,PhysicsSystem.positionOf(a),PhysicsSystem.positionOf(b),1280);
            separate(a,b,"Wall overlap");check(a.getX()>=a.getBodyHalfWidth() && b.getX()>=b.getBodyHalfWidth(),"Wall bounds");
            a.setX(1280-a.getBodyHalfWidth());b.setX(1280-b.getBodyHalfWidth());
            PhysicsSystem.resolveBodies(a,b,PhysicsSystem.positionOf(a),PhysicsSystem.positionOf(b),1280);
            separate(a,b,"Right-wall overlap");check(a.getX()<=1280-a.getBodyHalfWidth() && b.getX()<=1280-b.getBodyHalfWidth(),"Right-wall bounds");
            a.setX(200);b.setX(1000);a0=PhysicsSystem.positionOf(a);b0=PhysicsSystem.positionOf(b);
            a.setX(1000);b.setX(200);PhysicsSystem.resolveBodies(a,b,a0,b0,1280);
            separate(a,b,"Two fast movers tunnel");check(a.getX()<b.getX(),"Fast movers exchange through bodies");
        }
        // The time of crossing matters: clear overhead, then descend on the far side.
        Fighter a=new Fighter("SQUIRTLE",200,400,false),b=new Fighter("CHARMANDER",600,677,true);
        a.setOnGround(false);var a0=PhysicsSystem.positionOf(a);var b0=PhysicsSystem.positionOf(b);
        a.setX(900);a.setY(677);PhysicsSystem.resolveBodies(a,b,a0,b0,1280);
        check(a.getX()==900,"Artificial no-swap rule blocks overhead crossing");
        a.setX(200);a.setY(677);a0=PhysicsSystem.positionOf(a);
        a.setX(900);a.setY(400);PhysicsSystem.resolveBodies(a,b,a0,b0,1280);
        // This arc clears vertically before reaching the rival, so it may also cross.
        check(a.getX()==900,"Separated heights blocked");
        a.setX(600);a.setY(570);a0=PhysicsSystem.positionOf(a);a.setY(640);
        PhysicsSystem.resolveBodies(a,b,a0,PhysicsSystem.positionOf(b),1280);
        separate(a,b,"Descending into rival");check(b.getX()==600,"Landing pushes grounded rival");
        System.out.println("OK solid bodies: 32 walk/contact cases, sweeps, overlaps, boundaries and vertical crossing");
    }
    private static void jumpEvolutionAndAttacks() {
        for(String species:SPECIES)for(boolean reverse:new boolean[]{false,true}) {
            var s=new FightingGameSession(setup(species,"CHARMANDER"));var a=s.getFighters().get(0);var b=s.getFighters().get(1);
            a.setX(reverse?920:360);b.setX(reverse?730:550);
            var input=new FightingInput();input.setPlayerOneJump(true);
            if(reverse)input.setPlayerOneLeft(true);else input.setPlayerOneRight(true);
            for(int i=0;i<100;i++){s.update(input,DT);separate(a,b,"Jump overlap");}
            check(reverse?a.getX()<b.getX():a.getX()>b.getX(),"Cannot jump over");
            check(b.getX()==(reverse?730:550),"Jump pushes rival");

            s=new FightingGameSession(setup(species,"SQUIRTLE"));a=s.getFighters().get(0);b=s.getFighters().get(1);
            a.setX(600);a.setFacingLeft(reverse);b.setX(600+(reverse?-1:1)*(a.getBodyHalfWidth()+b.getBodyHalfWidth()));
            input=new FightingInput();input.setPlayerOneAttackOne(true);
            for(int i=0;i<16;i++){s.update(input,DT);separate(a,b,"Attack overlaps body");}
            check(b.getHealth()==192,"Placaje misses at body contact");
            input.setPlayerOneAttackTwo(true);s.update(input,DT);
            check(!s.getCombatSystem().getAttackVisual(a,s.getFighters()).definition().moveId().equals("TACKLE"),"Combo broken by solid bodies");

            s=new FightingGameSession(setup(species,"BULBASAUR"));a=s.getFighters().get(0);b=s.getFighters().get(1);
            a.setX(600);b.setX(600+a.getBodyHalfWidth()+b.getBodyHalfWidth());
            a.addEvolutionEnergy(100);input=new FightingInput();input.setPlayerOneEvolution(true);s.update(input,DT);
            check(a.isEvolved(),"Evolution blocked");separate(a,b,"Evolution overlap");
            a.returnToBaseAfterUltimate();s.update(input,DT);separate(a,b,"Return to base overlap");
            b.setX(230);s.debugDefeatActiveFighter(0);ticks(s,new FightingInput(),120);
            separate(s.getFighters().get(0),b,"Replacement inside rival");
        }
        var s=new FightingGameSession(setup("EEVEE","CHARMANDER"));var a=s.getFighters().get(0);var b=s.getFighters().get(1);
        a.setX(500);b.setX(570);
        var combat=s.getCombatSystem();combat.update(s.getFighters(),1280,List.of(),1,-1,0.12);
        combat.update(s.getFighters(),1280,List.of(),-1,-1,0.20);
        separate(a,b,"Quick Attack tunnels");check(a.getX()<b.getX() && b.getHealth()<200,"Quick Attack lost contact");
        System.out.println("OK jump-over, exact-contact attacks/combos, quick attack, evolution and replacements");
    }
    private static void shieldsAndTackle() throws Exception {
        var renderer=new FightingRenderer(SpriteLoader.shared());
        var draw=FightingRenderer.class.getDeclaredMethod("drawBlockShield",Graphics2D.class,Fighter.class,FightingCombatSystem.class,boolean.class);
        draw.setAccessible(true);
        var attackFor=FightingCombatSystem.class.getDeclaredMethod("attackFor",String.class,int.class);attackFor.setAccessible(true);
        BufferedImage preview=new BufferedImage(900,280,BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg=preview.createGraphics();pg.setColor(new Color(0x293746));pg.fillRect(0,0,900,280);
        var spriteDraw=FightingRenderer.class.getDeclaredMethod("drawSpriteAnimation",Graphics2D.class,Fighter.class,FightingSpriteProfile.class,FightingCombatSystem.AttackVisual.class,double.class);
        spriteDraw.setAccessible(true);
        int column=0;
        for(String species:SPECIES){
            var s=new FightingGameSession(setup(species,"SQUIRTLE"));var f=s.getFighters().get(0);var input=new FightingInput();
            input.setPlayerOneBlock(true);ticks(s,input,15);
            check(f.isBlocking() && f.getShieldOpacity()==1,"Shield not held");
            double age=f.getShieldAnimationSeconds();s.pause();s.update(input,1);
            check(f.getShieldAnimationSeconds()==age && f.getShieldOpacity()==1,"Shield advances in pause");s.resume();
            var onlyShield=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=onlyShield.createGraphics();draw.invoke(renderer,g,f,s.getCombatSystem(),false);draw.invoke(renderer,g,f,s.getCombatSystem(),true);g.dispose();
            int minX=1280,maxX=0,minY=720,maxY=0;
            for(int y=0;y<720;y++)for(int x=0;x<1280;x++)if(onlyShield.getRGB(x,y)>>>24!=0){minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
            check(minX<f.getX()-f.getBodyHalfWidth() && maxX>f.getX()+f.getBodyHalfWidth(),"Shield does not surround body");
            var envelope=FightingSpriteProfile.forFighter(f).shieldEnvelope(f.isFacingLeft());
            check(minY<f.getY()+envelope.centerY() && maxY>f.getY(),"Shield misses upper body/feet");
            f.setX(112+column*225);f.setY(240);
            draw.invoke(renderer,pg,f,s.getCombatSystem(),false);
            spriteDraw.invoke(renderer,pg,f,FightingSpriteProfile.forFighter(f),null,f.getX());
            draw.invoke(renderer,pg,f,s.getCombatSystem(),true);
            pg.setColor(Color.WHITE);pg.drawString(species,(int)f.getX()-45,45);
            f.setX(230);f.setY(677);f.setOnGround(true);
            input.setPlayerOneBlock(false);input.setPlayerOneRight(true);s.update(input,DT);
            check(f.getShieldOpacity()>0 && f.getShieldOpacity()<1,"No shield release transition");
            input.setPlayerOneBlock(true);s.update(input,DT);
            check(f.getShieldAnimationSeconds()<0.03,"Repeated guard does not reactivate");
            input.setPlayerOneBlock(false);
            ticks(s,input,12);check(f.getShieldOpacity()==0,"Released shield stuck");
            input.clear();input.setPlayerOneBlock(true);ticks(s,input,15);
            var rival=s.getFighters().get(1);
            rival.setX(f.getX()+f.getBodyHalfWidth()+rival.getBodyHalfWidth()+6);rival.setFacingLeft(true);
            input.setPlayerTwoAttackOne(true);ticks(s,input,15);
            check(f.getHealth()<200 && f.getHealth()>=197 && f.getShieldOpacity()==1,"Shield must stop most damage but let a little through");
            check(s.getCombatSystem().getImpactEffects().stream().anyMatch(FightingCombatSystem.ImpactEffect::isBlocked),
                    "Missing blocked impact for shield flash");
            f.receiveDamage(1);s.update(input,DT);
            f.applyNormalHitStun(.2);s.update(input,DT);
            check(f.getShieldOpacity()==0,"Shield remains during stun");
            f.markDefeated();check(f.getShieldOpacity()==0,"KO shield stuck");

            var profile=FightingSpriteProfile.forSpecies(species);
            Fighter attacker=new Fighter(species,600,677,false);
            var def=(AttackDefinition)attackFor.invoke(new FightingCombatSystem(),species,0);
            var active=new FightingCombatSystem.AttackVisual(def,"ACTIVE",0,0,false,1);
            for(boolean left:new boolean[]{false,true}) {
                attacker.setFacingLeft(left);
                var frame=renderer.spriteFrame(attacker,profile,active);
                check(frame!=profile.sprites().get("Idle").frame(left,0),"Placaje impact is Idle");
                if(!species.equals("SQUIRTLE")){
                    check(frame==profile.sprites().get(profile.attacks.get("TACKLE")).frame(left,
                            profile.sprites().get(profile.attacks.get("TACKLE")).hitFrame()),"Impact frame not displayed");
                    var recovered=new FightingCombatSystem.AttackVisual(def,"RECOVERY",.9,1,false,1);
                    check(renderer.spriteFrame(attacker,profile,recovered)==renderer.spriteFrame(attacker,profile,null),"Long visual recovery");
                }
            }
            column++;
        }
        pg.dispose();ImageIO.write(preview,"png",new File("target/free-fight-shield-preview.png"));
        check(FightingSpriteProfile.forSpecies("CHARMANDER").blockAnimation.equals("Idle"),"Charmander screaming guard");
        System.out.println("OK shield coverage, release, pause, stun/KO and visible Placaje impact/recovery");
    }
    private static void key(JComponent panel,String stroke) {
        Object id=panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(stroke));
        check(id!=null,"Missing binding "+stroke);
        panel.getActionMap().get(id).actionPerformed(new ActionEvent(panel,ActionEvent.ACTION_PERFORMED,"check"));
    }
    private static Object field(Object target,String name) throws Exception {
        Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private static void capture(JComponent component,String name)throws Exception{
        var image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();component.paint(g);g.dispose();ImageIO.write(image,"png",new File("target/"+name));
    }
    private static void windowCheck() throws Exception {
        final Throwable[] failure={null};
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{failure[0]=error;error.printStackTrace();});
        MainGUI.main(new String[0]);
        SwingUtilities.invokeAndWait(()->{});
        final GameWindow[] main={null};final FightingGamePanel[] panel={null};final FightingGameSession[] session={null};
        SwingUtilities.invokeAndWait(()->{
            try {
                for(Window w:Window.getWindows())if(w instanceof GameWindow game && game.isShowing())main[0]=game;
                check(main[0]!=null,"MainGUI did not open normally");
                var screens=(java.util.Map<?,?>)field(main[0],"screens");
                check(((JComponent)screens.get("intro")).isShowing(),"Startup intro not visible");
                capture((JComponent)main[0].getContentPane(),"free-fight-app-startup.png");
                key((JComponent)screens.get("intro"),"pressed ENTER");
                main[0].showScreen("menu");
                // Supply in-memory selections through the existing setup object: no Oracle mutations.
                panel[0]=(FightingGamePanel)screens.get("fighting");
                var setup=(FreeFightSetup)field(panel[0],"setup");
                var selected=setup("BULBASAUR","CHARMANDER");
                setup.setPlayerOne(selected.getPlayerOne(),selected.getTeamOne());
                setup.setPlayerTwo(selected.getPlayerTwo(),selected.getTeamTwo());
                main[0].setGameSize(1280,720);main[0].showScreen("fighting");
                session[0]=(FightingGameSession)field(panel[0],"session");
                check(panel[0].isShowing() && session[0].getFighters().get(0).getPokemonId().equals("BULBASAUR"),"Real fighting mode did not activate");
                var a=session[0].getFighters().get(0);var b=session[0].getFighters().get(1);a.setX(490);b.setX(700);
            }catch(Throwable e){failure[0]=e;}
        });
        if(failure[0]==null)FightingWindowChecks.awaitGameplay(panel[0]);
        SwingUtilities.invokeAndWait(()->{if(failure[0]==null){key(panel[0],"pressed D");key(panel[0],"pressed LEFT");}});
        Thread.sleep(1200);
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]!=null)return;
                var a=session[0].getFighters().get(0);var b=session[0].getFighters().get(1);
                separate(a,b,"Live timers walk through");
                check(a.getX()>490 && a.getX()<b.getX(),"Live simulation did not move");
                key(panel[0],"released D");key(panel[0],"released LEFT");
                key(panel[0],"pressed S");key(panel[0],"pressed DOWN");
            }catch(Throwable e){failure[0]=e;}
        });
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]!=null)return;
                check(session[0].getFighters().stream().allMatch(f->f.isBlocking()&&f.getShieldOpacity()>0),"Live guard");
                capture(panel[0],"free-fight-live-block.png");
                key(panel[0],"released S");key(panel[0],"released DOWN");
            }catch(Throwable e){failure[0]=e;}
        });
        Thread.sleep(250);
        SwingUtilities.invokeAndWait(()->{if(failure[0]==null)key(panel[0],"pressed F");});
        Thread.sleep(250);
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]!=null)return;
                check(session[0].getFighters().get(1).getHealth()==192,"Live Placaje does not connect");
                key(panel[0],"released F");key(panel[0],"pressed ESCAPE");
                check(session[0].getState()==FightingGameSession.State.PAUSED,"Live pause failed");
                capture(panel[0],"free-fight-live-pause.png");
            }catch(Throwable e){failure[0]=e;}
        });
        double before=session[0].getFighters().get(0).getMotionAnimationSeconds();
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]!=null)return;
                check(session[0].getFighters().get(0).getMotionAnimationSeconds()==before,"Live paused clock advances");
                key(panel[0],"pressed ESCAPE");
                check(session[0].getState()==FightingGameSession.State.RUNNING,"Live resume failed");
                key(panel[0],"pressed D");key(panel[0],"pressed W");
            }catch(Throwable e){failure[0]=e;}
        });
        Thread.sleep(800);
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]!=null)return;
                check(session[0].getFighters().get(0).getX()>session[0].getFighters().get(1).getX(),"Live jump-over failed");
                key(panel[0],"released D");key(panel[0],"released W");
                session[0].debugDefeatActiveFighter(0);
            }catch(Throwable e){failure[0]=e;}
        });
        FightingWindowChecks.awaitCondition(() -> failure[0]!=null
                || session[0].getFighters().get(0).getPokemonId().equals("EEVEE"), "Live replacement did not complete");
        SwingUtilities.invokeAndWait(()->{
            try{
                if(failure[0]==null){
                    check(session[0].getFighters().get(0).getPokemonId().equals("EEVEE"),"Live replacement failed");
                    separate(session[0].getFighters().get(0),session[0].getFighters().get(1),"Live replacement overlap");
                    main[0].showScreen("menu");
                }
            }catch(Throwable e){failure[0]=e;}
            finally{for(Window window:Window.getWindows())window.dispose();}
        });
        if(failure[0]!=null)throw new AssertionError("Window smoke failed",failure[0]);
        System.out.println("OK normal MainGUI startup, real FightingGamePanel, live timers, key bindings, guard, hit, pause/resume, jump-over and replacement");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("pokemon.fighting.skipIntro", "true");
        try {
            if(args.length>0 && args[0].equals("--window"))windowCheck();
            else {collisions();jumpEvolutionAndAttacks();shieldsAndTackle();}
            System.out.println("PASS gameplay: "+checks+" checks");
        } catch(Throwable e) {e.printStackTrace();System.exit(1);}
        if(args.length>0)System.exit(0);
    }
}
