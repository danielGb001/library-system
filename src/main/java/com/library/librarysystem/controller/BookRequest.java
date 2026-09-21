package com.library.librarysystem.controller;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class BookRequest {
    private UUID authorId;
    private String title;
    private String isbn;
    private Integer totalCopies;
}
