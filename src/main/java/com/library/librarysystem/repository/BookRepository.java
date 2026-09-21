package com.library.librarysystem.repository;

import com.library.librarysystem.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
}
