package com.library.librarysystem.service;

import com.library.librarysystem.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface BookService {
    Book createBook(UUID authorId, String title, String isbn, Integer totalCopies);
    Book getBookById(UUID id);
    List<Book> getAllBooks();
    // One page of books; if title is not blank, only books whose title contains it (ignoring case).
    Page<Book> searchBooks(String title, Pageable pageable);
    Book updateBook(UUID id, String title, String isbn, Integer totalCopies);
    void deleteBook(UUID id);
}
