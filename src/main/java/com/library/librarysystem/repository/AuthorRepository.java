package com.library.librarysystem.repository;

import com.library.librarysystem.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {
}