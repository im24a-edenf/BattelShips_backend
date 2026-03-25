package com.example.springtemplate.schiffe.dto;


import com.example.springtemplate.schiffe.model.enums.ShipType;
import com.example.springtemplate.schiffe.model.enums.ShotResult;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class ShotResultDTO {

    private int x;
    private int y;
    private ShotResult result;
    private ShipType shipType; // null on MISS

    public ShotResultDTO() {}

    public ShotResultDTO(int x, int y, ShotResult result, ShipType shipType) {
        this.x = x;
        this.y = y;
        this.result = result;
        this.shipType = shipType;
    }

}