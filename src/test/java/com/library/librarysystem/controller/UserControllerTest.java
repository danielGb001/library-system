package com.library.librarysystem.controller;

import com.library.librarysystem.config.JwtConfig;
import com.library.librarysystem.config.SecurityConfig;
import com.library.librarysystem.model.AppUser;
import com.library.librarysystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests POST /api/users (admin-only account creation). The service is mocked.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private String body(String username, String password, String role) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"role\":\"" + role + "\"}";
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_returns201WithoutPassword() throws Exception {
        AppUser saved = new AppUser("newuser", "some-hash", "USER");
        saved.setId(UUID.randomUUID());
        when(userService.createUser("newuser", "longenough1", "USER")).thenReturn(saved);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("newuser", "longenough1", "USER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createUser_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("newuser", "longenough1", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("newuser", "longenough1", "USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateUsername_returns409() throws Exception {
        when(userService.createUser(eq("taken"), any(), any()))
                .thenThrow(new IllegalStateException("Username already exists"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("taken", "longenough1", "USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_badRole_returns400() throws Exception {
        when(userService.createUser(any(), any(), eq("SUPERUSER")))
                .thenThrow(new IllegalArgumentException("Role must be ADMIN or USER"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("newuser", "longenough1", "SUPERUSER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Role must be ADMIN or USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("newuser", "short", "USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 8 and 72 characters"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "longenough1", "USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }
}
