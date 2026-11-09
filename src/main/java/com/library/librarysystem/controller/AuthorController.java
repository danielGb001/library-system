package com.library.librarysystem.controller;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
        Author created = authorService.createAuthor(request.getName(), request.getNationality());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthorResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable UUID id) {
        Author author = authorService.getAuthorById(id);
        return ResponseEntity.ok(AuthorResponse.from(author));
    }

    @GetMapping
    public ResponseEntity<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors().stream()
                .map(AuthorResponse::from)
                .toList();
        return ResponseEntity.ok(authors);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponse> updateAuthor(@PathVariable UUID id,
                                                       @Valid @RequestBody AuthorRequest request) {
        Author updated = authorService.updateAuthor(id, request.getName(), request.getNationality());
        return ResponseEntity.ok(AuthorResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }
}
