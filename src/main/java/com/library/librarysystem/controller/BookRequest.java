package com.library.librarysystem.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class BookRequest {

    @NotNull(message = "authorId is required")
    private UUID authorId;

    @NotBlank(message = "title must not be empty")
    private String title;

    @NotBlank(message = "isbn must not be empty")
    private String isbn;

    @NotNull(message = "totalCopies is required")
    @Min(value = 1, message = "totalCopies must be at least 1")
    private Integer totalCopies;
}