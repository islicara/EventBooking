package com.isli.controller;

import com.isli.dto.VenueDTO;
import com.isli.service.VenueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Venues", description = "Admin-managed venues")
@RestController
@RequestMapping("/api/venues")
@PreAuthorize("hasRole('ADMIN')")
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueDTO> create(@Valid @RequestBody VenueDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueDTO> update(@PathVariable Long id, @Valid @RequestBody VenueDTO request) {
        return ResponseEntity.ok(venueService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        venueService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(venueService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<VenueDTO>> getAll() {
        return ResponseEntity.ok(venueService.getAll());
    }
}
