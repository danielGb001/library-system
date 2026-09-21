package com.library.librarysystem.repository;

import com.library.librarysystem.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    List<Loan> findByMember_IdAndReturnDateIsNull(UUID memberId);
}
