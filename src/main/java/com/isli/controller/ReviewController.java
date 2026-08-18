package com.isli.controller;

import com.isli.dto.ReviewDTO;
import com.isli.dto.ReviewRequestDTO;
import com.isli.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reviews", description = "Post-event ratings and reviews")
@RestController
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PreAuthorize("hasRole('ATTENDEE')")
    @PostMapping("/api/reviews")
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewRequestDTO request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request, auth.getName()));
    }

    @GetMapping("/api//events/{eventId}/reviews")
    public ResponseEntity<Page<ReviewDTO>> getForEvent(@PathVariable Long eventId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getForEvent(eventId, pageable));
    }
}
