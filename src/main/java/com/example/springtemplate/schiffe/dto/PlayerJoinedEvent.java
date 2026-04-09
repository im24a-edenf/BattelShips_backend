package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class PlayerJoinedEvent {

    private final String type = "PLAYER_JOINED";
    private final String playerTwoEmail;
    private final String gameId;

    public PlayerJoinedEvent(String playerTwoEmail, String gameId) {
        this.playerTwoEmail = playerTwoEmail;
        this.gameId = gameId;
    }
}
