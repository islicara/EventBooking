package com.isli.controller;

import com.isli.dto.BookingDTO;
import com.isli.dto.BookingRequestDTO;
import com.isli.entity.BookingStatus;
import com.isli.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookings", description = "Attendee seat booking, organizer/admin views")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @PostMapping
    public ResponseEntity<BookingDTO> book(@Valid @RequestBody BookingRequestDTO request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.book(request, auth.getName()));
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @GetMapping("/me")
    public ResponseEntity<Page<BookingDTO>> getMyBookings(
            @RequestParam(required = false) BookingStatus status, Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName(), status, pageable));
    }

    @PreAuthorize("hasAnyRole('ATTENDEE', 'ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingDTO> cancel(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(bookingService.cancel(id, auth.getName(), isAdmin));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/organizer/events/{eventId}")
    public ResponseEntity<List<BookingDTO>> getForOrganizerEvent(@PathVariable Long eventId, Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsForOrganizerEvent(eventId, auth.getName()));
    }
}
