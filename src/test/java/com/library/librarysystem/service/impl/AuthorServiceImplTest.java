package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.repository.AuthorRepository;
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
class AuthorServiceImplTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private UUID authorId;
    private Author existingAuthor;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        existingAuthor = new Author("Chinua Achebe", "Nigerian");
        existingAuthor.setId(authorId);
    }

    // --- createAuthor ---

    @Test
    void createAuthor_success_savesAndReturnsAuthor() {
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Author result = authorService.createAuthor("Wole Soyinka", "Nigerian");

        assertNotNull(result);
        assertEquals("Wole Soyinka", result.getName());
        assertEquals("Nigerian", result.getNationality());
        verify(authorRepository, times(1)).save(any(Author.class));
    }

    @Test
    void createAuthor_blankName_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authorService.createAuthor("   ", "Nigerian"));

        assertTrue(ex.getMessage().contains("name cannot be empty"));
        verify(authorRepository, never()).save(any());
    }

    @Test
    void createAuthor_blankNationality_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authorService.createAuthor("Wole Soyinka", ""));

        assertTrue(ex.getMessage().contains("Nationality cannot be empty"));
        verify(authorRepository, never()).save(any());
    }

    // --- getAuthorById ---

    @Test
    void getAuthorById_success_returnsAuthor() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(existingAuthor));

        Author result = authorService.getAuthorById(authorId);

        assertEquals(existingAuthor, result);
    }

    @Test
    void getAuthorById_notFound_throwsNoSuchElementException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> authorService.getAuthorById(authorId));
    }

    // --- getAllAuthors ---

    @Test
    void getAllAuthors_returnsListFromRepository() {
        when(authorRepository.findAll()).thenReturn(List.of(existingAuthor));

        List<Author> result = authorService.getAllAuthors();

        assertEquals(1, result.size());
        assertEquals(existingAuthor, result.get(0));
    }

    // --- updateAuthor ---


    @Test
    void updateAuthor_success_updatesNameAndNationality() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(existingAuthor));
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Author result = authorService.updateAuthor(authorId, "Chinua Achebe Jr.", "British-Nigerian");

        assertEquals("Chinua Achebe Jr.", result.getName());
        assertEquals("British-Nigerian", result.getNationality());
        verify(authorRepository, times(1)).save(existingAuthor);
    }

    @Test
    void updateAuthor_notFound_throwsNoSuchElementException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> authorService.updateAuthor(authorId, "New Name", "New Nationality"));

        verify(authorRepository, never()).save(any());
    }

    @Test
    void updateAuthor_blankName_throwsIllegalArgumentException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(existingAuthor));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authorService.updateAuthor(authorId, "", "Nigerian"));

        assertTrue(ex.getMessage().contains("name cannot be empty"));
        verify(authorRepository, never()).save(any());
    }


    @Test
    void updateAuthor_blankNationality_throwsIllegalArgumentException() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(existingAuthor));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authorService.updateAuthor(authorId, "Wole Soyinka", "  "));

        assertTrue(ex.getMessage().contains("Nationality cannot be empty"));
        verify(authorRepository, never()).save(any());
    }

    // --- deleteAuthor ---

    @Test
    void deleteAuthor_success_callsRepositoryDelete() {
        when(authorRepository.existsById(authorId)).thenReturn(true);

        authorService.deleteAuthor(authorId);

        verify(authorRepository, times(1)).deleteById(authorId);
    }

    @Test
    void deleteAuthor_notFound_throwsNoSuchElementExceptionAndNeverDeletes() {
        when(authorRepository.existsById(authorId)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> authorService.deleteAuthor(authorId));

        verify(authorRepository, never()).deleteById(any());
    }
}