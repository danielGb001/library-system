package com.library.librarysystem.controller;

import com.library.librarysystem.model.Author;
import lombok.Getter;
import java.util.UUID;

@Getter
public class AuthorResponse {

    private final UUID id;
    private final String name;
    private final String nationality;

    public AuthorResponse(UUID id, String name, String nationality) {
        this.id = id;
        this.name = name;
        this.nationality = nationality;
    }

    // Builds a response from a database entity, so controllers never return the entity itself.
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getNationality());
    }
}
