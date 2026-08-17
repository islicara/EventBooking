package com.isli.service;

import com.isli.dto.UserDTO;
import com.isli.entity.User;
import com.isli.exception.ResourceNotFoundException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Page<UserDTO> listUsers(Pageable pageable) {
        log.trace("Listing users, page={}", pageable.getPageNumber());
        return userRepository.findAll(pageable).map(EntityMapper::toResponse);
    }

    @Transactional
    public UserDTO setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        log.debug("User {} active flag: {} -> {}", userId, user.isActive(), active);
        user.setActive(active);
        log.info("User '{}' set active={} by admin", user.getUsername(), active);
        return EntityMapper.toResponse(user);
    }
}