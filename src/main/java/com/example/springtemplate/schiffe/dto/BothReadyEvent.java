package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class BothReadyEvent {

    private final String type = "BOTH_READY";
    private final String firstTurnEmail;
    private final String gameId;

    public BothReadyEvent(String firstTurnEmail, String gameId) {
        this.firstTurnEmail = firstTurnEmail;
        this.gameId = gameId;
    }
}
