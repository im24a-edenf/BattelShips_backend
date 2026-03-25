package com.example.springtemplate.schiffe.service;

import com.example.springtemplate.schiffe.model.Board;
import com.example.springtemplate.schiffe.model.enums.CellState;
import com.example.springtemplate.schiffe.model.enums.Difficulty;
import com.example.springtemplate.schiffe.model.enums.ShotResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BotService {

    // Per-game hunt/target state — keyed by gameId
    // We store this here so the bot remembers its state between turns
    private final Map<String, BotState> botStates = new HashMap<>();

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Initialises bot state when a new game starts.
     */
    public void initGame(String gameId) {
        botStates.put(gameId, new BotState());
    }

    /**
     * Cleans up bot state when a game ends.
     */
    public void removeGame(String gameId) {
        botStates.remove(gameId);
    }

    /**
     * Picks the bot's next shot coordinates based on difficulty.
     * Returns int[]{x, y}.
     */
    public int[] pickShot(String gameId, Board playerBoard, Difficulty difficulty) {
        if (difficulty == Difficulty.EASY) {
            return pickRandomShot(playerBoard);
        } else {
            return pickSmartShot(gameId, playerBoard);
        }
    }

    /**
     * Must be called after the bot's shot is resolved so the AI can
     * update its hunt/target state based on whether it hit or missed.
     */
    public void reportShotResult(String gameId, int x, int y, ShotResult result) {
        BotState state = botStates.get(gameId);
        if (state == null) return;

        if (result == ShotResult.SUNK) {
            // Ship is fully sunk — clear hunt state and go back to random search
            state.clearHunt();
        } else if (result == ShotResult.HIT) {
            state.registerHit(x, y);
        }
        // MISS — nothing to update in hunt/target mode
    }

    // ── EASY: random ─────────────────────────────────────────────────────────

    private int[] pickRandomShot(Board board) {
        List<int[]> available = getAvailableCells(board);
        if (available.isEmpty()) return new int[]{0, 0}; // fallback, should never happen
        return available.get(new Random().nextInt(available.size()));
    }

    // ── HARD: hunt / target ───────────────────────────────────────────────────
    //
    // Phases:
    //   HUNT  — no known hits; shoot random cells (checkerboard pattern for efficiency)
    //   TARGET — we have at least one hit; systematically attack adjacent cells
    //            once we know the direction, we continue along that axis

    private int[] pickSmartShot(String gameId, Board board) {
        BotState state = botStates.get(gameId);

        if (state.isHunting()) {
            return pickCheckerboardShot(board);
        } else {
            return pickTargetShot(state, board);
        }
    }

    /**
     * HUNT phase: shoot only "even" cells (checkerboard) so we never waste
     * shots on cells a size-2+ ship couldn't possibly occupy alone.
     * Falls back to any unshot cell if checkerboard is exhausted.
     */
    private int[] pickCheckerboardShot(Board board) {
        List<int[]> checkerboard = new ArrayList<>();
        List<int[]> fallback = new ArrayList<>();

        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!board.cellAlreadyShot(x, y)) {
                    if ((x + y) % 2 == 0) {
                        checkerboard.add(new int[]{x, y});
                    } else {
                        fallback.add(new int[]{x, y});
                    }
                }
            }
        }

        List<int[]> pool = checkerboard.isEmpty() ? fallback : checkerboard;
        return pool.get(new Random().nextInt(pool.size()));
    }

    /**
     * TARGET phase: we have one or more hits on a ship we haven't sunk yet.
     *
     * Strategy:
     * 1. If we have 2+ hits, we know the direction — continue along that axis.
     * 2. If we have only 1 hit, try the 4 orthogonal neighbours in order.
     * 3. If a direction is blocked (edge / already shot / miss), reverse direction.
     */
    private int[] pickTargetShot(BotState state, Board board) {
        List<int[]> hits = state.getCurrentHits();

        if (hits.size() >= 2) {
            // Direction is known
            return continueAlongAxis(hits, board);
        } else {
            // Only one hit — probe neighbours
            return probeNeighbours(hits.get(0), state, board);
        }
    }

    private int[] continueAlongAxis(List<int[]> hits, Board board) {
        // Sort hits to find the two ends of the known streak
        boolean horizontal = hits.get(0)[1] == hits.get(1)[1]; // same Y → horizontal

        List<int[]> sorted = new ArrayList<>(hits);
        sorted.sort(horizontal
                ? Comparator.comparingInt(c -> c[0])
                : Comparator.comparingInt(c -> c[1]));

        int[] front = sorted.get(sorted.size() - 1); // highest index end
        int[] back  = sorted.get(0);                 // lowest index end

        // Try extending forward first, then backward
        int[] forwardCell  = horizontal
                ? new int[]{front[0] + 1, front[1]}
                : new int[]{front[0], front[1] + 1};

        int[] backwardCell = horizontal
                ? new int[]{back[0] - 1, back[1]}
                : new int[]{back[0], back[1] - 1};

        if (isShotable(forwardCell, board))  return forwardCell;
        if (isShotable(backwardCell, board)) return backwardCell;

        // Both ends blocked — fall back to random (shouldn't normally happen)
        return pickCheckerboardShot(board);
    }

    private int[] probeNeighbours(int[] hit, BotState state, Board board) {
        // Try neighbours in a fixed priority: right, left, down, up
        // Skip directions the bot already tried and got a miss
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] dir : directions) {
            int[] candidate = {hit[0] + dir[0], hit[1] + dir[1]};
            if (isShotable(candidate, board) && !state.isFailedDirection(dir)) {
                state.setLastProbeDirection(dir);
                return candidate;
            }
        }

        // All neighbours blocked — give up and hunt randomly
        state.clearHunt();
        return pickCheckerboardShot(board);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isShotable(int[] cell, Board board) {
        int x = cell[0], y = cell[1];
        return x >= 0 && x < Board.SIZE
                && y >= 0 && y < Board.SIZE
                && !board.cellAlreadyShot(x, y);
    }

    private List<int[]> getAvailableCells(Board board) {
        List<int[]> cells = new ArrayList<>();
        for (int x = 0; x < Board.SIZE; x++) {
            for (int y = 0; y < Board.SIZE; y++) {
                if (!board.cellAlreadyShot(x, y)) {
                    cells.add(new int[]{x, y});
                }
            }
        }
        return cells;
    }

    // ── BotState inner class ─────────────────────────────────────────────────

    /**
     * Holds the hunt/target memory for one game instance.
     */
    private static class BotState {

        // Cells we've hit that belong to the current target ship (not yet sunk)
        private final List<int[]> currentHits = new ArrayList<>();

        // Directions we probed from the first hit and got a miss back
        private final List<int[]> failedDirections = new ArrayList<>();

        // The direction of the last probe attempt
        private int[] lastProbeDirection = null;

        boolean isHunting() {
            return currentHits.isEmpty();
        }

        void registerHit(int x, int y) {
            currentHits.add(new int[]{x, y});
        }

        void clearHunt() {
            currentHits.clear();
            failedDirections.clear();
            lastProbeDirection = null;
        }

        List<int[]> getCurrentHits() {
            return currentHits;
        }

        void setLastProbeDirection(int[] dir) {
            this.lastProbeDirection = dir;
        }

        /**
         * If the last probe in a direction missed, remember that direction as failed.
         * Called externally when a MISS is reported after a single-hit probe.
         */
        void markLastDirectionFailed() {
            if (lastProbeDirection != null) {
                failedDirections.add(lastProbeDirection);
                lastProbeDirection = null;
            }
        }

        boolean isFailedDirection(int[] dir) {
            for (int[] failed : failedDirections) {
                if (failed[0] == dir[0] && failed[1] == dir[1]) return true;
            }
            return false;
        }
    }
}