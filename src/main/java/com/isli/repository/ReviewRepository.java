package com.isli.repository;

import com.isli.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Page<Review> findByEventId(Long eventId, Pageable pageable);
}
