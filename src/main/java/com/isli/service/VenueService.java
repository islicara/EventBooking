package com.isli.service;

import com.isli.dto.VenueDTO;
import com.isli.entity.Venue;
import com.isli.exception.ResourceNotFoundException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.VenueRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueService {

    private static final Logger log = LogManager.getLogger(VenueService.class);

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Transactional
    public VenueDTO create(VenueDTO request) {
        log.trace("Entering create() name='{}', city='{}'", request.getName(), request.getCity());
        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .capacity(request.getCapacity())
                .build();
        venue = venueRepository.save(venue);
        log.info("Created venue '{}' in {}", venue.getName(), venue.getCity());
        return EntityMapper.toResponse(venue);
    }

    @Transactional
    public VenueDTO update(Long id, VenueDTO request) {
        Venue venue = findOrThrow(id);
        log.debug("Updating venue {}: '{}' -> '{}'", id, venue.getName(), request.getName());
        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setCapacity(request.getCapacity());
        log.info("Updated venue {}", id);
        return EntityMapper.toResponse(venue);
    }

    @Transactional
    public void delete(Long id) {
        Venue venue = findOrThrow(id);
        venueRepository.delete(venue);
        log.info("Deleted venue {}", id);
    }

    @Transactional
    public VenueDTO getById(Long id) {
        log.trace("Fetching venue {}", id);
        return EntityMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public List<VenueDTO> getAll() {
        log.trace("Fetching all venues");
        return venueRepository.findAll().stream().map(EntityMapper::toResponse).toList();
    }

    private Venue findOrThrow(Long id) {
        return venueRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Venue", id));
    }
}