package com.example.springtemplate.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public UsernamePasswordAuthenticationToken authenticate(String jwt) {
        String userEmail = jwtService.extractUserName(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(jwt, userDetails)) {
            throw new AccessDeniedException("JWT is invalid or expired");
        }

        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    public Authentication revalidate(String jwt, Principal principal) {
        if (!(principal instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Unauthenticated STOMP session");
        }

        Object authenticatedPrincipal = authentication.getPrincipal();
        if (!(authenticatedPrincipal instanceof UserDetails userDetails)) {
            throw new AccessDeniedException("Authenticated STOMP session is invalid");
        }

        if (!jwtService.isTokenValid(jwt, userDetails)) {
            throw new AccessDeniedException("JWT is invalid or expired");
        }

        return authentication;
    }

    public boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}