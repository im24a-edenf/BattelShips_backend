package com.example.springtemplate.socket;

import com.example.springtemplate.security.jwt.JwtAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String SESSION_JWT_ATTRIBUTE = "stomp.jwt";

    private final JwtAuthenticationService jwtAuthenticationService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            throw new AccessDeniedException("Missing STOMP headers");
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("Missing Authorization header");
            }

            String jwt = authHeader.substring(7);
            UsernamePasswordAuthenticationToken authentication = jwtAuthenticationService.authenticate(jwt);

            accessor.setUser(authentication);

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(SESSION_JWT_ATTRIBUTE, jwt);
            }
        }

        if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null || !(sessionAttributes.get(SESSION_JWT_ATTRIBUTE) instanceof String jwt)) {
                throw new AccessDeniedException("Missing STOMP session token");
            }

            jwtAuthenticationService.revalidate(jwt, accessor.getUser());
        }

        return message;
    }
}