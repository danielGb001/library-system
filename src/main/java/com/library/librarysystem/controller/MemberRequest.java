package com.library.librarysystem.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequest {

    @NotBlank(message = "fullName must not be empty")
    private String fullName;

    @Email(message = "email must be valid")
    @NotBlank(message = "email must not be empty")
    private String email;
}
