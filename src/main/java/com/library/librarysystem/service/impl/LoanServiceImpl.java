package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Book;
import com.library.librarysystem.model.Loan;
import com.library.librarysystem.model.Member;
import com.library.librarysystem.repository.BookRepository;
import com.library.librarysystem.repository.LoanRepository;
import com.library.librarysystem.repository.MemberRepository;
import com.library.librarysystem.service.LoanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Loan borrowBook(UUID bookId, UUID memberId) {
        // 1. Lock the member's row FIRST. Simultaneous borrows by the same member now take
        //    turns, so the loan-limit check below cannot be bypassed. (The lock must come
        //    before any other read, so the reads after it see the other request's committed loan.)
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + memberId));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + bookId));

        // Friendly early checks (the atomic update below is what truly guards the last copy).
        if (book.getAvailableCopies() <= 0) {
            throw new IllegalStateException("No available copies of \"" + book.getTitle() + "\" to borrow");
        }

        List<Loan> activeLoans = loanRepository.findByMember_IdAndReturnDateIsNull(memberId);
        if (activeLoans.size() >= MAX_ACTIVE_LOANS_PER_MEMBER) {
            throw new IllegalStateException(
                    "Member has reached the maximum of " + MAX_ACTIVE_LOANS_PER_MEMBER + " active loans");
        }

        // 2. Atomically take one copy: the "is one left?" check and the "-1" happen in a single
        //    SQL statement, so two simultaneous requests can never both take the last copy.
        if (bookRepository.decrementAvailableCopies(bookId) == 0) {
            throw new IllegalStateException("No available copies of \"" + book.getTitle() + "\" to borrow");
        }

        // 3. The update cleared Hibernate's in-memory copies, so reload the book (it now shows
        //    the reduced count) and the member before creating the loan.
        Book updatedBook = bookRepository.findById(bookId)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + bookId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + memberId));

        Loan loan = new Loan(updatedBook, member, LocalDate.now().plusDays(LOAN_PERIOD_DAYS));
        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public Loan returnBook(UUID loanId) {
        // Lock the loan's row: a second simultaneous return waits here, then finds
        // returnDate already set and gets the normal "already returned" error.
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new NoSuchElementException("Loan not found with id: " + loanId));

        if (loan.getReturnDate() != null) {
            throw new IllegalStateException("This loan has already been returned");
        }

        loan.setReturnDate(LocalDate.now());

        // Atomic +1 (it saves the return date first, then adds the copy back, capped at totalCopies).
        bookRepository.incrementAvailableCopies(loan.getBook().getId());

        // Reload so the response shows the updated book.
        return getLoanById(loanId);
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
