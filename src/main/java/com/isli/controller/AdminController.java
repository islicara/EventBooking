package com.isli.controller;

import com.isli.dto.BookingDTO;
import com.isli.dto.EventDTO;
import com.isli.dto.UserDTO;
import com.isli.service.BookingService;
import com.isli.service.EventService;
import com.isli.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "Admin oversight - users, all events, all bookings")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
    private final EventService eventService;
    private final BookingService bookingService;

    public AdminController(UserService userService, EventService eventService, BookingService bookingService) {
        this.userService = userService;
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<UserDTO> activate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setActive(id, true));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<UserDTO> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(userService.setActive(id, false));
    }

    @GetMapping("/events")
    public ResponseEntity<Page<EventDTO>> listAllEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.getAllForAdmin(pageable));
    }

    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingDTO>> listAllBookings(Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllForAdmin(pageable));
    }

    @PatchMapping("/bookings/{id}/cancel")
    public ResponseEntity<BookingDTO> cancelAnyBooking(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(bookingService.cancel(id, auth.getName(), true));
    }
}
