package com.example.springtemplate.schiffe.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FireRequest {

    private int x; // column 0–9
    private int y; // row 0–9

    public FireRequest() {}

}