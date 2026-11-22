package com.library.librarysystem.config;

import com.library.librarysystem.model.AppUser;
import com.library.librarysystem.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DatabaseUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .roles(appUser.getRole())
                .build();
    }
}
