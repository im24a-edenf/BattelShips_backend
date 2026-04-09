package com.example.springtemplate.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CorsConfigurationValues {

    @Value("${ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://localhost:8080}")
    private String allowedOriginsValue;

    public List<String> getAllowedOrigins() {
        return Arrays.asList(allowedOriginsValue.split(","));
    }

    public static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    public static final List<String> ALLOWED_HEADERS = List.of("Authorization", "Content-Type");
}
