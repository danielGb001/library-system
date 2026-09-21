package com.library.librarysystem.service;

import com.library.librarysystem.model.Book;
import java.util.List;
import java.util.UUID;

public interface BookService {
    Book createBook(UUID authorId, String title, String isbn, Integer totalCopies);
    Book getBookById(UUID id);
    List<Book> getAllBooks();
    Book updateBook(UUID id, String title, String isbn, Integer totalCopies);
    void deleteBook(UUID id);
}
