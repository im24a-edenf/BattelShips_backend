package com.example.springtemplate.schiffe.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRoomRequest {

    private String roomCode;

    public JoinRoomRequest() {}
}
