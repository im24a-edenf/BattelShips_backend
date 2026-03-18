package com.example.springtemplate.chat;

import com.example.springtemplate.user.User;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RestController
public class MessageHandlingController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessageOutput send(@Valid @Payload ChatMessageInput message, Principal principal) {
        User user = extractAuthenticatedUser(principal);
        String time = LocalTime.now().format(TIME_FORMATTER);

        ChatSenderOutput sender = new ChatSenderOutput(
                user.getId(),
                user.getEmail()
        );

        return new ChatMessageOutput(sender, message.getMessage().trim(), time);
    }

    private User extractAuthenticatedUser(Principal principal) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated message sender");
        }

        boolean hasWritePrivilege = authentication.getAuthorities().stream()
                .anyMatch(authority -> "WRITE_PRIVILEGE".equals(authority.getAuthority()));
        if (!hasWritePrivilege) {
            throw new AccessDeniedException("Missing chat write permission");
        }

        Object authenticatedPrincipal = authentication.getPrincipal();
        if (!(authenticatedPrincipal instanceof User user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}