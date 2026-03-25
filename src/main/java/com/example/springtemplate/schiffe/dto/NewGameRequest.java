package com.example.springtemplate.schiffe.dto;


import com.example.springtemplate.schiffe.model.enums.Difficulty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NewGameRequest {

    private Difficulty difficulty;

    public NewGameRequest() {}

}