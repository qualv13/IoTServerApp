package org.qualv13.iotbackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // DEBUG: Requests
        final String authHeader = request.getHeader("Authorization");
        System.out.println("--- JWT FILTER DEBUG ---");
        System.out.println("Request URL: " + request.getRequestURI());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Brak nagłówka Authorization lub brak 'Bearer '");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        // Show part of token for debugging (only if token is long enough)
        if (jwt.length() > 10) {
            System.out.println("Token found: " + jwt.substring(0, 10) + "...");
        } else {
            System.out.println("Token found: " + jwt + " (short token)");
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            System.out.println("User from token: " + userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    System.out.println("Token jest poprawny! Ustawiam SecurityContext.");

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    System.out.println("Token NIE JEST poprawny według jwtService.isTokenValid()");
                }
            }
        } catch (Exception e) {
            // 2. TUTAJ JEST ZMIANA:
            // Jeśli token jest nieprawidłowy (np. wygasł), NIE puszczamy żądania dalej.
            // Zwracamy natychmiast błąd 403 Forbidden.

            System.out.println("BŁĄD WERYFIKACJI TOKENA (Zwracam 403): " + e.getMessage());

            // Ustawiamy status 403
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expired or invalid\"}");

            // Ważne: RETURN, aby nie wykonać filterChain.doFilter() na dole
            //return;
        }

        System.out.println("--- END FILTER DEBUG ---");
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        // Nie uruchamiaj filtra JWT dla Swaggera
        return  path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/auth/login") ||
                path.startsWith("/api/mqtt/auth");
    }
}