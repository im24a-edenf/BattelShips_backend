package com.example.springtemplate.schiffe.model;


import com.example.springtemplate.schiffe.model.enums.CellState;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Cell {

    private final int x; // column 0–9
    private final int y; // row 0–9
    @Setter
    private CellState state;
    @Setter
    private Ship ship; // reference to ship occupying this cell, null if none

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        this.state = CellState.EMPTY;
        this.ship = null;
    }

    public boolean isEmpty() { return state == CellState.EMPTY; }
    public boolean hasShip() { return ship != null; }
    public boolean isShot() {
        return state == CellState.HIT || state == CellState.MISS || state == CellState.SUNK;
    }
}