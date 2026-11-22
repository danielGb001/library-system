package com.library.librarysystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Turns the "role" claim of a token ("ADMIN" or "USER") into the authority ROLE_ADMIN or ROLE_USER.
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // Rules are checked top to bottom; the first match wins.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless API (no browser session or cookies), so CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Two ways to log in: HTTP Basic (username and password) or a Bearer token from /api/auth/login.
                .httpBasic(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .authorizeHttpRequests(auth -> auth
                        // Spring's error page must stay reachable, or errors would hide behind a login prompt.
                        .requestMatchers("/error").permitAll()
                        // Public: getting a token (the login itself checks the username and password).
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Public: the root redirect, health check, Swagger UI and API docs.
                        .requestMatchers(HttpMethod.GET, "/", "/actuator/health", "/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        // Public: every read of the library data.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // USER or ADMIN: borrowing and returning books.
                        .requestMatchers(HttpMethod.POST, "/api/loans/borrow", "/api/loans/*/return")
                        .hasAnyRole("USER", "ADMIN")
                        // ADMIN only: creating, updating and deleting authors, books and members.
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        // Anything else needs at least a login.
                        .anyRequest().authenticated());
        return http.build();
    }
}
