package com.library.librarysystem.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // "ADMIN" or "USER" (without the ROLE_ prefix)
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
