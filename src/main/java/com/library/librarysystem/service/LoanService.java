package com.library.librarysystem.service;

import com.library.librarysystem.model.Loan;
import java.util.List;
import java.util.UUID;

public interface LoanService {
    Loan borrowBook(UUID bookId, UUID memberId);
    Loan returnBook(UUID loanId);
    Loan getLoanById(UUID id);
    List<Loan> getAllLoans();
    List<Loan> getActiveLoansForMember(UUID memberId);
}