package com.example.springtemplate.schiffe.dto;


import com.example.springtemplate.schiffe.model.enums.ShipType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlacementRequest {

    private ShipType shipType;
    private int x;          // top-left column (0–9)
    private int y;          // top-left row (0–9)
    private boolean horizontal;

    public PlacementRequest() {}

}