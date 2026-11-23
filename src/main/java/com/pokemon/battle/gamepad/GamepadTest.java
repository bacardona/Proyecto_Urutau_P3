package com.pokemon.battle.gamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

public class GamepadTest {

    public static void main(String[] args) throws InterruptedException {
        ControllerManager controllers = new ControllerManager();
        controllers.initSDLGamepad();
        System.out.println("Conecten los mandos. Pulsen botones y muevan la palanca. BACK del mando 0 para salir.");

        boolean[] was = new boolean[2];
        while (true) {
            for (int i = 0; i < 2; i++) {
                ControllerState s = controllers.getState(i);
                if (s.isConnected != was[i]) {
                    was[i] = s.isConnected;
                    System.out.println("Mando " + i + (s.isConnected ? " CONECTADO. Tipo: " + s.controllerType : " DESCONECTADO"));
                }
                if (!s.isConnected) continue;
                String p = "[mando " + i + " " + s.controllerType + "] ";
                if (s.aJustPressed) System.out.println(p + "Boton A");
                if (s.bJustPressed) System.out.println(p + "Boton B");
                if (s.xJustPressed) System.out.println(p + "Boton X");
                if (s.yJustPressed) System.out.println(p + "Boton Y");
                if (s.lbJustPressed) System.out.println(p + "LB");
                if (s.rbJustPressed) System.out.println(p + "RB");
                if (s.leftTrigger > 0.5) System.out.println(p + "LT " + s.leftTrigger);
                if (s.rightTrigger > 0.5) System.out.println(p + "RT " + s.rightTrigger);
                if (s.startJustPressed) System.out.println(p + "START");
                if (s.backJustPressed) System.out.println(p + "BACK");
                if (Math.abs(s.leftStickY) > 0.7) System.out.println(p + "Palanca Y=" + s.leftStickY);
            }
            if (controllers.getState(0).backJustPressed) break;
            Thread.sleep(16);
        }
        controllers.quitSDLGamepad();
    }
}