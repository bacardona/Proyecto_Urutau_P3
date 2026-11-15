package com.pokemon.battle.fighting;

import com.pokemon.battle.MainGUI;
import com.pokemon.battle.model.Player;
import com.pokemon.battle.model.Pokemon;
import com.pokemon.battle.model.Type;
import com.pokemon.battle.ui.GameWindow;
import com.pokemon.battle.ui.GifAnimation;
import com.pokemon.battle.ui.SpriteLoader;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/** Evolution integration, with --window for the real application's panel and timers. */
public final class FreeFightEvolutionCheck {
    private static final String[] BASE={"BULBASAUR","CHARMANDER","SQUIRTLE","EEVEE"};
    private static final String[] EVOLVED={"IVYSAUR","CHARMELEON","WARTORTLE","FLAREON"};
    private static final double DT=1.0/60;
    private static int checks;
    private static final class Loader extends SpriteLoader {
        final List<String> gifs=new ArrayList<>();
        @Override public GifAnimation getOrLoad(String species,boolean back,Runnable ready){
            gifs.add(species);return front(species);
        }
    }
    private static final Loader LOADER=new Loader();
    private static final FightingRenderer RENDERER=new FightingRenderer(LOADER);
    private static final BufferedImage CANVAS=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
    private static Method drawPokemon,attackFor,attackHitbox,drawSprite,drawShield;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Pokemon pokemon(String base){return new Pokemon(1,base,50,50,50,50,50,50,Type.NORMAL,Type.NONE,List.of());}
    private static FreeFightSetup setup(String one,String two){
        var s=new FreeFightSetup();
        s.setPlayerOne(new Player(1,"J1",0,0),List.of(pokemon(one),pokemon("EEVEE"),pokemon("BULBASAUR")));
        s.setPlayerTwo(new Player(2,"J2",0,0),List.of(pokemon(two),pokemon("SQUIRTLE"),pokemon("CHARMANDER")));
        return s;
    }
    private static Fighter evolved(String base){
        var f=new Fighter(base,600,677,false);f.addEvolutionEnergy(100);
        check(f.evolve(),"Cannot evolve "+base);f.updateStatusEffects(1.1);return f;
    }
    private static FightingGameSession session(String base){
        var s=new FightingGameSession(setup(base,"SQUIRTLE"));s.debugFillActiveEnergy(0);
        var input=new FightingInput();input.setPlayerOneEvolution(true);s.update(input,DT);
        for(int i=0;i<65;i++)s.update(input,DT);
        check(s.getFighters().get(0).isEvolved()&&!s.getFighters().get(0).isEvolutionAnimating(),"Session evolution failed");
        return s;
    }
    private static FightingInput attackInput(int slot){
        var i=new FightingInput();
        switch(slot){case 0->i.setPlayerOneAttackOne(true);case 1->i.setPlayerOneAttackTwo(true);case 2->i.setPlayerOneAttackThree(true);case 3->i.setPlayerOneAttackFour(true);}
        return i;
    }
    private static AttackDefinition definition(String base,int slot)throws Exception{
        return (AttackDefinition)attackFor.invoke(new FightingCombatSystem(),base,slot);
    }
    private static boolean belongs(SpriteCollabAnimationSet.Animation animation,SpriteCollabAnimationSet.Frame frame,boolean left){
        for(int i=0;i<animation.frameCount();i++)if(frame==animation.frame(left,i))return true;return false;
    }
    private static void draw(Fighter f,FightingCombatSystem combat,List<Fighter> pair)throws Exception{
        var g=CANVAS.createGraphics();g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,1280,720);g.setComposite(AlphaComposite.SrcOver);
        drawPokemon.invoke(RENDERER,g,f,f.getRenderHeight(),0,(Runnable)()->{},677,combat,pair,!combat.getUltimateVisuals().isEmpty());g.dispose();
    }
    private static void noOverlap(Fighter a,Fighter b){
        boolean y=a.getY()>b.getY()-b.getRenderHeight()+1e-7 && b.getY()>a.getY()-a.getRenderHeight()+1e-7;
        check(!y || Math.abs(a.getX()-b.getX())+1e-6>=a.getBodyHalfWidth()+b.getBodyHalfWidth(),"Body overlap");
    }
    private static void states(String base)throws Exception{
        var f=evolved(base);var p=FightingSpriteProfile.forFighter(f);var sprites=p.sprites();
        check(p.isEvolution()&&p.resourceSpecies.equals(base),"Wrong resource package");
        check(sprites!=FightingSpriteProfile.forSpecies(base).sprites(),"Base package reused");
        check(f.getRenderHeight()==p.bodyHeight && f.getBodyHalfWidth()==p.bodyHalfWidth,"Geometry ignores current form");
        var unique=Collections.newSetFromMap(new IdentityHashMap<SpriteCollabAnimationSet.Animation,Boolean>());
        for(String name:p.attacks.values()){check(sprites.get(name)!=null,"Missing attack sheet "+p.species+"/"+name);unique.add(sprites.get(name));}
        check(unique.size()==(p.species.equals("WARTORTLE")?3:4),"Duplicate attack sheet");
        if(p.species.equals("WARTORTLE"))check(!p.attacks.containsKey("BITE"),"Invented bite animation");
        for(boolean left:new boolean[]{false,true}){
            f=evolved(base);f.setFacingLeft(left);var combat=new FightingCombatSystem();var pair=List.of(f,new Fighter("SQUIRTLE",1000,677,true));
            check(belongs(sprites.get("Idle"),RENDERER.spriteFrame(f,p,null),left),"Idle");
            f.updateMotionAnimation(Fighter.MotionAnimation.WALKING,0);f.updateMotionAnimation(Fighter.MotionAnimation.WALKING,.2);
            check(belongs(sprites.get("Walk"),RENDERER.spriteFrame(f,p,null),left),"Walk");
            check(sprites.get("Walk").frame(false,0)!=sprites.get("Walk").frame(true,0),"Native directions lost");
            f.setOnGround(false);
            for(double vy:new double[]{-650,-300,0,300,650}){f.setVelocityY(vy);check(belongs(sprites.get("Hop"),RENDERER.spriteFrame(f,p,null),left),"Hop");}
            f.setVelocityY(0);draw(f,combat,pair);
            int top=720;for(int y=0;y<720&&top==720;y++)for(int x=0;x<1280;x++)if(CANVAS.getRGB(x,y)>>>24!=0){top=y;break;}
            check(top>=f.getY()-p.bodyHeight-24,"Hop doubles vertical movement");
            f.setOnGround(true);f.setBlocking(true);f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING,0);f.updateShieldVisual(false,.2);
            check(f.getShieldOpacity()==1 && belongs(sprites.get(p.blockAnimation),RENDERER.spriteFrame(f,p,null),left),"Guard/bubble");
            var frame=RENDERER.spriteFrame(f,p,null);double time=f.getShieldAnimationSeconds();
            draw(f,combat,pair);draw(f,combat,pair);
            check(frame==RENDERER.spriteFrame(f,p,null)&&time==f.getShieldAnimationSeconds(),"Painting advances clocks");
            f.setBlocking(false);f.receiveDamage(1);f.applyNormalHitStun(.2);
            check(belongs(sprites.get("Hurt"),RENDERER.spriteFrame(f,p,null),left)&&f.isEvolved(),"Hurt/form");
            draw(f,combat,pair);f.markDefeated();draw(f,combat,pair);
            check(belongs(sprites.get(p.defeatAnimation),RENDERER.spriteFrame(f,p,null),left),"KO");
        }
        System.out.println("OK states/native directions/unique sheets "+p.species+" scale="+p.scale+" body="+(p.bodyHalfWidth*2)+"x"+p.bodyHeight);
    }
    private static void attacks(String base)throws Exception{
        for(boolean left:new boolean[]{false,true})for(int slot=0;slot<4;slot++){
            var s=session(base);var a=s.getFighters().get(0);var b=s.getFighters().get(1);a.setFacingLeft(left);a.setX(left?1000:230);b.setX(left?230:1000);
            var def=definition(base,slot);if(def.kind()==AttackDefinition.Kind.FLY)b.setX(left?730:500);
            var input=attackInput(slot);double start=a.getX();s.update(input,DT);boolean active=false;
            var p=FightingSpriteProfile.forFighter(a);
            var v=s.getCombatSystem().getAttackVisual(a,s.getFighters());check(v!=null&&v.definition()==def,"Inherited move not started");
            for(int tick=0;tick<480;tick++){
                s.update(input,DT);v=s.getCombatSystem().getAttackVisual(a,s.getFighters());if(v==null)break;
                if(v.phase().equals("ACTIVE")){
                    active=true;var frame=RENDERER.spriteFrame(a,p,v);
                    if(p.attacks.containsKey(def.moveId()))check(belongs(p.sprites().get(p.attacks.get(def.moveId())),frame,left),"Attack overwritten");
                    else check(p.species.equals("WARTORTLE")&&def.moveId().equals("BITE"),"Unexpected missing attack");
                }
                if(slot==0)check(a.getX()==start,"Placaje translates");
                if(tick%6==0)draw(a,s.getCombatSystem(),s.getFighters());
                noOverlap(a,b);
            }
            check(active&&v==null,"Attack completion");
            if(def.kind()==AttackDefinition.Kind.PROJECTILE){
                s=session(base);a=s.getFighters().get(0);b=s.getFighters().get(1);a.setX(600);a.setFacingLeft(left);b.setX(left?400:800);input=attackInput(slot);
                var origin=FightingSpriteProfile.forFighter(a).projectileOrigin(a,def.moveId());
                check(origin.y()<a.getY()-8&&origin.y()>=a.getY()-a.getRenderHeight()-8,"Projectile muzzle height");
                for(int t=0;t<100;t++)s.update(input,DT);
                check(b.getHealth()==200-(int)Math.round(def.damage()*1.12),"Projectile contact/damage bonus");
            }
        }
        for(boolean left:new boolean[]{false,true}){
            var s=session(base);var a=s.getFighters().get(0);var b=s.getFighters().get(1);a.setX(600);a.setFacingLeft(left);b.setX(600+(left?-1:1)*(a.getBodyHalfWidth()+b.getBodyHalfWidth()));
            var i=attackInput(0);for(int t=0;t<20&&b.getHealth()==200;t++)s.update(i,DT);
            check(b.getHealth()==191,"Tackle exact contact/damage bonus");
            i.setPlayerOneAttackTwo(true);s.update(i,DT);
            check(!s.getCombatSystem().getAttackVisual(a,s.getFighters()).definition().moveId().equals("TACKLE"),"Evolved combo cancellation");
        }
        System.out.println("OK inherited four attacks, hit/miss, both sides, contact and combos: "+base);
    }
    private static void collisionsAndKO(String base)throws Exception{
        for(boolean reverse:new boolean[]{false,true}){
            var s=session(base);var a=s.getFighters().get(0);var b=s.getFighters().get(1);a.setX(reverse?900:380);b.setX(reverse?600:680);
            var i=new FightingInput();if(reverse)i.setPlayerOneLeft(true);else i.setPlayerOneRight(true);
            for(int t=0;t<180;t++){s.update(i,DT);noOverlap(a,b);}
            check(reverse?a.getX()>b.getX():a.getX()<b.getX(),"Walk-through");
            s=session(base);a=s.getFighters().get(0);b=s.getFighters().get(1);a.setX(reverse?920:360);b.setX(reverse?730:550);
            i=new FightingInput();i.setPlayerOneJump(true);if(reverse)i.setPlayerOneLeft(true);else i.setPlayerOneRight(true);
            for(int t=0;t<100;t++){s.update(i,DT);noOverlap(a,b);}check(reverse?a.getX()<b.getX():a.getX()>b.getX(),"Jump-over");
        }
        var s=session(base);var f=s.getFighters().get(0);var i=new FightingInput();i.setPlayerOneBlock(true);
        for(int t=0;t<15;t++)s.update(i,DT);
        double age=f.getShieldAnimationSeconds();s.pause();s.update(i,1);check(f.getShieldAnimationSeconds()==age,"Paused shield");
        s.resume();s.debugDefeatActiveFighter(0);check(s.getFighters().get(0)==f,"Evolved KO replaced immediately");
        var p=FightingSpriteProfile.forFighter(f);check(p.faintDuration()>=.5,"Reduced-package KO not held");
        s.pause();double koTime=f.getDefeatedAnimationSeconds();s.update(i,1);check(f.getDefeatedAnimationSeconds()==koTime,"Paused KO");
        s.resume();for(int t=0;t<120;t++)s.update(i,DT);
        check(s.getFighters().get(0)!=f && !s.getFighters().get(0).isEvolved(),"KO replacement form");
        noOverlap(s.getFighters().get(0),s.getFighters().get(1));
    }
    private static void ultimates()throws Exception{
        for(String ownerBase:BASE)for(String targetBase:BASE)for(boolean lethal:new boolean[]{false,true}){
            var owner=evolved(ownerBase);owner.setX(300);owner.addEvolutionEnergy(100);
            var target=evolved(targetBase);target.setX(650);target.setFacingLeft(true);if(lethal)target.receiveDamage(190);
            var targetId=target.getPokemonId();var pair=List.of(owner,target);var combat=new FightingCombatSystem();combat.beginUltimateRequests(pair,true,false);
            boolean damage=false,release=false;int initial=target.getHealth();
            for(int t=0;t<240;t++){
                owner.updateStatusEffects(DT);target.updateStatusEffects(DT);combat.update(pair,1280,List.of(),-1,-1,DT);
                check(target.getPokemonId().equals(targetId),"Ultimate changes target species");
                damage|=target.getHealth()<initial;
                for(var v:combat.getUltimateVisuals())if(v.phase()==UltimateController.Phase.RELEASING){
                    release=true;var p=FightingSpriteProfile.forFighter(owner);
                    check(belongs(p.sprites().get("Shoot"),RENDERER.spriteFrame(owner,p,null,v),owner.isFacingLeft()),"Owner release sheet");
                }
                if(t%6==0||target.isShowingDamage()){draw(owner,combat,pair);draw(target,combat,pair);}
            }
            check(damage&&release&&target.isDefeated()==lethal,"Ulti damage/KO");
            check(!owner.isEvolved()&&!owner.isEvolutionAnimating(),"Return to base");
            check(!target.isUltimateStunned(),"Stuck ultimate stun");
        }
        for(String base:BASE){
            var s=session(base);var target=s.getFighters().get(0);target.receiveDamage(190);
            var owner=s.getFighters().get(1);owner.addEvolutionEnergy(100);owner.evolve();owner.updateStatusEffects(1.1);owner.addEvolutionEnergy(100);
            var i=new FightingInput();i.setPlayerTwoUltimate(true);for(int t=0;t<240&&!target.isDefeated();t++)s.update(i,DT);
            check(target.isDefeated()&&s.getFighters().get(0)==target,"Ulti KO skips evolved defeat");
            for(int t=0;t<120;t++)s.update(i,DT);check(s.getFighters().get(0)!=target,"Ulti KO replacement stuck");
        }
        System.out.println("OK 32 evolved-receiver ultimate scenarios and four session KO replacements");
    }
    private static void pairsAndRematches()throws Exception{
        for(String first:BASE)for(String second:BASE){
            var s=new FightingGameSession(setup(first,second));s.debugFillActiveEnergy(0);s.debugFillActiveEnergy(1);
            var input=new FightingInput();input.setPlayerOneEvolution(true);input.setPlayerTwoEvolution(true);
            for(int t=0;t<66;t++)s.update(input,DT);
            var a=s.getFighters().get(0);var b=s.getFighters().get(1);a.setX(400);b.setX(700);
            input.clear();input.setPlayerOneRight(true);input.setPlayerTwoLeft(true);
            for(int t=0;t<150;t++){s.update(input,DT);noOverlap(a,b);}
            double ax=a.getX(),bx=b.getX();for(int t=0;t<60;t++)s.update(input,DT);
            check(Math.abs(a.getX()-ax)<1e-6&&Math.abs(b.getX()-bx)<1e-6,"Evolved contact drifts");
            s=new FightingGameSession(setup(first,second));a=s.getFighters().get(0);b=s.getFighters().get(1);
            a.setX(600);b.setX(600+a.getBodyHalfWidth()+b.getBodyHalfWidth());ax=a.getX();bx=b.getX();
            s.debugFillActiveEnergy(0);s.debugFillActiveEnergy(1);input=new FightingInput();
            input.setPlayerOneEvolution(true);input.setPlayerTwoEvolution(true);s.update(input,DT);noOverlap(a,b);
            check(Math.abs(a.getX()-ax)<=16&&Math.abs(b.getX()-bx)<=16,"Excessive resize correction");
        }
        for(String base:BASE){
            var s=new FightingGameSession(setup(base,"CHARMANDER"));s.debugFillActiveEnergy(0);s.debugFillActiveEnergy(1);
            var input=new FightingInput();input.setPlayerOneEvolution(true);input.setPlayerTwoEvolution(true);
            for(int t=0;t<66;t++)s.update(input,DT);
            var a=s.getFighters().get(0);var b=s.getFighters().get(1);a.setX(360);b.setX(550);
            input.clear();input.setPlayerOneRight(true);input.setPlayerOneJump(true);
            for(int t=0;t<100;t++){s.update(input,DT);noOverlap(a,b);}
            check(a.getX()>b.getX(),"Cannot jump over largest evolution");
            final String selected=base;
            SwingUtilities.invokeAndWait(()->{try{
                var panel=new FightingGamePanel(setup(selected,"SQUIRTLE"),()->{});
                var previous=(FightingGameSession)field(panel,"session");previous.debugFillActiveEnergy(0);
                var keys=new FightingInput();keys.setPlayerOneEvolution(true);previous.update(keys,DT);
                for(int i=0;i<3;i++){previous.debugDefeatActiveFighter(0);for(int t=0;t<120;t++)previous.update(new FightingInput(),DT);}
                check(previous.getState()==FightingGameSession.State.FINISHED,"Match did not finish");
                var method=FightingGamePanel.class.getDeclaredMethod("activateResultOption");method.setAccessible(true);method.invoke(panel);
                var fresh=(FightingGameSession)field(panel,"session");
                check(fresh!=previous&&fresh.getFighters().get(0).getPokemonId().equals(selected)
                        &&!FightingSpriteProfile.forFighter(fresh.getFighters().get(0)).isEvolution(),"Rematch retains evolved visuals");
                var g=CANVAS.createGraphics();RENDERER.draw(g,fresh,0,()->{});g.dispose();
            }catch(Exception e){throw new RuntimeException(e);}});
        }
        System.out.println("OK all 16 evolved pairs, contact resizing, jump over Charmeleon and four rematches");
    }
    private static void previews()throws Exception{
        var out=new BufferedImage(1760,1680,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();
        g.setColor(new Color(0x293746));g.fillRect(0,0,1760,1680);
        int row=0;
        for(String base:BASE)for(boolean left:new boolean[]{false,true}){
            for(int col=0;col<11;col++){
                var f=evolved(base);f.setFacingLeft(left);f.setX(col*160+75);f.setY(row*200+258);
                var p=FightingSpriteProfile.forFighter(f);FightingCombatSystem.AttackVisual v=null;
                UltimateController.Visual ultimate=null;String label;
                if(col==0)label="Idle";else if(col==1){label="Walk";f.updateMotionAnimation(Fighter.MotionAnimation.WALKING,0);f.updateMotionAnimation(Fighter.MotionAnimation.WALKING,.2);}
                else if(col==2){label="Hop";f.setOnGround(false);}
                else if(col==3){label="Block";f.setBlocking(true);f.updateMotionAnimation(Fighter.MotionAnimation.BLOCKING,0);f.updateShieldVisual(false,.2);}
                else if(col<8){var def=definition(base,col-4);label=def.moveId();v=new FightingCombatSystem.AttackVisual(def,"ACTIVE",.25,.25,false,1);}
                else if(col==8){label="Hurt";f.receiveDamage(1);}
                else if(col==9){label=p.defeatAnimation+" KO";f.markDefeated();f.advanceDefeatedAnimation(.3);}
                else{label="Ultimate";ultimate=new UltimateController.Visual(0,f,null,
                        UltimateDefinition.forEvolvedForm(f.getPokemonId()),UltimateController.Phase.RELEASING,
                        .3,.2,0,f.getX(),f.getY(),List.of());}
                g.setColor(Color.WHITE);g.drawString(p.species+" "+(left?"W":"E")+" "+label,col*160+4,row*200+100);
                g.setClip(col*160,row*200+105,160,175);
                if(col==3)drawShield.invoke(RENDERER,g,f,new FightingCombatSystem(),false);
                if(ultimate==null)drawSprite.invoke(RENDERER,g,f,p,v,f.getX());
                else {
                    var withUltimate=FightingRenderer.class.getDeclaredMethod("drawSpriteAnimation",Graphics2D.class,
                            Fighter.class,FightingSpriteProfile.class,FightingCombatSystem.AttackVisual.class,
                            double.class,UltimateController.Visual.class);withUltimate.setAccessible(true);
                    withUltimate.invoke(RENDERER,g,f,p,v,f.getX(),ultimate);
                }
                if(col==3)drawShield.invoke(RENDERER,g,f,new FightingCombatSystem(),true);
                g.setClip(null);
            }row++;
        }
        g.dispose();ImageIO.write(out,"png",new File("target/free-fight-evolution-preview.png"));
    }
    private static Object field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void key(JComponent panel,String stroke){
        var id=panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(stroke));check(id!=null,"Binding "+stroke);
        panel.getActionMap().get(id).actionPerformed(new ActionEvent(panel,ActionEvent.ACTION_PERFORMED,"check"));
    }
    private static void capture(JComponent component,String path)throws Exception{
        var image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();component.paint(g);g.dispose();ImageIO.write(image,"png",new File("target/"+path));
    }
    private static void awaitEdt(java.util.concurrent.Callable<Boolean> condition,String message)throws Exception{
        long deadline=System.nanoTime()+20_000_000_000L;
        while(System.nanoTime()<deadline){
            var probe=new java.util.concurrent.FutureTask<Boolean>(condition);
            SwingUtilities.invokeAndWait(probe);
            if(probe.get())return;
            Thread.sleep(40);
        }
        throw new AssertionError(message);
    }
    private static void window()throws Exception{
        final Throwable[] error={null};Thread.setDefaultUncaughtExceptionHandler((t,e)->{error[0]=e;e.printStackTrace();});
        MainGUI.main(new String[0]);SwingUtilities.invokeAndWait(()->{});
        final GameWindow[] window={null};final FightingGamePanel[] panel={null};final FightingGameSession[] session={null};
        for(int index=0;index<BASE.length;index++){
            final int idx=index;
            System.out.println("GUI scenario: "+EVOLVED[idx]);
            SwingUtilities.invokeAndWait(()->{try{
                if(error[0]!=null)return;
                if(window[0]==null)for(Window w:Window.getWindows())if(w instanceof GameWindow game&&game.isShowing())window[0]=game;
                check(window[0]!=null,"MainGUI did not start");
                var screens=(Map<?,?>)field(window[0],"screens");
                if(idx==0)key((JComponent)screens.get("intro"),"pressed ENTER");
                window[0].showScreen("menu");panel[0]=(FightingGamePanel)screens.get("fighting");
                var selection=(FreeFightSetup)field(panel[0],"setup");var chosen=setup(BASE[idx],BASE[(idx+1)%4]);
                selection.setPlayerOne(chosen.getPlayerOne(),chosen.getTeamOne());selection.setPlayerTwo(chosen.getPlayerTwo(),chosen.getTeamTwo());
                window[0].setGameSize(1280,720);window[0].showScreen("fighting");session[0]=(FightingGameSession)field(panel[0],"session");
                session[0].debugFillActiveEnergy(0);session[0].debugFillActiveEnergy(1);key(panel[0],"pressed R");key(panel[0],"pressed P");
            }catch(Throwable e){error[0]=e;}});
            awaitEdt(()->error[0]!=null || session[0].getFighters().stream()
                    .allMatch(f->f.isEvolved()&&!f.isEvolutionAnimating()),"Live transformation did not complete");
            SwingUtilities.invokeAndWait(()->{try{
                if(error[0]!=null)return;
                check(session[0].getFighters().get(0).getPokemonId().equals(EVOLVED[idx]),"Live evolution");
                var a=session[0].getFighters().get(0);var b=session[0].getFighters().get(1);a.setX(500);b.setX(500+a.getBodyHalfWidth()+b.getBodyHalfWidth());
                key(panel[0],"pressed S");key(panel[0],"pressed DOWN");
            }catch(Throwable e){error[0]=e;}});
            awaitEdt(()->error[0]!=null || session[0].getFighters().stream()
                    .allMatch(f->f.isBlocking()&&f.getShieldOpacity()>0.9),"Live guard did not activate");
            SwingUtilities.invokeAndWait(()->{try{
                if(error[0]!=null)return;
                check(session[0].getFighters().stream().allMatch(f->f.isBlocking()&&f.getShieldOpacity()>0),"Live evolved guard");
                capture(panel[0],"free-fight-evolved-live-"+EVOLVED[idx]+".png");key(panel[0],"released S");key(panel[0],"released DOWN");
            }catch(Throwable e){error[0]=e;}});
            Thread.sleep(180);
            SwingUtilities.invokeAndWait(()->{if(error[0]==null)key(panel[0],"pressed F");});
            awaitEdt(()->error[0]!=null || session[0].getFighters().get(1).getHealth()<200,"Live attack did not hit");
            SwingUtilities.invokeAndWait(()->{try{
                if(error[0]!=null)return;
                check(session[0].getFighters().get(1).getHealth()==191,"Live evolved Tackle damage");
                key(panel[0],"released F");key(panel[0],"pressed ESCAPE");check(session[0].getState()==FightingGameSession.State.PAUSED,"Live pause");key(panel[0],"pressed ESCAPE");
            }catch(Throwable e){error[0]=e;}});
            Thread.sleep(550);
            SwingUtilities.invokeAndWait(()->{if(error[0]==null){session[0].debugFillActiveEnergy(0);key(panel[0],"pressed T");}});
            awaitEdt(()->error[0]!=null || !session[0].getFighters().get(0).isEvolved(),"Live ultimate did not return to base");
            SwingUtilities.invokeAndWait(()->{try{
                if(error[0]!=null)return;check(!session[0].getFighters().get(0).isEvolved(),"Live ultimate return");
                noOverlap(session[0].getFighters().get(0),session[0].getFighters().get(1));
            }catch(Throwable e){error[0]=e;}});
            if(error[0]!=null)break;
        }
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});
        if(error[0]!=null)throw new AssertionError("Window check",error[0]);
        System.out.println("OK normal MainGUI, four live evolutions, evolved guards, Tackle, pause/resume, ultimates and return");
    }
    public static void main(String[] args)throws Exception{
        try{
            if(args.length>0&&args[0].equals("--window"))window();
            else{
                drawPokemon=FightingRenderer.class.getDeclaredMethod("drawPokemon",Graphics2D.class,Fighter.class,int.class,int.class,Runnable.class,int.class,FightingCombatSystem.class,List.class,boolean.class);drawPokemon.setAccessible(true);
                attackFor=FightingCombatSystem.class.getDeclaredMethod("attackFor",String.class,int.class);attackFor.setAccessible(true);
                attackHitbox=FightingCombatSystem.class.getDeclaredMethod("attackHitbox",Fighter.class,AttackDefinition.class);attackHitbox.setAccessible(true);
                drawSprite=FightingRenderer.class.getDeclaredMethod("drawSpriteAnimation",Graphics2D.class,Fighter.class,FightingSpriteProfile.class,FightingCombatSystem.AttackVisual.class,double.class);drawSprite.setAccessible(true);
                drawShield=FightingRenderer.class.getDeclaredMethod("drawBlockShield",Graphics2D.class,Fighter.class,FightingCombatSystem.class,boolean.class);drawShield.setAccessible(true);
                for(String base:BASE){states(base);attacks(base);collisionsAndKO(base);}
                ultimates();pairsAndRematches();previews();check(LOADER.gifs.isEmpty(),"GIF fallback requested "+LOADER.gifs);
            }
            System.out.println("PASS evolutions: "+checks+" checks");
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
        if(args.length>0)System.exit(0);
    }
}
