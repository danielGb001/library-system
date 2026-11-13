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

import java.time.LocalDate;
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
        member.setEmail("ada@example.com");
    }

    @Test
    void borrowBook_success_takesACopyAtomicallyAndSavesLoan() {
        // The service locks the member row first, so it uses findByIdForUpdate.
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        // The book is read twice (before and after the atomic update); the same stub serves both.
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(loanRepository.findByMember_IdAndReturnDateIsNull(memberId)).thenReturn(List.of());
        // 1 = the atomic update really took a copy
        when(bookRepository.decrementAvailableCopies(bookId)).thenReturn(1);
        // "return whatever was passed in" — lets us inspect the exact object being saved
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.borrowBook(bookId, memberId);

        assertNotNull(result);
        assertSame(book, result.getBook());
        assertSame(member, result.getMember());
        assertEquals(LocalDate.now().plusDays(14), result.getDueDate());
        // verify() confirms a mock method was ACTUALLY called. The copy count is now changed
        // by the database in one atomic statement, so we verify that call instead of a setter.
        verify(memberRepository, times(1)).findByIdForUpdate(memberId);
        verify(bookRepository, times(1)).decrementAvailableCopies(bookId);
        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void borrowBook_bookNotFound_throwsNoSuchElementException() {
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("Book not found"));
        verify(bookRepository, never()).decrementAvailableCopies(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_memberNotFound_throwsNoSuchElementException() {
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("Member not found"));
        // The member is checked first now, so nothing else is touched
        verify(bookRepository, never()).findById(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_noAvailableCopies_throwsIllegalStateException() {
        book.setAvailableCopies(0);
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("No available copies"));
        verify(bookRepository, never()).decrementAvailableCopies(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_lastCopyTakenByAnotherRequest_throwsIllegalStateException() {
        // Simulates the race: this request SAW a copy available (1), but by the time the
        // atomic update runs, another request has taken it, so the update changes 0 rows.
        book.setAvailableCopies(1);
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.findByMember_IdAndReturnDateIsNull(memberId)).thenReturn(List.of());
        when(bookRepository.decrementAvailableCopies(bookId)).thenReturn(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("No available copies"));
        // No loan may be created when the copy could not be taken
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBook_memberAtMaxActiveLoans_throwsIllegalStateException() {
        when(memberRepository.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        // Simulate 3 existing active loans — exactly MAX_ACTIVE_LOANS_PER_MEMBER,
        // so the 4th attempt should be rejected
        List<Loan> threeActiveLoans = List.of(new Loan(), new Loan(), new Loan());
        when(loanRepository.findByMember_IdAndReturnDateIsNull(memberId)).thenReturn(threeActiveLoans);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.borrowBook(bookId, memberId));

        assertTrue(ex.getMessage().contains("maximum"));
        // A copy must NOT be taken when the limit blocks the borrow
        verify(bookRepository, never()).decrementAvailableCopies(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnBook_success_setsReturnDateAndReturnsCopyAtomically() {
        Loan loan = new Loan(book, member, LocalDate.now().plusDays(14));
        UUID loanId = UUID.randomUUID();

        // The loan row is locked on the way in; getLoanById reloads it for the response.
        when(loanRepository.findByIdForUpdate(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(bookRepository.incrementAvailableCopies(bookId)).thenReturn(1);

        Loan result = loanService.returnBook(loanId);

        assertNotNull(result.getReturnDate(), "returnDate should be set after returning");
        verify(loanRepository, times(1)).findByIdForUpdate(loanId);
        verify(bookRepository, times(1)).incrementAvailableCopies(bookId);
    }

    @Test
    void returnBook_alreadyReturned_throwsIllegalStateException() {
        Loan loan = new Loan(book, member, LocalDate.now().plusDays(14));
        loan.setReturnDate(LocalDate.now()); // already returned
        UUID loanId = UUID.randomUUID();

        when(loanRepository.findByIdForUpdate(loanId)).thenReturn(Optional.of(loan));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> loanService.returnBook(loanId));

        assertTrue(ex.getMessage().contains("already been returned"));
        // A second return must never add a copy back
        verify(bookRepository, never()).incrementAvailableCopies(any());
    }

    @Test
    void returnBook_loanNotFound_throwsNoSuchElementException() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findByIdForUpdate(loanId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> loanService.returnBook(loanId));
        verify(bookRepository, never()).incrementAvailableCopies(any());
    }
}
