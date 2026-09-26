package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.Book;
import com.library.librarysystem.model.Loan;
import com.library.librarysystem.model.Member;
import com.library.librarysystem.repository.BookRepository;
import com.library.librarysystem.repository.LoanRepository;
import com.library.librarysystem.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) activates Mockito for this test class —
// it's what makes @Mock and @InjectMocks actually work.
@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    // @Mock creates a fake version of each dependency. No real database is
    // ever touched — we fully control what these "pretend" to return.
    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    // @InjectMocks builds a REAL LoanServiceImpl, automatically passing in
    // the three @Mock fields above through its constructor. This is the
    // actual class under test — everything else here is fake scaffolding
    // around it.
    @InjectMocks
    private LoanServiceImpl loanService;

    // Shared test data, rebuilt fresh before every single test method via
    // @BeforeEach, so tests can never accidentally affect each other by
    // reusing mutated state from a previous test.
    private UUID bookId;
    private UUID memberId;
    private Book book;
    private Member member;

    @BeforeEach
    void setUp() {
        bookId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        book = new Book();
        book.setId(bookId);
        book.setTitle("Things Fall Apart");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);

        member = new Member();
        member.setId(memberId);
        member.setFullName("Ada Okafor");
        member.setEmail("[email protected]");
    }

    @Test
    void borrowBook_success_decrementsAvailableCopiesAndSavesLoan() {
        // "when X is called, then return Y" — this is how we script a mock's behavior
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(loanRepository.findByMember_IdAndReturnDateIsNull(memberId)).thenReturn(List.of());
        // "return whatever was passed in" — lets us inspect the exact object being saved
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.borrowBook(bookId, memberId);

        assertNotNull(result);
        assertEquals(2, book.getAvailableCopies(), "available copies should decrease by 1");
        // verify() confirms a mock method was ACTUALLY called — proves the
        // service really persisted the updated book, not just mutated it in memory
        verify(bookRepository, times(1)).save(book);
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void borrowBook_bookNotFound_throwsNoSuchElementException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("Book not found"));
        // Confirms the method exits BEFORE ever touching the member lookup or saving anything
        verify(memberRepository, never()).findById(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_memberNotFound_throwsNoSuchElementException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("Member not found"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_noAvailableCopies_throwsIllegalStateException() {
        book.setAvailableCopies(0);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("No available copies"));
        verify(bookRepository, never()).save(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_memberAtMaxActiveLoans_throwsIllegalStateException() {
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // Simulate 3 existing active loans — exactly MAX_ACTIVE_LOANS_PER_MEMBER,
        // so the 4th attempt should be rejected
        List<Loan> threeActiveLoans = List.of(new Loan(), new Loan(), new Loan());
        when(loanRepository.findByMember_IdAndReturnDateIsNull(memberId)).thenReturn(threeActiveLoans);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("maximum"));
        assertEquals(3, book.getAvailableCopies(), "copies should NOT decrement when the limit blocks the borrow");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void returnBook_success_incrementsAvailableCopiesAndSetsReturnDate() {
        book.setAvailableCopies(2); // simulate one copy currently out on loan
        Loan loan = new Loan(book, member, java.time.LocalDate.now().plusDays(14));
        UUID loanId = UUID.randomUUID();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.returnBook(loanId);

        assertNotNull(result.getReturnDate(), "returnDate should be set after returning");
        assertEquals(3, book.getAvailableCopies(), "available copies should increase by 1");
        verify(bookRepository, times(1)).save(book);
    }

    @Test
    void returnBook_alreadyReturned_throwsIllegalStateException() {
        Loan loan = new Loan(book, member, java.time.LocalDate.now().plusDays(14));
        loan.setReturnDate(java.time.LocalDate.now()); // already returned
        UUID loanId = UUID.randomUUID();

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.returnBook(loanId));

        assertTrue(ex.getMessage().contains("already been returned"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void returnBook_loanNotFound_throwsNoSuchElementException() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> loanService.returnBook(loanId));
    }
}
