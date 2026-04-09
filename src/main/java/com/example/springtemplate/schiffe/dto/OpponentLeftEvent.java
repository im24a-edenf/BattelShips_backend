package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class OpponentLeftEvent {

    private final String type = "OPPONENT_LEFT";
    private final String message = "Dein Gegner hat das Spiel verlassen.";
}
