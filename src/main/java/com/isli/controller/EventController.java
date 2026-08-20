package com.isli.controller;

import com.isli.dto.EventDTO;
import com.isli.dto.EventRequestDTO;
import com.isli.dto.EventSearchCriteria;
import com.isli.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Tag(name = "Events", description = "Public browsing plus organizer/admin event management")
@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }


    @GetMapping
    public ResponseEntity<Page<EventDTO>> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @ParameterObject Pageable pageable) {

        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .city(city)
                .categoryId(categoryId)
                .from(from)
                .to(to)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();

        return ResponseEntity.ok(eventService.search(criteria, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PostMapping
    public ResponseEntity<EventDTO> create(@Valid @RequestBody EventRequestDTO request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request, auth.getName()));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(@PathVariable Long id, @Valid @RequestBody EventRequestDTO request,
                                           Authentication auth) {
        return ResponseEntity.ok(eventService.update(id, request, auth.getName()));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<EventDTO> publish(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(eventService.publish(id, auth.getName()));
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EventDTO> cancel(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(eventService.cancel(id, auth.getName(), isAdmin));
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    @GetMapping("/organizer/me")
    public ResponseEntity<Page<EventDTO>> getMyEvents(Authentication auth, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.getByOrganizer(auth.getName(), pageable));
    }
}
