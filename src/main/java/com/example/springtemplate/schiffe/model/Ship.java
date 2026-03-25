package com.example.springtemplate.schiffe.model;



import com.example.springtemplate.schiffe.model.enums.ShipType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Ship {

    private final ShipType type;
    private final List<int[]> coordinates; // each entry is [x, y]
    private final boolean horizontal;
    private int hits;
    private boolean sunk;

    public Ship(ShipType type, int startX, int startY, boolean horizontal) {
        this.type = type;
        this.horizontal = horizontal;
        this.hits = 0;
        this.sunk = false;
        this.coordinates = new ArrayList<>();

        // Build the list of cells this ship occupies
        for (int i = 0; i < type.getSize(); i++) {
            int x = horizontal ? startX + i : startX;
            int y = horizontal ? startY : startY + i;
            coordinates.add(new int[]{x, y});
        }
    }

    /**
     * Register a hit on this ship. Returns true if the ship is now sunk.
     */
    public boolean registerHit() {
        hits++;
        if (hits >= type.getSize()) {
            sunk = true;
        }
        return sunk;
    }

    public int getStartX() { return coordinates.getFirst()[0]; }
    public int getStartY() { return coordinates.getFirst()[1]; }
}