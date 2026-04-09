package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class ShotFiredEvent {

    private final String type = "SHOT_FIRED";
    private final String firingPlayerEmail;
    private final String firingRole;
    private final ShotResultDTO shot;
    private final String nextTurnEmail;
    private final String nextTurnRole;
    private final String winner;
    private final boolean gameOver;

    public ShotFiredEvent(String firingPlayerEmail, String firingRole, ShotResultDTO shot,
                          String nextTurnEmail, String nextTurnRole, String winner, boolean gameOver) {
        this.firingPlayerEmail = firingPlayerEmail;
        this.firingRole = firingRole;
        this.shot = shot;
        this.nextTurnEmail = nextTurnEmail;
        this.nextTurnRole = nextTurnRole;
        this.winner = winner;
        this.gameOver = gameOver;
    }
}
