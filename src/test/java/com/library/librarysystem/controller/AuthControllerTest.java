package com.library.librarysystem.controller;

import com.library.librarysystem.config.JwtConfig;
import com.library.librarysystem.config.SecurityConfig;
import com.library.librarysystem.config.UserConfig;
import com.library.librarysystem.service.AuthorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests POST /api/auth/login and that the token it returns is accepted by
 * the security rules. The user lookup is mocked, so no database is needed;
 * the real JwtConfig (token signing) and BCrypt password encoder are used.
 */
@WebMvcTest(controllers = {AuthController.class, AuthorController.class})
@Import({SecurityConfig.class, JwtConfig.class, UserConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthorService authorService;

    @BeforeEach
    void setUpUsers() {
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(
                User.withUsername("alice").password(passwordEncoder.encode("alice-pass")).roles("USER").build());
        when(userDetailsService.loadUserByUsername("boss")).thenReturn(
                User.withUsername("boss").password(passwordEncoder.encode("boss-pass")).roles("ADMIN").build());
        when(userDetailsService.loadUserByUsername("nobody"))
                .thenThrow(new UsernameNotFoundException("User not found"));
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(body);
        assertTrue(matcher.find(), "response should contain an accessToken");
        return matcher.group(1);
    }

    // ---------- Login ----------

    @Test
    void login_withCorrectPassword_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody("alice", "alice-pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody("alice", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_withUnknownUser_returnsSame401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody("nobody", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_withBlankFields_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody("", "")))
                .andExpect(status().isBadRequest());
    }

    // ---------- Using the token ----------

    @Test
    void userToken_isAccepted_butUserMayNotCreateAuthors() throws Exception {
        String token = loginAndGetToken("alice", "alice-pass");
        mockMvc.perform(post("/api/authors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminToken_passesSecurityOnAdminAction() throws Exception {
        String token = loginAndGetToken("boss", "boss-pass");
        int status = mockMvc.perform(post("/api/authors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertNotEquals(401, status);
        assertNotEquals(403, status);
    }

    @Test
    void garbageToken_returns401() throws Exception {
        mockMvc.perform(post("/api/authors")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
