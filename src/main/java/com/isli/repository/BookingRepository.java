package com.isli.repository;

import com.isli.entity.Booking;
import com.isli.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByEventOrganizerId(Long organizerId);

    Optional<Booking> findByUserIdAndEventIdAndStatus(Long userId, Long eventId, BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.seatsBooked), 0) FROM Booking b " +
            "WHERE b.event.id = :eventId AND b.status = :status")
    Integer sumConfirmedSeats(@Param("eventId") Long eventId, @Param("status") BookingStatus status);
}
