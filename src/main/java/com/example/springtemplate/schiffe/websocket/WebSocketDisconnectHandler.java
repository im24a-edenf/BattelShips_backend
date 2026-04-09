package com.example.springtemplate.schiffe.websocket;

import com.example.springtemplate.schiffe.dto.OpponentLeftEvent;
import com.example.springtemplate.schiffe.model.MultiplayerGame;
import com.example.springtemplate.schiffe.model.enums.GamePhase;
import com.example.springtemplate.schiffe.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketDisconnectHandler {

    // Maps sessionId → gameId, populated when a player subscribes to a game topic
    private final Map<String, String> sessionToGame = new ConcurrentHashMap<>();

    private final LobbyService lobbyService;
    private final WebSocketEventPublisher publisher;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination != null && sessionId != null && destination.startsWith("/topic/game/")) {
            String gameId = destination.substring("/topic/game/".length());
            sessionToGame.put(sessionId, gameId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String gameId = sessionToGame.remove(sessionId);
        if (gameId == null) return;

        MultiplayerGame game = lobbyService.getGameById(gameId);
        if (game == null || game.getPhase() == GamePhase.FINISHED) return;

        game.setPhase(GamePhase.FINISHED);
        publisher.publishToGame(gameId, new OpponentLeftEvent());
    }
}
