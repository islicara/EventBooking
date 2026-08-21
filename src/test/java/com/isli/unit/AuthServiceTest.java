package com.isli.unit;

import com.isli.dto.auth.AuthDTO;
import com.isli.dto.auth.LoginRequestDTO;
import com.isli.dto.auth.RegisterRequestDTO;
import com.isli.entity.Role;
import com.isli.entity.User;
import com.isli.exception.AccountDisabledException;
import com.isli.exception.DuplicateResourceException;
import com.isli.repository.UserRepository;
import com.isli.security.JwtTokenService;
import com.isli.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequestDTO.builder()
                .username("cara")
                .password("cara12345")
                .email("cara@example.com")
                .role(Role.ATTENDEE)
                .build();
    }

    @Test
    void registerSavesUserAndReturnsToken() {
        when(userRepository.existsByUsername("cara")).thenReturn(false);
        when(userRepository.existsByEmail("cara@example.com")).thenReturn(false);
        when(passwordEncoder.encode("cara12345")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(tokenService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthDTO result = authService.register(registerRequest);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUsername()).isEqualTo("cara");
        assertThat(result.getRole()).isEqualTo(Role.ATTENDEE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerRejectsAdminSelfRegistration() {
        registerRequest.setRole(Role.ADMIN);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("cara")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("cara")).thenReturn(false);
        when(userRepository.existsByEmail("cara@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        User user = User.builder()
                .id(1L).username("cara").password("hashed")
                .email("cara@example.com").role(Role.ATTENDEE).active(true)
                .build();
        LoginRequestDTO loginRequest = LoginRequestDTO.builder().username("cara").password("cara12345").build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("cara12345", "hashed")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("jwt-token");

        AuthDTO result = authService.login(loginRequest);

        assertThat(result.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder()
                .id(1L).username("cara").password("hashed")
                .email("cara@example.com").role(Role.ATTENDEE).active(true)
                .build();
        LoginRequestDTO loginRequest = LoginRequestDTO.builder().username("cara").password("wrong").build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginRejectsDeactivatedAccount() {
        User user = User.builder()
                .id(1L).username("cara").password("hashed")
                .email("cara@example.com").role(Role.ATTENDEE).active(false)
                .build();
        LoginRequestDTO loginRequest = LoginRequestDTO.builder().username("cara").password("cara12345").build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccountDisabledException.class);
    }
}
