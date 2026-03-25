package com.example.springtemplate.schiffe.model;


import com.example.springtemplate.schiffe.model.enums.ShipType;
import com.example.springtemplate.schiffe.model.enums.ShotResult;
import lombok.Getter;

@Getter
public class Shot {

    private final int x;
    private final int y;
    private final ShotResult result;
    private final ShipType shipType; // non-null only if result is HIT or SUNK

    public Shot(int x, int y, ShotResult result, ShipType shipType) {
        this.x = x;
        this.y = y;
        this.result = result;
        this.shipType = shipType;
    }

    // Convenience constructors
    public static Shot miss(int x, int y) {
        return new Shot(x, y, ShotResult.MISS, null);
    }

    public static Shot hit(int x, int y, ShipType shipType) {
        return new Shot(x, y, ShotResult.HIT, shipType);
    }

    public static Shot sunk(int x, int y, ShipType shipType) {
        return new Shot(x, y, ShotResult.SUNK, shipType);
    }

}