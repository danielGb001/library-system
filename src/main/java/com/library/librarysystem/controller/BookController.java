package com.library.librarysystem.controller;

import jakarta.validation.Valid;
import com.library.librarysystem.model.Book;
import com.library.librarysystem.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        Book created = bookService.createBook(
                request.getAuthorId(),
                request.getTitle(),
                request.getIsbn(),
                request.getTotalCopies()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(BookResponse.from(bookService.getBookById(id)));
    }

    // One page of books, optionally filtered by a title search: GET /api/books?title=ring&page=0&size=20
    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        // Sorted by title, then id, so the order (and therefore the pages) stay stable.
        Page<Book> books = bookService.searchBooks(title, PageRequest.of(page, size, Sort.by("title", "id")));
        return ResponseEntity.ok(PageResponse.from(books, BookResponse::from));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable UUID id,
                                                   @Valid @RequestBody BookRequest request) {
        Book updated = bookService.updateBook(
                id,
                request.getTitle(),
                request.getIsbn(),
                request.getTotalCopies()
        );
        return ResponseEntity.ok(BookResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
