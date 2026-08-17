package com.isli.service;

import com.isli.dto.WaitlistDTO;
import com.isli.entity.Event;
import com.isli.entity.User;
import com.isli.entity.Waitlist;
import com.isli.entity.WaitlistStatus;
import com.isli.exception.DuplicateResourceException;
import com.isli.exception.InvalidStateException;
import com.isli.exception.ResourceNotFoundException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.WaitlistRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class WaitlistService {

    private static final Logger log = LogManager.getLogger(WaitlistService.class);

    private final WaitlistRepository waitlistRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public WaitlistService(WaitlistRepository waitlistRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.waitlistRepository = waitlistRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WaitlistDTO join(Long eventId, String username) {
        log.trace("Entering join() eventId={}, username={}", eventId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("Event", eventId));

        if (event.getAvailableSeats() > 0) {
            log.warn("Rejected waitlist join: event {} still has {} seat(s) available",
                    eventId, event.getAvailableSeats());
            throw new InvalidStateException("Event still has seats available - book directly instead");
        }
        if (waitlistRepository.existsByUserIdAndEventIdAndStatus(user.getId(), eventId, WaitlistStatus.WAITING)) {
            log.warn("Rejected waitlist join: user '{}' already waiting for event {}", username, eventId);
            throw new DuplicateResourceException("You are already on the waitlist for this event");
        }

        long position = waitlistRepository.countByEventIdAndStatus(eventId, WaitlistStatus.WAITING) + 1;
        log.debug("Assigning waitlist position {} to user '{}' for event {}", position, username, eventId);

        Waitlist entry = Waitlist.builder()
                .status(WaitlistStatus.WAITING)
                .position(position)
                .user(user)
                .event(event)
                .build();
        entry = waitlistRepository.save(entry);

        log.info("User '{}' joined waitlist for event {} at position {}", username, eventId, position);
        return EntityMapper.toResponse(entry);
    }
}