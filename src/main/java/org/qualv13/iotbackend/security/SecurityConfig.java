package org.qualv13.iotbackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http)) // Włącz obsługę CORS z kontrolerów
                .authorizeHttpRequests(auth -> auth
                        // 1. Zezwól na pliki statyczne i stronę główną (TO NAPRAWI TWÓJ BŁĄD)
                        .requestMatchers("/", "/index.html", "/static/**", "/*.js", "/*.css", "/favicon.ico").permitAll()

                        // 2. Logowanie i Rejestracja
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll() // Rejestracja

                        // 3. Obsługa zapytań OPTIONS (dla CORS w przeglądarce)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 4. Reszta wymaga logowania
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
