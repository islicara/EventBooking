package com.isli.repository;

import com.isli.entity.Event;
import com.isli.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    List<Event> findByStatus(EventStatus status);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.categories c
            WHERE e.status = :status
            AND (:city IS NULL OR LOWER(e.venue.city) = LOWER(:city))
            AND (:categoryId IS NULL OR c.id = :categoryId)
            AND (:from IS NULL OR e.startDateTime >= :from)
            AND (:to IS NULL OR e.startDateTime <= :to)
            AND (:minPrice IS NULL OR e.price >= :minPrice)
            AND (:maxPrice IS NULL OR e.price <= :maxPrice)
            """)
    Page<Event> search(
            @Param("status") EventStatus status,
            @Param("city") String city,
            @Param("categoryId") Long categoryId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query(value = "SELECT AVG(r.rating) FROM reviews r WHERE r.event_id = :eventId", nativeQuery = true)
    Double findAverageRating(@Param("eventId") Long eventId);
}
