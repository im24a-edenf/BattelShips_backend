package com.example.springtemplate.schiffe.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FireResponse {

    private ShotResultDTO playerShot;  // result of the human's shot
    private ShotResultDTO botShot;     // result of the bot's automatic return shot
    private String winner;             // "PLAYER", "BOT", or null
    private String gamePhase;          // current phase after the round

    public FireResponse() {}

}