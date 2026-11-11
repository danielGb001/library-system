package com.library.librarysystem.controller;

import com.library.librarysystem.model.Loan;
import lombok.Getter;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class LoanResponse {

    private final UUID id;
    private final BookResponse book;
    private final MemberResponse member;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate;

    public LoanResponse(UUID id, BookResponse book, MemberResponse member,
                        LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
        this.id = id;
        this.book = book;
        this.member = member;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Converts a Loan entity into a response, including its book and member as response DTOs.
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                BookResponse.from(loan.getBook()),
                MemberResponse.from(loan.getMember()),
                loan.getBorrowDate(),
                loan.getDueDate(),
                loan.getReturnDate()
        );
    }
}
