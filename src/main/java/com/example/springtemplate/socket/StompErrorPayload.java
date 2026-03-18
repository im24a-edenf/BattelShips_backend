package com.example.springtemplate.socket;

import java.time.Instant;

public record StompErrorPayload(
        String code,
        String message,
        Instant timestamp
) {
}