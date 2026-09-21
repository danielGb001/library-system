package com.library.librarysystem.service;

import com.library.librarysystem.model.Author;
import java.util.List;
import java.util.UUID;

public interface AuthorService {
    Author createAuthor(String name, String nationality);
    Author getAuthorById(UUID id);
    List<Author> getAllAuthors();
    Author updateAuthor(UUID id, String name, String nationality);
    void deleteAuthor(UUID id);
}