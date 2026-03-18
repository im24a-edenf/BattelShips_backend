package com.example.springtemplate.security.config;

import java.util.List;

public final class CorsConfigurationValues {

    public static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:8080",
            "http://127.0.0.1:8080"
    );

    public static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    public static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");

    private CorsConfigurationValues() {
    }
}