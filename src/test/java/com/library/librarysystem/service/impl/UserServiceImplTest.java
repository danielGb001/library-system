package com.library.librarysystem.service.impl;

import com.library.librarysystem.model.AppUser;
import com.library.librarysystem.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_storesOnlyTheHashedPassword() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("longenough1")).thenReturn("HASHED");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = userService.createUser("newuser", "longenough1", "ADMIN");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertEquals("HASHED", captor.getValue().getPasswordHash());
        assertEquals("newuser", result.getUsername());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void createUser_withUnknownRole_throwsAndDoesNotSave() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("newuser", "longenough1", "SUPERUSER"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_withExistingUsername_throwsAndDoesNotSave() {
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> userService.createUser("taken", "longenough1", "USER"));
        verify(userRepository, never()).save(any());
    }
}
