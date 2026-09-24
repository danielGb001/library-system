package com.library.librarysystem.controller;

import com.library.librarysystem.model.Loan;
import com.library.librarysystem.service.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // POST /api/loans/borrow?bookId=...&memberId=...
    @PostMapping("/borrow")
    public ResponseEntity<Loan> borrowBook(@RequestParam UUID bookId,
                                           @RequestParam UUID memberId) {
        Loan loan = loanService.borrowBook(bookId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    // POST /api/loans/{loanId}/return
    @PostMapping("/{loanId}/return")
    public ResponseEntity<Loan> returnBook(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.returnBook(loanId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @GetMapping
    public ResponseEntity<List<Loan>> getAllLoans() {
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    // GET /api/loans/member/{memberId}
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Loan>> getActiveLoansForMember(@PathVariable UUID memberId) {
        return ResponseEntity.ok(loanService.getActiveLoansForMember(memberId));
    }
}
