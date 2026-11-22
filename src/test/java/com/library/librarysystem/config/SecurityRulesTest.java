package com.library.librarysystem.config;

import com.library.librarysystem.controller.AuthorController;
import com.library.librarysystem.controller.BookController;
import com.library.librarysystem.controller.LoanController;
import com.library.librarysystem.controller.MemberController;
import com.library.librarysystem.service.AuthorService;
import com.library.librarysystem.service.BookService;
import com.library.librarysystem.service.LoanService;
import com.library.librarysystem.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the access rules in SecurityConfig: who may call what.
 *
 * "Allowed" cases send a bad or missing parameter and expect 400: that error
 * comes from the controller, so the request got past security.
 */
@WebMvcTest(controllers = {
        AuthorController.class, BookController.class,
        MemberController.class, LoanController.class})
@Import({SecurityConfig.class, JwtConfig.class})
class SecurityRulesTest {

    private static final String ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private LoanService loanService;

    // ---------- Anonymous: reads allowed, everything else 401 ----------

    @Test
    void anonymous_canRead() throws Exception {
        mockMvc.perform(get("/api/authors")).andExpect(status().isOk());
    }

    @Test
    void anonymous_createAuthor_returns401() throws Exception {
        mockMvc.perform(post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_updateBook_returns401() throws Exception {
        mockMvc.perform(put("/api/books/" + ID)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_deleteMember_returns401() throws Exception {
        mockMvc.perform(delete("/api/members/" + ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_borrow_returns401() throws Exception {
        mockMvc.perform(post("/api/loans/borrow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_return_returns401() throws Exception {
        mockMvc.perform(post("/api/loans/" + ID + "/return"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- USER: borrow/return allowed, admin actions 403 ----------

    @Test
    @WithMockUser(roles = "USER")
    void user_createAuthor_returns403() throws Exception {
        mockMvc.perform(post("/api/authors")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_updateBook_returns403() throws Exception {
        mockMvc.perform(put("/api/books/" + ID)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_deleteMember_returns403() throws Exception {
        mockMvc.perform(delete("/api/members/" + ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_borrow_passesSecurity() throws Exception {
        mockMvc.perform(post("/api/loans/borrow"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_return_passesSecurity() throws Exception {
        mockMvc.perform(post("/api/loans/not-a-uuid/return"))
                .andExpect(status().isBadRequest());
    }

    // ---------- ADMIN: may also borrow ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_borrow_passesSecurity() throws Exception {
        mockMvc.perform(post("/api/loans/borrow"))
                .andExpect(status().isBadRequest());
    }
}
