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
    public ResponseEntity<LoanResponse> borrowBook(@RequestParam UUID bookId,
                                                   @RequestParam UUID memberId) {
        Loan loan = loanService.borrowBook(bookId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.from(loan));
    }

    // POST /api/loans/{loanId}/return
    @PostMapping("/{loanId}/return")
    public ResponseEntity<LoanResponse> returnBook(@PathVariable UUID loanId) {
        return ResponseEntity.ok(LoanResponse.from(loanService.returnBook(loanId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable UUID id) {
        return ResponseEntity.ok(LoanResponse.from(loanService.getLoanById(id)));
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> getAllLoans() {
        List<LoanResponse> loans = loanService.getAllLoans().stream()
                .map(LoanResponse::from)
                .toList();
        return ResponseEntity.ok(loans);
    }

    // GET /api/loans/member/{memberId}
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<LoanResponse>> getActiveLoansForMember(@PathVariable UUID memberId) {
        List<LoanResponse> loans = loanService.getActiveLoansForMember(memberId).stream()
                .map(LoanResponse::from)
                .toList();
        return ResponseEntity.ok(loans);
    }
}
