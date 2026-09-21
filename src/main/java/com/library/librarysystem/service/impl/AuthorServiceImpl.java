package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Author;
import com.library.librarysystem.repository.AuthorRepository;
import com.library.librarysystem.service.AuthorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorServiceImpl(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    public Author createAuthor(String name, String nationality) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Author name cannot be empty");
        }
        if (nationality == null || nationality.isBlank()) {
            throw new IllegalArgumentException("Nationality cannot be empty");
        }
        Author author = new Author(name, nationality);
        return authorRepository.save(author);
    }

    @Override
    public Author getAuthorById(UUID id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Author not found with id: " + id));
    }

    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Override
    public Author updateAuthor(UUID id, String name, String nationality) {
        Author existingAuthor = getAuthorById(id);
        existingAuthor.setName(name);
        existingAuthor.setNationality(nationality);
        return authorRepository.save(existingAuthor);
    }

    @Override
    public void deleteAuthor(UUID id) {
        if (!authorRepository.existsById(id)) {
            throw new NoSuchElementException("Author not found with id: " + id);
        }
        authorRepository.deleteById(id);
    }
}