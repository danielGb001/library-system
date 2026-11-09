package com.library.librarysystem.exception;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the error responses produced by GlobalExceptionHandler.
 *
 * The handler is shared by every controller, so this class loads all four and
 * mocks their services, then triggers each failure through a real HTTP call.
 */
@WebMvcTest(controllers = {
        AuthorController.class, BookController.class,
        MemberController.class, LoanController.class})
class GlobalExceptionHandlerTest {

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

    // ---------- Database constraint violations -> 409 ----------

    @Test
    void duplicateUniqueValue_returns409() throws Exception {
        when(memberService.registerMember(any(String.class), any(String.class)))
                .thenThrow(new DataIntegrityViolationException("could not execute statement",
                        new RuntimeException("Duplicate entry 'dup@example.com' for key 'member.UK_email'")));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dup Test\",\"email\":\"dup@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "A record with the same unique value (for example an email or ISBN) already exists"));
    }

    @Test
    void foreignKeyViolation_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Cannot delete or update a parent row: a foreign key constraint fails")))
                .when(authorService).deleteAuthor(id);

        mockMvc.perform(delete("/api/authors/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This record is still referenced by other records, or refers to one that does not exist"));
    }

    @Test
    void otherIntegrityViolation_returnsGeneric409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new DataIntegrityViolationException("could not execute statement",
                new RuntimeException("Something unrecognised")))
                .when(authorService).deleteAuthor(id);

        mockMvc.perform(delete("/api/authors/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data"));
    }

    // ---------- Problems with the request itself -> 400 ----------

    @Test
    void malformedJsonBody_returns400WithMessage() throws Exception {
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void invalidUuidInPath_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/api/authors/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'not-a-uuid' for parameter 'id'"));
    }

    @Test
    void missingRequiredParameter_returns400WithMessage() throws Exception {
        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'memberId' is missing"));
    }

    @Test
    void validationFailure_includesMessageAndFieldErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":\"" + UUID.randomUUID()
                                + "\",\"title\":\"\",\"isbn\":\"111\",\"totalCopies\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title must not be empty"));
    }
}
