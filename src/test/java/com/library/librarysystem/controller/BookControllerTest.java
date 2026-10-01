package com.library.librarysystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.librarysystem.model.Author;
import com.library.librarysystem.model.Book;
import com.library.librarysystem.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for BookController.
 *
 * Unlike AuthorController, BookController's create/update endpoints take a
 * BookRequest DTO annotated with @Valid, so bean-validation failures (blank
 * title, missing authorId, totalCopies below 1) are caught by Spring before
 * the controller method even runs, and surface as 400s with a "fieldErrors"
 * map from GlobalExceptionHandler.handleValidation - a path AuthorController
 * couldn't exercise since it takes a raw, unvalidated entity.
 *
 * Same Spring Boot 4 setup as AuthorControllerTest: @WebMvcTest for the web
 * layer only, @MockitoBean (not the removed @MockBean) to replace the real
 * BookService, and a plain `new ObjectMapper()` rather than @Autowired,
 * since @WebMvcTest doesn't reliably provide one in this Boot version.
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BookService bookService;

    private UUID validAuthorId;
    private Author author;

    private String validRequestJson(UUID authorId, String title, String isbn, Integer totalCopies) throws Exception {
        BookRequest request = new BookRequest();
        request.setAuthorId(authorId);
        request.setTitle(title);
        request.setIsbn(isbn);
        request.setTotalCopies(totalCopies);
        return objectMapper.writeValueAsString(request);
    }

    // ---------- POST /api/books ----------

    @Test
    void createBook_success() throws Exception {
        UUID authorId = UUID.randomUUID();
        Author author = new Author("Chinua Achebe", "Nigerian");
        author.setId(authorId);

        Book saved = Book.builder()
                .author(author)
                .title("Things Fall Apart")
                .isbn("978-0-385-47454-2")
                .totalCopies(5)
                .availableCopies(5)
                .build();
        saved.setId(UUID.randomUUID());

        when(bookService.createBook(authorId, "Things Fall Apart", "978-0-385-47454-2", 5))
                .thenReturn(saved);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart", "978-0-385-47454-2", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Things Fall Apart"))
                .andExpect(jsonPath("$.isbn").value("978-0-385-47454-2"))
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    void createBook_blankTitle_returnsBadRequestWithFieldError() throws Exception {
        UUID authorId = UUID.randomUUID();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "", "978-0-385-47454-2", 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title must not be empty"));
    }

    @Test
    void createBook_missingAuthorId_returnsBadRequestWithFieldError() throws Exception {
        // authorId deliberately omitted -> deserializes as null -> @NotNull fires
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(null, "Things Fall Apart", "978-0-385-47454-2", 5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.authorId").value("authorId is required"));
    }

    @Test
    void createBook_totalCopiesBelowMinimum_returnsBadRequestWithFieldError() throws Exception {
        UUID authorId = UUID.randomUUID();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart", "978-0-385-47454-2", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.totalCopies").value("totalCopies must be at least 1"));
    }

    @Test
    void createBook_authorNotFound_returns404() throws Exception {
        UUID authorId = UUID.randomUUID();

        when(bookService.createBook(any(UUID.class), any(String.class), any(String.class), any(Integer.class)))
                .thenThrow(new NoSuchElementException("Author not found with id: " + authorId));

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart", "978-0-385-47454-2", 5)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found with id: " + authorId));
    }

    // ---------- GET /api/books/{id} ----------

    @Test
    void getBookById_success() throws Exception {
        UUID bookId = UUID.randomUUID();
        Author author = new Author("Chinua Achebe", "Nigerian");
        author.setId(UUID.randomUUID());

        Book book = Book.builder()
                .author(author)
                .title("Things Fall Apart")
                .isbn("978-0-385-47454-2")
                .totalCopies(5)
                .availableCopies(3)
                .build();
        book.setId(bookId);

        when(bookService.getBookById(bookId)).thenReturn(book);

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookId.toString()))
                .andExpect(jsonPath("$.availableCopies").value(3));
    }

    @Test
    void getBookById_notFound_returns404() throws Exception {
        UUID bookId = UUID.randomUUID();

        when(bookService.getBookById(bookId))
                .thenThrow(new NoSuchElementException("Book not found with id: " + bookId));

        mockMvc.perform(get("/api/books/{id}", bookId))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /api/books ----------

    @Test
    void getAllBooks_success() throws Exception {
        Author author = new Author("Chinua Achebe", "Nigerian");
        author.setId(UUID.randomUUID());

        Book b1 = Book.builder().author(author).title("Things Fall Apart").isbn("111").totalCopies(5).availableCopies(5).build();
        b1.setId(UUID.randomUUID());
        Book b2 = Book.builder().author(author).title("No Longer at Ease").isbn("222").totalCopies(3).availableCopies(3).build();
        b2.setId(UUID.randomUUID());

        when(bookService.getAllBooks()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Things Fall Apart"))
                .andExpect(jsonPath("$[1].title").value("No Longer at Ease"));
    }

    // ---------- PUT /api/books/{id} ----------

    @Test
    void updateBook_success() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        Author author = new Author("Chinua Achebe", "Nigerian");
        author.setId(authorId);

        Book updated = Book.builder()
                .author(author)
                .title("Things Fall Apart (Revised)")
                .isbn("978-0-385-47454-2")
                .totalCopies(6)
                .availableCopies(6)
                .build();
        updated.setId(bookId);

        when(bookService.updateBook(bookId, "Things Fall Apart (Revised)", "978-0-385-47454-2", 6))
                .thenReturn(updated);

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart (Revised)", "978-0-385-47454-2", 6)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Things Fall Apart (Revised)"))
                .andExpect(jsonPath("$.totalCopies").value(6));
    }

    @Test
    void updateBook_notFound_returns404() throws Exception {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(bookService.updateBook(any(UUID.class), any(String.class), any(String.class), any(Integer.class)))
                .thenThrow(new NoSuchElementException("Book not found with id: " + bookId));

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart", "978-0-385-47454-2", 5)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBook_reducingBelowCopiesOnLoan_returnsConflict() throws Exception {
        // Mirrors the service-layer test: reducing totalCopies below the
        // number currently on loan is a business-rule conflict, not a
        // validation error, so the service throws IllegalStateException
        // and GlobalExceptionHandler maps it to 409, not 400.
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        when(bookService.updateBook(any(UUID.class), any(String.class), any(String.class), any(Integer.class)))
                .thenThrow(new IllegalStateException("totalCopies cannot be less than copies currently on loan"));

        mockMvc.perform(put("/api/books/{id}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson(authorId, "Things Fall Apart", "978-0-385-47454-2", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("totalCopies cannot be less than copies currently on loan"));
    }

    // ---------- DELETE /api/books/{id} ----------

    @Test
    void deleteBook_success() throws Exception {
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(delete("/api/books/{id}", bookId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_notFound_returns404() throws Exception {
        UUID bookId = UUID.randomUUID();

        org.mockito.Mockito.doThrow(new NoSuchElementException("Book not found with id: " + bookId))
                .when(bookService).deleteBook(bookId);

        mockMvc.perform(delete("/api/books/{id}", bookId))
                .andExpect(status().isNotFound());
    }
}
