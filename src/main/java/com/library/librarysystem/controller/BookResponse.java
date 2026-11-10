package com.library.librarysystem.controller;

import com.library.librarysystem.model.Book;
import lombok.Getter;
import java.util.UUID;

@Getter
public class BookResponse {

    private final UUID id;
    private final AuthorResponse author;
    private final String title;
    private final String isbn;
    private final Integer totalCopies;
    private final Integer availableCopies;

    public BookResponse(UUID id, AuthorResponse author, String title, String isbn,
                        Integer totalCopies, Integer availableCopies) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.isbn = isbn;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    // Converts a Book entity into a response, including its author as an AuthorResponse.
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                AuthorResponse.from(book.getAuthor()),
                book.getTitle(),
                book.getIsbn(),
                book.getTotalCopies(),
                book.getAvailableCopies()
        );
    }
}
