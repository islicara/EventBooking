package com.isli.controller;

import com.isli.dto.WaitlistDTO;
import com.isli.service.WaitlistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Waitlist", description = "Join the waitlist for a fully booked event")
@RestController
@RequestMapping("/api/events/{eventId}/waitlist")
public class WaitlistController {
    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @PostMapping
    public ResponseEntity<WaitlistDTO> join(@PathVariable Long eventId, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.join(eventId, auth.getName()));
    }
}
