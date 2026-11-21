package com.pokemon.battle.gamepad;

public enum GameAction {
    UP, DOWN, LEFT, RIGHT,
    CONFIRM,      // A
    BACK,         // B
    REMOVE_LAST,  // X (solo perfil EQUIPO)
    PAGE_PREV,    // LB (solo LISTA y EQUIPO)
    PAGE_NEXT,    // RB (solo LISTA y EQUIPO)
    TOGGLE_VIEW,  // Y (solo LISTA y EQUIPO): barras / hexágono
    ANY_BUTTON,    // solo perfil CUALQUIER_BOTON
}