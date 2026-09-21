package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Book;
import com.library.librarysystem.model.Loan;
import com.library.librarysystem.model.Member;
import com.library.librarysystem.repository.BookRepository;
import com.library.librarysystem.repository.LoanRepository;
import com.library.librarysystem.repository.MemberRepository;
import com.library.librarysystem.service.LoanService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class LoanServiceImpl implements LoanService {

    private static final int MAX_ACTIVE_LOANS_PER_MEMBER = 3;
    private static final int LOAN_PERIOD_DAYS = 14;

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public LoanServiceImpl(LoanRepository loanRepository,
                           BookRepository bookRepository,
                           MemberRepository memberRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Loan borrowBook(UUID bookId, UUID memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + bookId));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + memberId));

        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No available copies of \"" + book.getTitle() + "\" to borrow");
        }

        List<Loan> activeLoans = loanRepository.findByMember_IdAndReturnDateIsNull(memberId);
        if (activeLoans.size() >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new IllegalStateException(
                    "Member has reached the maximum of " + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan loan = new Loan(book, member, LocalDate.now().plusDays(LOAN_PERIOD_DAYS));
        return loanRepository.save(loan);
    }

    @Override
    public Loan returnBook(UUID loanId) {
        Loan loan = getLoanById(loanId);

        if (loan.getReturnDate() != null) {
            throw new IllegalStateException("This loan has already been returned");
        }

        loan.setReturnDate(LocalDate.now());

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        return loanRepository.save(loan);
    }

    @Override
    public Loan getLoanById(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Loan not found with id: " + id));
    }

    @Override
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Override
    public List<Loan> getActiveLoansForMember(UUID memberId) {
        return loanRepository.findByMember_IdAndReturnDateIsNull(memberId);
    }
}