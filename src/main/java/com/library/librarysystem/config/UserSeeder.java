package com.library.librarysystem.config;

import com.library.librarysystem.model.AppUser;
import com.library.librarysystem.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Copies the two accounts from the environment variables into the app_user table at startup.
// The variables have no defaults: if one is missing the application refuses to start.
@Component
public class UserSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String userUsername;
    private final String userPassword;

    public UserSeeder(AppUserRepository users, PasswordEncoder encoder,
                      @Value("${ADMIN_USERNAME}") String adminUsername,
                      @Value("${ADMIN_PASSWORD}") String adminPassword,
                      @Value("${USER_USERNAME}") String userUsername,
                      @Value("${USER_PASSWORD}") String userPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.userUsername = userUsername;
        this.userPassword = userPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        upsert(adminUsername, adminPassword, "ADMIN");
        upsert(userUsername, userPassword, "USER");
    }

    private void upsert(String username, String rawPassword, String role) {
        AppUser appUser = users.findByUsername(username).orElseGet(AppUser::new);
        appUser.setUsername(username);
        appUser.setPasswordHash(encoder.encode(rawPassword));
        appUser.setRole(role);
        users.save(appUser);
    }
}
