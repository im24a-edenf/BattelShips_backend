package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class GameOverEvent {

    private final String type = "GAME_OVER";
    private final String winnerEmail;
    private final String winnerRole;

    public GameOverEvent(String winnerEmail, String winnerRole) {
        this.winnerEmail = winnerEmail;
        this.winnerRole = winnerRole;
    }
}
