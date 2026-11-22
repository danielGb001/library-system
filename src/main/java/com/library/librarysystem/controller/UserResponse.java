package com.library.librarysystem.controller;

import com.library.librarysystem.model.AppUser;

import java.util.UUID;

// Never includes the password hash.
public record UserResponse(UUID id, String username, String role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
