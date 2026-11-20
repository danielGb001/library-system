package com.library.librarysystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Rules are checked top to bottom; the first match wins.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless API with HTTP Basic: no browser session, so CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Spring's error page must stay reachable, or errors would hide behind a login prompt.
                        .requestMatchers("/error").permitAll()
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
