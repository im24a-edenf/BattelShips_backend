package com.example.springtemplate.socket;

import com.example.springtemplate.security.jwt.JwtAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtStompChannelInterceptorTests {

    @Mock
    private JwtAuthenticationService jwtAuthenticationService;

    private JwtStompChannelInterceptor interceptor;
    private MessageChannel messageChannel;

    @BeforeEach
    void setUp() {
        interceptor = new JwtStompChannelInterceptor(jwtAuthenticationService);
        messageChannel = mock(MessageChannel.class);
    }

    @Test
    void connectWithoutBearerHeaderIsRejected() {
        Message<byte[]> message = createMessage(StompCommand.CONNECT, accessor -> {
        });

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Missing Authorization header");
    }

    @Test
    void connectWithBearerHeaderAuthenticatesAndStoresSessionToken() {
        String jwt = "test-jwt";
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com",
                null,
                List.of()
        );
        when(jwtAuthenticationService.authenticate(jwt)).thenReturn(authentication);

        Message<byte[]> message = createMessage(StompCommand.CONNECT, accessor ->
                accessor.addNativeHeader("Authorization", "Bearer " + jwt));

        Message<?> intercepted = interceptor.preSend(message, messageChannel);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(intercepted);

        assertThat(accessor.getUser()).isEqualTo(authentication);
        assertThat(accessor.getSessionAttributes()).containsEntry("stomp.jwt", jwt);
        verify(jwtAuthenticationService).authenticate(jwt);
    }

    @Test
    void sendWithoutStoredJwtIsRejected() {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com",
                null,
                List.of()
        );
        Message<byte[]> message = createMessage(StompCommand.SEND, accessor -> accessor.setUser(authentication));

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Missing STOMP session token");
    }

    @Test
    void sendWithStoredJwtRevalidatesSession() {
        String jwt = "test-jwt";
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "user@example.com",
                null,
                List.of()
        );
        Message<byte[]> message = createMessage(StompCommand.SEND, accessor -> {
            accessor.setUser(authentication);
            accessor.getSessionAttributes().put("stomp.jwt", jwt);
        });

        interceptor.preSend(message, messageChannel);

        verify(jwtAuthenticationService).revalidate(jwt, authentication);
    }

    private Message<byte[]> createMessage(StompCommand command, Consumer<StompHeaderAccessor> customizer) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(new HashMap<>());
        accessor.setLeaveMutable(true);
        customizer.accept(accessor);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}