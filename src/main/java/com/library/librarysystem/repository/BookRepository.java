package com.library.librarysystem.repository;

import com.library.librarysystem.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    // Atomic: "check there is a copy left" and "take it" happen in ONE SQL statement,
    // so two simultaneous borrows can never both take the last copy.
    // Returns the number of rows changed: 1 = a copy was taken, 0 = none left (or no such book).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Book b set b.availableCopies = b.availableCopies - 1 "
            + "where b.id = :id and b.availableCopies > 0")
    int decrementAvailableCopies(@Param("id") UUID id);

    // Atomic return of a copy. The guard stops the count from ever exceeding totalCopies.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Book b set b.availableCopies = b.availableCopies + 1 "
            + "where b.id = :id and b.availableCopies < b.totalCopies")
    int incrementAvailableCopies(@Param("id") UUID id);

    // Case-insensitive "title contains" search, one page at a time (Spring Data writes the SQL from the method name).
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
