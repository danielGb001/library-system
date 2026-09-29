package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.model.Book;
import com.library.librarysystem.repository.AuthorRepository;
import com.library.librarysystem.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private UUID authorId;
    private UUID bookId;
    private Author author;
    private Book existingBook;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        bookId = UUID.randomUUID();

        author = new Author();
        author.setId(authorId);
        author.setName("Chinua Achebe");
        author.setNationality("Nigerian");

        existingBook = Book.builder()
                .author(author)
                .title("Things Fall Apart")
                .isbn("9780385474542")
                .totalCopies(3)
                .availableCopies(3)
                .build();
        existingBook.setId(bookId);
    }

    // --- createBook ---

    @Test
    void createBook_success_setsAvailableCopiesEqualToTotalCopies() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.createBook(authorId, "Arrow of God", "9780435905250", 5);

        assertNotNull(result);
        assertEquals("Arrow of God", result.getTitle());
        assertEquals(5, result.getAvailableCopies(), "a brand-new book should start with availableCopies == totalCopies");
        assertEquals(author, result.getAuthor());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    void createBook_authorNotFound_throwsNoSuchElementException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> bookService.createBook(authorId, "Arrow of God", "9780435905250", 5));

        assertTrue(ex.getMessage().contains("Author not found"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createBook_blankTitle_throwsIllegalArgumentException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.createBook(authorId, "   ", "9780435905250", 5));

        assertTrue(ex.getMessage().contains("cannot be empty"));
        verify(bookRepository, never()).save(any());
    }

    // --- getBookById ---

    @Test
    void getBookById_success_returnsBook() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));

        Book result = bookService.getBookById(bookId);

        assertEquals(existingBook, result);
    }

    @Test
    void getBookById_notFound_throwsNoSuchElementException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> bookService.getBookById(bookId));
    }

    // --- getAllBooks ---

    @Test
    void getAllBooks_returnsListFromRepository() {
        when(bookRepository.findAll()).thenReturn(List.of(existingBook));

        List<Book> result = bookService.getAllBooks();

        assertEquals(1, result.size());
        assertEquals(existingBook, result.get(0));
    }

    // --- updateBook ---

    @Test
    void updateBook_success_recalculatesAvailableCopiesBasedOnCopiesOnLoan() {
        // 2 copies out of 3 are currently on loan (availableCopies = 1)
        existingBook.setAvailableCopies(1);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Increasing totalCopies from 3 to 5 — the 2 on loan should still be
        // reflected, so availableCopies should become 5 - 2 = 3
        Book result = bookService.updateBook(bookId, "Things Fall Apart", "9780385474542", 5);

        assertEquals(5, result.getTotalCopies());
        assertEquals(3, result.getAvailableCopies(), "availableCopies should be recalculated as totalCopies - copiesOnLoan");
    }

    @Test
    void updateBook_blankTitle_throwsIllegalArgumentException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));

        assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook(bookId, "", "9780385474542", 5));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBook_reducingTotalCopiesBelowCopiesOnLoan_throwsIllegalArgumentException() {
        // 2 copies out of 3 are currently on loan
        existingBook.setAvailableCopies(1);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(existingBook));

        // Trying to reduce totalCopies to 1, but 2 are already out on loan — should be rejected
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook(bookId, "Things Fall Apart", "9780385474542", 1));

        assertTrue(ex.getMessage().contains("currently on loan"));
        verify(bookRepository, never()).save(any());
    }

    // --- deleteBook ---

    @Test
    void deleteBook_success_callsRepositoryDelete() {
        when(bookRepository.existsById(bookId)).thenReturn(true);

        bookService.deleteBook(bookId);

        verify(bookRepository, times(1)).deleteById(bookId);
    }

    @Test
    void deleteBook_notFound_throwsNoSuchElementExceptionAndNeverDeletes() {
        when(bookRepository.existsById(bookId)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> bookService.deleteBook(bookId));

        verify(bookRepository, never()).deleteById(any());
    }
}
