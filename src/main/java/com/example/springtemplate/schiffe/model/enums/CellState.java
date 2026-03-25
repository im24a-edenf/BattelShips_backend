package com.example.springtemplate.schiffe.model.enums;

public enum CellState {
    EMPTY,   // nothing here
    SHIP,    // ship placed, not yet hit
    HIT,     // ship cell that was hit
    MISS,    // shot that hit water
    SUNK     // part of a fully sunk ship
}