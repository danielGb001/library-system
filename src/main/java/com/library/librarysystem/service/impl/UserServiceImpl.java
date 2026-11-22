package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.AppUser;
import com.library.librarysystem.repository.AppUserRepository;
import com.library.librarysystem.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ROLES = Set.of("ADMIN", "USER");

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AppUser createUser(String username, String password, String role) {
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be ADMIN or USER");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already exists");
        }
        return userRepository.save(new AppUser(username, passwordEncoder.encode(password), role));
    }
}
