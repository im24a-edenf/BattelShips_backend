package com.example.springtemplate.schiffe.controller;

import com.example.springtemplate.schiffe.dto.*;
import com.example.springtemplate.schiffe.service.GameService;
import com.example.springtemplate.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    // ── POST /api/game/new ───────────────────────────────────────────────────

    @PostMapping("/new")
    public ResponseEntity<?> newGame(@RequestBody NewGameRequest request) {
        try {
            String gameId = gameService.createGame(request.getDifficulty());
            return ResponseEntity.ok(Map.of("gameId", gameId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/game/{id}/place ────────────────────────────────────────────

    @PostMapping("/{id}/place")
    public ResponseEntity<?> placeShips(
            @PathVariable String id,
            @RequestBody List<PlacementRequest> requests) {
        try {
            List<String> errors = gameService.placePlayerShips(id, requests);
            if (errors.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/game/{id}/fire ─────────────────────────────────────────────

    @PostMapping("/{id}/fire")
    public ResponseEntity<?> fire(
            @PathVariable String id,
            @RequestBody FireRequest request) {
        try {
            FireResponse response = gameService.fire(id, request.getX(), request.getY());
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/game/{id}/state ─────────────────────────────────────────────

    @GetMapping("/{id}/state")
    public ResponseEntity<?> getState(@PathVariable String id) {
        try {
            GameStateResponse state = gameService.getState(id);
            return ResponseEntity.ok(state);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/game/{id}/place/multi ──────────────────────────────────────

    @PostMapping("/{id}/place/multi")
    public ResponseEntity<?> placeShipsMultiplayer(
            @PathVariable String id,
            @RequestBody List<PlacementRequest> requests) {
        try {
            User currentUser = getCurrentUser();
            List<String> errors = gameService.placeShipsMultiplayer(id, requests, currentUser);
            if (errors.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "errors", errors));
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/game/{id}/fire/multi ───────────────────────────────────────

    @PostMapping("/{id}/fire/multi")
    public ResponseEntity<?> fireMultiplayer(
            @PathVariable String id,
            @RequestBody FireRequest request) {
        try {
            User currentUser = getCurrentUser();
            FireResponse response = gameService.fireMultiplayer(id, request.getX(), request.getY(), currentUser);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/game/{id}/state/multi ───────────────────────────────────────

    @GetMapping("/{id}/state/multi")
    public ResponseEntity<?> getStateMultiplayer(@PathVariable String id) {
        try {
            User currentUser = getCurrentUser();
            GameStateResponse state = gameService.getStateMultiplayer(id, currentUser);
            return ResponseEntity.ok(state);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
