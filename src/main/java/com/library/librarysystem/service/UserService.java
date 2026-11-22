package com.library.librarysystem.service;

import com.library.librarysystem.model.AppUser;

public interface UserService {
    // role must be "ADMIN" or "USER"; the username must not exist yet.
    AppUser createUser(String username, String password, String role);
}
