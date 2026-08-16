package com.isli.repository;

import com.isli.entity.Waitlist;
import com.isli.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    Optional<Waitlist> findByEventIdAndStatus(Long eventId, WaitlistStatus status);

    boolean existsByUserIdAndEventIdAndStatus(Long userId, Long eventId, WaitlistStatus status);

    long countByEventIdAndStatus(Long eventId, WaitlistStatus status);
}
