package com.example.springtemplate.schiffe.dto;

import lombok.Getter;

@Getter
public class CreateRoomResponse {

    private final String gameId;
    private final String roomCode;

    public CreateRoomResponse(String gameId, String roomCode) {
        this.gameId = gameId;
        this.roomCode = roomCode;
    }
}
