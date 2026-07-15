package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.ChangePasswordRequest;
import com.nakul.wealthwise.dto.request.UpdateProfileRequest;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.InvalidPasswordException;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private String email = "test@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .password("encodedPassword")
                .enabled(true)
                .build();
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User result = userService.getProfile(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void getProfile_ThrowsUsernameNotFoundException() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getProfile(email));
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateProfile(email, request);

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(email, request);

        assertEquals("newEncodedPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changePassword_ThrowsInvalidPasswordException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .oldPassword("wrongOldPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidPasswordException.class, () -> userService.changePassword(email, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivateAccount_Success() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deactivateAccount(email);

        assertFalse(user.getEnabled());
        verify(userRepository, times(1)).save(user);
    }
}
