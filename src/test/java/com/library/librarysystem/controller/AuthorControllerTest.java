package com.library.librarysystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.librarysystem.model.Author;
import com.library.librarysystem.service.AuthorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for AuthorController.
 *
 * Unlike the service-layer tests, these go through MockMvc and hit the
 * actual HTTP layer: request parsing, the controller method, and
 * GlobalExceptionHandler's mapping of exceptions to status codes/JSON.
 *
 * @WebMvcTest loads ONLY the web layer for AuthorController (plus
 * @ControllerAdvice classes like GlobalExceptionHandler) - the real
 * AuthorService is replaced with a Mockito mock via @MockitoBean (Spring
 * Framework 7's replacement for the now-removed Spring Boot @MockBean),
 * same idea as @Mock in the service tests, just wired in by Spring instead
 * of MockitoExtension.
 */
@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Not @Autowired: in Spring Boot 4, @WebMvcTest no longer reliably
    // provides a Jackson ObjectMapper bean when the main app uses the
    // classic spring-boot-starter-web starter. We only need this to turn
    // Java objects into JSON strings for request bodies, which doesn't
    // require Spring's container at all - a plain instance works fine.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthorService authorService;

    // ---------- POST /api/authors ----------

    @Test
    void createAuthor_success() throws Exception {
        Author requestBody = new Author("Chinua Achebe", "Nigerian");

        Author saved = new Author("Chinua Achebe", "Nigerian");
        saved.setId(UUID.randomUUID());

        when(authorService.createAuthor("Chinua Achebe", "Nigerian")).thenReturn(saved);

        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Chinua Achebe"))
                .andExpect(jsonPath("$.nationality").value("Nigerian"));
    }

    @Test
    void createAuthor_blankName_returnsBadRequest() throws Exception {
        Author requestBody = new Author("", "Nigerian");

        // The controller has no @Valid here, so this 400 comes from the
        // service throwing IllegalArgumentException, not bean validation -
        // meaning no "fieldErrors" map, just a plain message.
        when(authorService.createAuthor("", "Nigerian"))
                .thenThrow(new IllegalArgumentException("Author name must not be blank"));

        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Author name must not be blank"));
    }

    // ---------- GET /api/authors/{id} ----------

    @Test
    void getAuthorById_success() throws Exception {
        UUID id = UUID.randomUUID();
        Author author = new Author("Chimamanda Ngozi Adichie", "Nigerian");
        author.setId(id);

        when(authorService.getAuthorById(id)).thenReturn(author);

        mockMvc.perform(get("/api/authors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Chimamanda Ngozi Adichie"));
    }

    @Test
    void getAuthorById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(authorService.getAuthorById(id))
                .thenThrow(new NoSuchElementException("Author not found with id: " + id));

        mockMvc.perform(get("/api/authors/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Author not found with id: " + id));
    }

    // ---------- GET /api/authors ----------

    @Test
    void getAllAuthors_success() throws Exception {
        Author a1 = new Author("Chinua Achebe", "Nigerian");
        a1.setId(UUID.randomUUID());
        Author a2 = new Author("Wole Soyinka", "Nigerian");
        a2.setId(UUID.randomUUID());

        when(authorService.getAllAuthors()).thenReturn(List.of(a1, a2));

        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Chinua Achebe"))
                .andExpect(jsonPath("$[1].name").value("Wole Soyinka"));
    }

    // ---------- PUT /api/authors/{id} ----------

    @Test
    void updateAuthor_success() throws Exception {
        UUID id = UUID.randomUUID();
        Author requestBody = new Author("Chinua Achebe", "Nigerian-British");

        Author updated = new Author("Chinua Achebe", "Nigerian-British");
        updated.setId(id);

        when(authorService.updateAuthor(id, "Chinua Achebe", "Nigerian-British")).thenReturn(updated);

        mockMvc.perform(put("/api/authors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nationality").value("Nigerian-British"));
    }

    @Test
    void updateAuthor_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        Author requestBody = new Author("Chinua Achebe", "Nigerian");

        when(authorService.updateAuthor(any(UUID.class), any(String.class), any(String.class)))
                .thenThrow(new NoSuchElementException("Author not found with id: " + id));

        mockMvc.perform(put("/api/authors/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /api/authors/{id} ----------

    @Test
    void deleteAuthor_success() throws Exception {
        UUID id = UUID.randomUUID();
        // authorService.deleteAuthor(id) is void - default Mockito behavior
        // (do nothing) is exactly what we want here, so no stubbing needed.

        mockMvc.perform(delete("/api/authors/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAuthor_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        org.mockito.Mockito.doThrow(new NoSuchElementException("Author not found with id: " + id))
                .when(authorService).deleteAuthor(id);

        mockMvc.perform(delete("/api/authors/{id}", id))
                .andExpect(status().isNotFound());
    }
}
