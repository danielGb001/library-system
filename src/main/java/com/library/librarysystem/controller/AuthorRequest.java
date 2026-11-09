package com.library.librarysystem.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorRequest {

    @NotBlank(message = "name must not be empty")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @Size(max = 50, message = "nationality must be at most 50 characters")
    private String nationality;
}
