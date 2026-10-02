package com.library.librarysystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.librarysystem.model.Member;
import com.library.librarysystem.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for MemberController.
 *
 * MemberController has no update endpoint (create, get-by-id, get-all,
 * delete only), and MemberRequest.email carries two stacked validation
 * annotations - @NotBlank and @Email - so a blank email and a malformed
 * (but non-blank) email are tested as two separate failure cases, since
 * they're caught by different constraints.
 *
 * Same Spring Boot 4 setup as the other controller tests: @WebMvcTest,
 * @MockitoBean, and a plain `new ObjectMapper()`.
 */
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberService memberService;

    private String requestJson(String fullName, String email) throws Exception {
        MemberRequest request = new MemberRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        return objectMapper.writeValueAsString(request);
    }

    // ---------- POST /api/members ----------

    @Test
    void createMember_success() throws Exception {
        Member saved = new Member("Ada Lovelace", "ada@example.com");
        saved.setId(UUID.randomUUID());

        when(memberService.registerMember("Ada Lovelace", "ada@example.com")).thenReturn(saved);

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Ada Lovelace", "ada@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void createMember_blankFullName_returnsBadRequestWithFieldError() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("", "ada@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").value("fullName must not be empty"));
    }

    @Test
    void createMember_blankEmail_returnsBadRequestWithFieldError() throws Exception {
        // Blank satisfies @Email (empty values are considered valid by the
        // Bean Validation spec), so @NotBlank is the one that fires here.
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Ada Lovelace", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("email must not be empty"));
    }

    @Test
    void createMember_malformedEmail_returnsBadRequestWithFieldError() throws Exception {
        // Non-blank but not a valid email shape - @NotBlank passes,
        // @Email is the one that fires here.
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Ada Lovelace", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("email must be valid"));
    }

    // ---------- GET /api/members/{id} ----------

    @Test
    void getMemberById_success() throws Exception {
        UUID id = UUID.randomUUID();
        Member member = new Member("Ada Lovelace", "ada@example.com");
        member.setId(id);

        when(memberService.getMemberById(id)).thenReturn(member);

        mockMvc.perform(get("/api/members/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"));
    }

    @Test
    void getMemberById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(memberService.getMemberById(id))
                .thenThrow(new NoSuchElementException("Member not found with id: " + id));

        mockMvc.perform(get("/api/members/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Member not found with id: " + id));
    }

    // ---------- GET /api/members ----------

    @Test
    void getAllMembers_success() throws Exception {
        Member m1 = new Member("Ada Lovelace", "ada@example.com");
        m1.setId(UUID.randomUUID());
        Member m2 = new Member("Grace Hopper", "grace@example.com");
        m2.setId(UUID.randomUUID());

        when(memberService.getAllMembers()).thenReturn(List.of(m1, m2));

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$[1].fullName").value("Grace Hopper"));
    }

    // ---------- DELETE /api/members/{id} ----------

    @Test
    void deleteMember_success() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/members/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMember_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        org.mockito.Mockito.doThrow(new NoSuchElementException("Member not found with id: " + id))
                .when(memberService).deleteMember(id);

        mockMvc.perform(delete("/api/members/{id}", id))
                .andExpect(status().isNotFound());
    }
}
