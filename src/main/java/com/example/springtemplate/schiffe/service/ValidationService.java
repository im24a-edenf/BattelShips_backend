package com.example.springtemplate.schiffe.service;

import com.example.springtemplate.schiffe.dto.PlacementRequest;
import com.example.springtemplate.schiffe.model.Board;
import com.example.springtemplate.schiffe.model.Ship;
import com.example.springtemplate.schiffe.model.enums.ShipType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    // How many of each ship type the player must place — standard fleet
    private static final Map<ShipType, Integer> REQUIRED_FLEET = Map.of(
            ShipType.SCHLACHTSCHIFF, 1,
            ShipType.KREUZER,        2,
            ShipType.ZERSTOERER,     3,
            ShipType.UBOOT,          4
    );

    /**
     * Validates a full list of placement requests before anything is placed.
     * Returns a list of error messages — empty list means all good.
     */
    public List<String> validatePlacements(List<PlacementRequest> requests) {
        List<String> errors = new ArrayList<>();

        errors.addAll(validateFleetComposition(requests));
        if (!errors.isEmpty()) {
            // No point checking geometry if the fleet is wrong
            return errors;
        }

        // Build a temporary boolean grid to check overlaps and adjacency
        // across all ships before committing anything
        boolean[][] occupied = new boolean[Board.SIZE][Board.SIZE];

        for (PlacementRequest req : requests) {
            ShipType type = req.getShipType();
            int x = req.getX();
            int y = req.getY();
            boolean horizontal = req.isHorizontal();

            List<int[]> cells = computeCells(type, x, y, horizontal);

            errors.addAll(checkBounds(type, cells));
            if (!errors.isEmpty()) continue; // skip further checks for this ship if out of bounds

            errors.addAll(checkOverlap(type, cells, occupied));
            errors.addAll(checkAdjacency(type, cells, occupied));

            // Mark cells as occupied for subsequent ships
            for (int[] cell : cells) {
                occupied[cell[0]][cell[1]] = true;
            }
        }

        return errors;
    }

    /**
     * Validates a single ship against an already-populated board.
     * Used when placing ships one at a time (e.g. re-placement).
     */
    public List<String> validateSinglePlacement(PlacementRequest req, Board board) {
        List<String> errors = new ArrayList<>();

        ShipType type = req.getShipType();
        List<int[]> cells = computeCells(type, req.getX(), req.getY(), req.isHorizontal());

        errors.addAll(checkBounds(type, cells));
        if (!errors.isEmpty()) return errors;

        // Build occupied grid from existing board ships
        boolean[][] occupied = buildOccupiedGrid(board);

        errors.addAll(checkOverlap(type, cells, occupied));
        errors.addAll(checkAdjacency(type, cells, occupied));

        return errors;
    }

    // ── Fleet composition ────────────────────────────────────────────────────

    private List<String> validateFleetComposition(List<PlacementRequest> requests) {
        List<String> errors = new ArrayList<>();

        Map<ShipType, Long> counts = requests.stream()
                .collect(Collectors.groupingBy(PlacementRequest::getShipType, Collectors.counting()));

        for (Map.Entry<ShipType, Integer> entry : REQUIRED_FLEET.entrySet()) {
            ShipType type = entry.getKey();
            int required = entry.getValue();
            long provided = counts.getOrDefault(type, 0L);

            if (provided != required) {
                errors.add(String.format(
                        "Falsche Anzahl %s: %d erwartet, %d angegeben.",
                        type.getDisplayName(), required, provided
                ));
            }
        }

        // Check for unexpected ship types
        for (ShipType type : counts.keySet()) {
            if (!REQUIRED_FLEET.containsKey(type)) {
                errors.add("Unbekannter Schiffstyp: " + type);
            }
        }

        return errors;
    }

    // ── Geometry checks ──────────────────────────────────────────────────────

    private List<String> checkBounds(ShipType type, List<int[]> cells) {
        List<String> errors = new ArrayList<>();
        for (int[] cell : cells) {
            if (cell[0] < 0 || cell[0] >= Board.SIZE || cell[1] < 0 || cell[1] >= Board.SIZE) {
                errors.add(String.format(
                        "%s liegt außerhalb des Spielfelds (Zelle %d,%d).",
                        type.getDisplayName(), cell[0], cell[1]
                ));
                break; // one error per ship is enough
            }
        }
        return errors;
    }

    private List<String> checkOverlap(ShipType type, List<int[]> cells, boolean[][] occupied) {
        List<String> errors = new ArrayList<>();
        for (int[] cell : cells) {
            if (occupied[cell[0]][cell[1]]) {
                errors.add(String.format(
                        "%s überschneidet sich mit einem anderen Schiff (Zelle %d,%d).",
                        type.getDisplayName(), cell[0], cell[1]
                ));
                break;
            }
        }
        return errors;
    }

    /**
     * No ship may touch another — not even diagonally.
     * We check all 8 neighbours of each cell the new ship occupies.
     */
    private List<String> checkAdjacency(ShipType type, List<int[]> cells, boolean[][] occupied) {
        List<String> errors = new ArrayList<>();

        for (int[] cell : cells) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue; // the cell itself

                    int nx = cell[0] + dx;
                    int ny = cell[1] + dy;

                    if (nx >= 0 && nx < Board.SIZE && ny >= 0 && ny < Board.SIZE) {
                        if (occupied[nx][ny]) {
                            errors.add(String.format(
                                    "%s berührt ein anderes Schiff (Zelle %d,%d).",
                                    type.getDisplayName(), cell[0], cell[1]
                            ));
                            return errors; // one adjacency error per ship is enough
                        }
                    }
                }
            }
        }
        return errors;
    }

    // ── Firing validation ────────────────────────────────────────────────────

    /**
     * Checks whether a shot at (x, y) is legal.
     * Returns an error string, or null if the shot is valid.
     */
    public String validateShot(int x, int y, Board targetBoard) {
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE) {
            return "Schuss außerhalb des Spielfelds (" + x + "," + y + ").";
        }
        if (targetBoard.cellAlreadyShot(x, y)) {
            return "Auf diese Zelle wurde bereits geschossen (" + x + "," + y + ").";
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Computes the list of [x,y] cells a ship would occupy given its placement.
     */
    public List<int[]> computeCells(ShipType type, int startX, int startY, boolean horizontal) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 0; i < type.getSize(); i++) {
            int x = horizontal ? startX + i : startX;
            int y = horizontal ? startY : startY + i;
            cells.add(new int[]{x, y});
        }
        return cells;
    }

    private boolean[][] buildOccupiedGrid(Board board) {
        boolean[][] occupied = new boolean[Board.SIZE][Board.SIZE];
        for (Ship ship : board.getShips()) {
            for (int[] coord : ship.getCoordinates()) {
                occupied[coord[0]][coord[1]] = true;
            }
        }
        return occupied;
    }
}