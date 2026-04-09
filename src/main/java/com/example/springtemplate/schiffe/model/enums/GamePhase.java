package com.example.springtemplate.schiffe.model.enums;

public enum GamePhase {
    WAITING_FOR_PLAYER,    // multiplayer: room created, waiting for second player
    WAITING_FOR_PLACEMENT, // multiplayer: one player has placed, other has not yet
    PLACEMENT,
    BATTLE,
    FINISHED
}
