package com.example.springtemplate.schiffe.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToGame(String gameId, Object event) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, event);
    }
}
