package com.example.springtemplate.schiffe.service;

import com.example.springtemplate.schiffe.dto.PlayerJoinedEvent;
import com.example.springtemplate.schiffe.model.MultiplayerGame;
import com.example.springtemplate.schiffe.model.enums.GamePhase;
import com.example.springtemplate.schiffe.websocket.WebSocketEventPublisher;
import com.example.springtemplate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LobbyService {

    // keyed by roomCode (uppercase)
    private final Map<String, MultiplayerGame> rooms = new ConcurrentHashMap<>();
    // keyed by gameId
    private final Map<String, MultiplayerGame> gamesById = new ConcurrentHashMap<>();

    private final WebSocketEventPublisher publisher;
    private final Random random = new Random();

    private static final String ROOM_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public MultiplayerGame createRoom(User creator) {
        String roomCode = generateUniqueRoomCode();
        MultiplayerGame game = new MultiplayerGame(roomCode, creator.getId(), creator.getUsername());
        rooms.put(roomCode, game);
        gamesById.put(game.getId(), game);
        return game;
    }

    public MultiplayerGame joinRoom(String roomCode, User joiner) {
        MultiplayerGame game = rooms.get(roomCode.toUpperCase());
        if (game == null) {
            throw new NoSuchElementException("Raum nicht gefunden: " + roomCode);
        }
        if (game.isBothJoined()) {
            throw new IllegalStateException("Raum ist bereits voll.");
        }
        if (game.getPlayerOneId().equals(joiner.getId())) {
            throw new IllegalStateException("Du kannst nicht deinem eigenen Raum beitreten.");
        }
        game.setPlayerTwoId(joiner.getId());
        game.setPlayerTwoEmail(joiner.getUsername());
        game.setPhase(GamePhase.PLACEMENT);
        publisher.publishToGame(game.getId(), new PlayerJoinedEvent(joiner.getUsername(), game.getId()));
        return game;
    }

    public MultiplayerGame getGameById(String gameId) {
        return gamesById.get(gameId);
    }

    public MultiplayerGame getGameForPlayer(String gameId, UUID playerId) {
        MultiplayerGame game = gamesById.get(gameId);
        if (game == null) {
            throw new NoSuchElementException("Spiel nicht gefunden: " + gameId);
        }
        if (!playerId.equals(game.getPlayerOneId()) && !playerId.equals(game.getPlayerTwoId())) {
            throw new SecurityException("Zugriff verweigert: Du bist nicht Teil dieses Spiels.");
        }
        return game;
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(ROOM_CODE_CHARS.charAt(random.nextInt(ROOM_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }
}
