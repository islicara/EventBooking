package com.isli.service;

import com.isli.dto.ReviewDTO;
import com.isli.dto.ReviewRequestDTO;
import com.isli.entity.*;
import com.isli.exception.DuplicateResourceException;
import com.isli.exception.ResourceNotFoundException;
import com.isli.exception.ReviewNotAllowedException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.BookingRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.ReviewRepository;
import com.isli.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    private static final Logger log = LogManager.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewDTO create(ReviewRequestDTO request, String username) {
        log.trace("Entering create() username={}, eventId={}, rating={}",
                username, request.getEventId(), request.getRating());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> ResourceNotFoundException.of("Event", request.getEventId()));

        Booking booking = bookingRepository
                .findByUserIdAndEventIdAndStatus(user.getId(), event.getId(), BookingStatus.CONFIRMED)
                .orElseThrow(() -> {
                    log.warn("Rejected review: user '{}' has no confirmed booking for event {}", username, event.getId());
                    return new ReviewNotAllowedException(
                            "You need a confirmed booking for this event to leave a review");
                });

        if (!event.getEndDateTime().isBefore(LocalDateTime.now())) {
            log.warn("Rejected review: event {} has not taken place yet", event.getId());
            throw new ReviewNotAllowedException("You can only review an event after it has taken place");
        }
        if (reviewRepository.existsByUserIdAndEventId(user.getId(), event.getId())) {
            log.warn("Rejected review: user '{}' already reviewed event {}", username, event.getId());
            throw new DuplicateResourceException("You have already reviewed this event");
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .event(event)
                .booking(booking)
                .build();
        review = reviewRepository.save(review);

        log.info("User '{}' reviewed event {} with rating {}", username, event.getId(), request.getRating());
        return EntityMapper.toResponse(review);
    }

    @Transactional
    public Page<ReviewDTO> getForEvent(Long eventId, Pageable pageable) {
        log.trace("Fetching reviews for event {}", eventId);
        return reviewRepository.findByEventId(eventId, pageable).map(EntityMapper::toResponse);
    }
}