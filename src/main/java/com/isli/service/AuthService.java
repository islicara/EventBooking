package com.isli.service;

import com.isli.dto.auth.AuthDTO;
import com.isli.dto.auth.LoginRequestDTO;
import com.isli.dto.auth.RegisterRequestDTO;
import com.isli.entity.Role;
import com.isli.entity.User;
import com.isli.exception.AccountDisabledException;
import com.isli.exception.DuplicateResourceException;
import com.isli.repository.UserRepository;
import com.isli.security.JwtTokenService;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthDTO register(RegisterRequestDTO request) {
        log.trace("Entering register() username='{}', role={}", request.getUsername(), request.getRole());

        if (request.getRole() == Role.ADMIN) {
            log.warn("Rejected registration: attempted self-registration as ADMIN (username='{}')",
                    request.getUsername());
            throw new IllegalArgumentException("Cannot self-register as ADMIN");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Rejected registration: username '{}' already taken", request.getUsername());
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Rejected registration: email '{}' already registered", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole())
                .active(true)
                .build();
        user = userRepository.save(user);
        log.debug("Persisted new user with id={}", user.getId());
        log.info("Registered new user '{}' with role {}", user.getUsername(), user.getRole());

        String token = tokenService.generateToken(user);
        return AuthDTO.builder().token(token).username(user.getUsername()).role(user.getRole()).build();
    }

    public AuthDTO login(LoginRequestDTO request) {
        log.trace("Entering login() username='{}'", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: unknown username '{}'", request.getUsername());
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password for username '{}'", request.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!user.isActive()) {
            log.warn("Login rejected: account '{}' is deactivated", request.getUsername());
            throw new AccountDisabledException("This account has been deactivated");
        }

        log.info("User '{}' logged in", user.getUsername());
        String token = tokenService.generateToken(user);
        return AuthDTO.builder().token(token).username(user.getUsername()).role(user.getRole()).build();
    }
}