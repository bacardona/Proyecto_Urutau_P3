package com.pokemon.battle.gamepad;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

/**
 * Prueba aislada del mando: no usa el juego. Imprime el mando detectado y cada boton
 * que se pulsa, para saber como lo reporta la libreria. START para salir.
 */
public class GamepadTest {

    public static void main(String[] args) throws InterruptedException {
        ControllerManager controllers;
        try {
            controllers = new ControllerManager();
            controllers.initSDLGamepad();
        } catch (Throwable t) {
            System.out.println("NO SE PUDO INICIAR LA LIBRERIA DE MANDOS:");
            t.printStackTrace();
            return;
        }

        System.out.println("Mandos detectados: " + controllers.getNumControllers());
        System.out.println("Pulsa botones, mueve el stick y la cruceta. START para salir.");

        boolean wasConnected = false;
        while (true) {
            ControllerState s = controllers.getState(0);
            if (!s.isConnected) {
                if (wasConnected) {
                    System.out.println("Mando desconectado");
                }
                wasConnected = false;
                Thread.sleep(300);
                continue;
            }
            if (!wasConnected) {
                System.out.println("Mando conectado. Tipo: " + s.controllerType);
                wasConnected = true;
            }
            if (s.startJustPressed) {
                break;
            }
            report("A", s.aJustPressed);
            report("B", s.bJustPressed);
            report("X", s.xJustPressed);
            report("Y", s.yJustPressed);
            report("LB", s.lbJustPressed);
            report("RB", s.rbJustPressed);
            report("BACK", s.backJustPressed);
            report("DPAD ARRIBA", s.dpadUpJustPressed);
            report("DPAD ABAJO", s.dpadDownJustPressed);
            report("DPAD IZQUIERDA", s.dpadLeftJustPressed);
            report("DPAD DERECHA", s.dpadRightJustPressed);
            if (Math.abs(s.leftStickX) > 0.7 || Math.abs(s.leftStickY) > 0.7) {
                System.out.println("STICK X=" + s.leftStickX + " Y=" + s.leftStickY);
                Thread.sleep(200);
            }
            Thread.sleep(16);
        }
        controllers.quitSDLGamepad();
        System.out.println("Fin de la prueba");
    }

    private static void report(String name, boolean pressed) {
        if (pressed) {
            System.out.println("Boton: " + name);
        }
    }
}