package com.library.librarysystem.controller;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.model.Book;
import com.library.librarysystem.model.Loan;
import com.library.librarysystem.model.Member;
import com.library.librarysystem.service.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for LoanController.
 *
 * LoanController takes no request body - borrow uses @RequestParam and the
 * rest use @PathVariable - so there's no ObjectMapper needed in this class.
 *
 * Two kinds of failure are covered here:
 *  1. Business-rule failures thrown by the (mocked) service and mapped by
 *     GlobalExceptionHandler: NoSuchElementException -> 404,
 *     IllegalStateException -> 409. These mirror the real LoanServiceImpl,
 *     which throws IllegalStateException for no copies left, max active
 *     loans, and double-return.
 *  2. HTTP-level failures that happen before the controller method even
 *     runs - a missing required @RequestParam, or a value that isn't a
 *     valid UUID. Spring's default resolvers turn these into 400s. These
 *     can't be tested at the service layer at all.
 *
 * Date fields (borrowDate/dueDate/returnDate) are only checked for presence,
 * not exact format, so these tests don't depend on how LocalDate happens to
 * be serialized.
 */
@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    private UUID bookId;
    private UUID memberId;
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        Author author = new Author("Chinua Achebe", "Nigerian");
        author.setId(UUID.randomUUID());

        book = Book.builder()
                .author(author)
                .title("Things Fall Apart")
                .isbn("978-0-385-47454-2")
                .totalCopies(3)
                .availableCopies(2)
                .build();
        book.setId(bookId);

        member = new Member("Ada Okafor", "ada@example.com");
        member.setId(memberId);
    }

    private Loan loanWithId(UUID loanId) {
        Loan loan = new Loan(book, member, LocalDate.now().plusDays(14));
        loan.setId(loanId);
        return loan;
    }

    // ---------- POST /api/loans/borrow ----------

    @Test
    void borrowBook_success_returns201() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.borrowBook(bookId, memberId)).thenReturn(loanWithId(loanId));

        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString())
                        .param("memberId", memberId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(loanId.toString()))
                .andExpect(jsonPath("$.book.title").value("Things Fall Apart"))
                .andExpect(jsonPath("$.member.fullName").value("Ada Okafor"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty());
    }

    @Test
    void borrowBook_missingMemberIdParam_returns400() throws Exception {
        // Required @RequestParam absent: Spring rejects this before the
        // controller method runs, so the service is never called.
        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void borrowBook_malformedUuidParam_returns400() throws Exception {
        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", "not-a-uuid")
                        .param("memberId", memberId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void borrowBook_bookNotFound_returns404() throws Exception {
        when(loanService.borrowBook(bookId, memberId))
                .thenThrow(new NoSuchElementException("Book not found with id: " + bookId));

        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString())
                        .param("memberId", memberId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id: " + bookId));
    }

    @Test
    void borrowBook_memberNotFound_returns404() throws Exception {
        when(loanService.borrowBook(bookId, memberId))
                .thenThrow(new NoSuchElementException("Member not found with id: " + memberId));

        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString())
                        .param("memberId", memberId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Member not found with id: " + memberId));
    }

    @Test
    void borrowBook_noAvailableCopies_returns409() throws Exception {
        when(loanService.borrowBook(bookId, memberId))
                .thenThrow(new IllegalStateException("No available copies of \"Things Fall Apart\" to borrow"));

        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString())
                        .param("memberId", memberId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No available copies of \"Things Fall Apart\" to borrow"));
    }

    @Test
    void borrowBook_memberAtMaxActiveLoans_returns409() throws Exception {
        when(loanService.borrowBook(bookId, memberId))
                .thenThrow(new IllegalStateException("Member has reached the maximum of 3 active loans"));

        mockMvc.perform(post("/api/loans/borrow")
                        .param("bookId", bookId.toString())
                        .param("memberId", memberId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Member has reached the maximum of 3 active loans"));
    }

    // ---------- POST /api/loans/{loanId}/return ----------

    @Test
    void returnBook_success_returns200WithReturnDate() throws Exception {
        UUID loanId = UUID.randomUUID();
        Loan returned = loanWithId(loanId);
        returned.setReturnDate(LocalDate.now());

        when(loanService.returnBook(loanId)).thenReturn(returned);

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId.toString()))
                .andExpect(jsonPath("$.returnDate").isNotEmpty());
    }

    @Test
    void returnBook_alreadyReturned_returns409() throws Exception {
        UUID loanId = UUID.randomUUID();

        when(loanService.returnBook(loanId))
                .thenThrow(new IllegalStateException("This loan has already been returned"));

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This loan has already been returned"));
    }

    @Test
    void returnBook_loanNotFound_returns404() throws Exception {
        UUID loanId = UUID.randomUUID();

        when(loanService.returnBook(loanId))
                .thenThrow(new NoSuchElementException("Loan not found with id: " + loanId));

        mockMvc.perform(post("/api/loans/{loanId}/return", loanId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Loan not found with id: " + loanId));
    }

    // ---------- GET /api/loans/{id} ----------

    @Test
    void getLoanById_success() throws Exception {
        UUID loanId = UUID.randomUUID();
        when(loanService.getLoanById(loanId)).thenReturn(loanWithId(loanId));

        mockMvc.perform(get("/api/loans/{id}", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId.toString()))
                .andExpect(jsonPath("$.book.title").value("Things Fall Apart"));
    }

    @Test
    void getLoanById_notFound_returns404() throws Exception {
        UUID loanId = UUID.randomUUID();

        when(loanService.getLoanById(loanId))
                .thenThrow(new NoSuchElementException("Loan not found with id: " + loanId));

        mockMvc.perform(get("/api/loans/{id}", loanId))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /api/loans ----------

    @Test
    void getAllLoans_success() throws Exception {
        when(loanService.getAllLoans())
                .thenReturn(List.of(loanWithId(UUID.randomUUID()), loanWithId(UUID.randomUUID())));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- GET /api/loans/member/{memberId} ----------

    @Test
    void getActiveLoansForMember_success() throws Exception {
        when(loanService.getActiveLoansForMember(memberId))
                .thenReturn(List.of(loanWithId(UUID.randomUUID())));

        mockMvc.perform(get("/api/loans/member/{memberId}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].member.fullName").value("Ada Okafor"));
    }
}
