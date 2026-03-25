package com.example.springtemplate.schiffe.model;



import com.example.springtemplate.schiffe.model.enums.CellState;

import java.util.ArrayList;
import java.util.List;

public class Board {

    public static final int SIZE = 10;

    private final Cell[][] grid;
    private final List<Ship> ships;

    public Board() {
        this.grid = new Cell[SIZE][SIZE];
        this.ships = new ArrayList<>();

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                grid[x][y] = new Cell(x, y);
            }
        }
    }

    // ── Grid access ──────────────────────────────────────────────────────────

    public Cell getCell(int x, int y) {
        return grid[x][y];
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    // ── Ship placement ───────────────────────────────────────────────────────

    /**
     * Places the ship on the board, marking all its cells as SHIP.
     * Assumes validation has already passed.
     */
    public void placeShip(Ship ship) {
        ships.add(ship);
        for (int[] coord : ship.getCoordinates()) {
            Cell cell = grid[coord[0]][coord[1]];
            cell.setState(CellState.SHIP);
            cell.setShip(ship);
        }
    }

    // ── Shot handling ────────────────────────────────────────────────────────

    /**
     * Records a shot at (x, y).
     * Returns the cell so the caller can inspect what happened.
     */
    public Cell receiveShot(int x, int y) {
        Cell cell = grid[x][y];

        if (cell.hasShip()) {
            Ship ship = cell.getShip();
            boolean nowSunk = ship.registerHit();

            if (nowSunk) {
                // Mark every cell of the sunk ship as SUNK
                for (int[] coord : ship.getCoordinates()) {
                    grid[coord[0]][coord[1]].setState(CellState.SUNK);
                }
            } else {
                cell.setState(CellState.HIT);
            }
        } else {
            cell.setState(CellState.MISS);
        }

        return cell;
    }

    // ── Status queries ───────────────────────────────────────────────────────

    public List<Ship> getShips() { return ships; }

    public boolean allShipsSunk() {
        return ships.stream().allMatch(Ship::isSunk);
    }

    public boolean cellAlreadyShot(int x, int y) {
        return grid[x][y].isShot();
    }

    /**
     * Returns a 2D array of CellState, safe to serialize.
     * Ships not yet hit appear as EMPTY (fog of war for opponent's board).
     */
    public CellState[][] getFogOfWarView() {
        CellState[][] view = new CellState[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                CellState state = grid[x][y].getState();
                view[x][y] = (state == CellState.SHIP) ? CellState.EMPTY : state;
            }
        }
        return view;
    }

    /**
     * Returns the full grid state (for the owner's own view).
     */
    public CellState[][] getFullView() {
        CellState[][] view = new CellState[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                view[x][y] = grid[x][y].getState();
            }
        }
        return view;
    }
}