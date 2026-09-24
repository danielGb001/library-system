package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.model.Book;
import com.library.librarysystem.repository.AuthorRepository;
import com.library.librarysystem.repository.BookRepository;
import com.library.librarysystem.service.BookService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookServiceImpl(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Override
    public Book createBook(UUID authorId, String title, String isbn, Integer totalCopies) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("Author not found with id: " + authorId));

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }

        Book book = Book.builder()
                .author(author)
                .title(title)
                .isbn(isbn)
                .totalCopies(totalCopies)
                .availableCopies(totalCopies)
                .build();

        return bookRepository.save(book);
    }

    @Override
    public Book getBookById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Book updateBook(UUID id, String title, String isbn, Integer totalCopies) {
        Book book = getBookById(id);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }

        // How many copies are currently out on loan, based on the OLD numbers
        int copiesOnLoan = book.getTotalCopies() - book.getAvailableCopies();

        if (totalCopies < copiesOnLoan) {
            throw new IllegalArgumentException(
                    "Cannot reduce totalCopies to " + totalCopies +
                            " — " + copiesOnLoan + " copies are currently on loan");
        }

        book.setTitle(title);
        book.setIsbn(isbn);
        book.setTotalCopies(totalCopies);
        // Recompute availableCopies so the two numbers stay consistent
        book.setAvailableCopies(totalCopies - copiesOnLoan);

        return bookRepository.save(book);
    }

    @Override
    public void deleteBook(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new NoSuchElementException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }
}
