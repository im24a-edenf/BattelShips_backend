package com.example.springtemplate.schiffe.controller;

import com.example.springtemplate.schiffe.dto.CreateRoomResponse;
import com.example.springtemplate.schiffe.dto.JoinRoomRequest;
import com.example.springtemplate.schiffe.model.MultiplayerGame;
import com.example.springtemplate.schiffe.service.LobbyService;
import com.example.springtemplate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;

    // ── POST /api/lobby/create ───────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<?> createRoom() {
        try {
            User user = getCurrentUser();
            MultiplayerGame game = lobbyService.createRoom(user);
            return ResponseEntity.ok(new CreateRoomResponse(game.getId(), game.getRoomCode()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/lobby/join ─────────────────────────────────────────────────

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRoomRequest request) {
        User user = getCurrentUser();
        try {
            MultiplayerGame game = lobbyService.joinRoom(request.getRoomCode(), user);
            return ResponseEntity.ok(Map.of("gameId", game.getId()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
