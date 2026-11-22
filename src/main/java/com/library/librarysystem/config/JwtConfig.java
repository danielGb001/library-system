package com.library.librarysystem.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Creates and verifies login tokens (JWTs) signed with HS256.
// JWT_SECRET comes from an environment variable (or the git-ignored config/application.properties)
// and deliberately has no default: if it is missing or too short the application refuses to start.
@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(@Value("${JWT_SECRET}") String secret) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(toKey(secret)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${JWT_SECRET}") String secret) {
        return NimbusJwtDecoder.withSecretKey(toKey(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey toKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
