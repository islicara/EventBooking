package com.isli.service;

import com.isli.dto.BookingDTO;
import com.isli.dto.BookingRequestDTO;
import com.isli.entity.*;
import com.isli.exception.*;
import com.isli.mapper.EntityMapper;
import com.isli.repository.BookingRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.WaitlistRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LogManager.getLogger(BookingService.class);

    private static final long CANCELLATION_HOURS = 24;

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final WaitlistRepository waitlistRepository;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository, UserRepository userRepository, WaitlistRepository waitlistRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.waitlistRepository = waitlistRepository;
    }

    @Transactional
    public BookingDTO book(BookingRequestDTO request, String username) {
        log.trace("Entering book() eventId={}, seatsBooked={}, username={}",
                request.getEventId(), request.getSeatsBooked(), username);

        User user = findUser(username);
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> ResourceNotFoundException.of("Event", request.getEventId()));

        log.debug("Event {} before booking: status={}, availableSeats={}",
                event.getId(), event.getStatus(), event.getAvailableSeats());

        if (event.getStatus() != EventStatus.PUBLISHED) {
            log.warn("Rejected booking: event {} is not PUBLISHED (status={})", event.getId(), event.getStatus());
            throw new InvalidStateException("Event is not open for booking");
        }
        if (!event.getStartDateTime().isAfter(LocalDateTime.now())) {
            log.warn("Rejected booking: event {} has already started", event.getId());
            throw new InvalidStateException("Event has already started");
        }
        if (event.getAvailableSeats() < request.getSeatsBooked()) {
            log.warn("Rejected booking: event {} has only {} seat(s) left, {} requested",
                    event.getId(), event.getAvailableSeats(), request.getSeatsBooked());
            throw new SeatsUnavailableException(
                    "Only " + event.getAvailableSeats() + " seat(s) left - join the waitlist instead");
        }

        event.setAvailableSeats(event.getAvailableSeats() - request.getSeatsBooked());

        Booking booking = Booking.builder()
                .seatsBooked(request.getSeatsBooked())
                .status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now())
                .user(user)
                .event(event)
                .build();
        booking = bookingRepository.save(booking);

        log.info("User '{}' booked {} seat(s) for event {}", username, request.getSeatsBooked(), event.getId());
        return EntityMapper.toResponse(booking);
    }

    @Transactional
    public BookingDTO cancel(Long bookingId, String requesterUsername, boolean isAdmin) {
        log.trace("Entering cancel() bookingId={}, requester={}, isAdmin={}", bookingId, requesterUsername, isAdmin);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        if (!isAdmin && !booking.getUser().getUsername().equals(requesterUsername)) {
            log.warn("User '{}' attempted to cancel booking {} they do not own", requesterUsername, bookingId);
            throw new ForbiddenOperationException("You do not own this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already cancelled - rejecting duplicate cancel", bookingId);
            throw new InvalidStateException("Booking is already cancelled");
        }

        Event event = booking.getEvent();
        if (!isAdmin) {
            LocalDateTime cutoff = event.getStartDateTime().minusHours(CANCELLATION_HOURS);
            if (LocalDateTime.now().isAfter(cutoff)) {
                log.warn("Booking {} cancellation rejected: within {}h cutoff of event {}",
                        bookingId, CANCELLATION_HOURS, event.getId());
                throw new BookingCancellationNotAllowedException(
                        "Bookings cannot be cancelled within " + CANCELLATION_HOURS + " hours of the event");
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        event.setAvailableSeats(event.getAvailableSeats() + booking.getSeatsBooked());
        log.info("Booking {} cancelled by '{}' (admin={})", bookingId, requesterUsername, isAdmin);

        promoteFromWaitlist(event);

        return EntityMapper.toResponse(booking);
    }

    private void promoteFromWaitlist(Event event) {
        if (event.getAvailableSeats() <= 0) {
            log.debug("No seats available on event {} after cancellation - nothing to promote", event.getId());
            return;
        }
        waitlistRepository.findByEventIdAndStatus(event.getId(), WaitlistStatus.WAITING)
                .ifPresentOrElse(entry -> {
                    try {
                        Booking promoted = Booking.builder()
                                .seatsBooked(1)
                                .status(BookingStatus.CONFIRMED)
                                .bookingDate(LocalDateTime.now())
                                .user(entry.getUser())
                                .event(event)
                                .build();
                        bookingRepository.save(promoted);
                        event.setAvailableSeats(event.getAvailableSeats() - 1);

                        entry.setStatus(WaitlistStatus.PROMOTED);
                        log.info("Promoted waitlisted user '{}' to a confirmed booking for event {}",
                                entry.getUser().getUsername(), event.getId());
                    } catch (Exception ex) {
                        log.error("Failed to promote waitlisted user for event {}", event.getId(), ex);
                    }
                }, () -> log.debug("Event {} has open seats but no one is waiting", event.getId()));
    }

    @Transactional
    public Page<BookingDTO> getMyBookings(String username, BookingStatus status, Pageable pageable) {
        log.trace("Fetching bookings for user='{}', status={}, page={}", username, status, pageable.getPageNumber());
        User user = findUser(username);

        Page<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        } else {
            bookings = bookingRepository.findByUserId(user.getId(), pageable);
        }

        return bookings.map(EntityMapper::toResponse);
    }

    @Transactional
    public List<BookingDTO> getBookingsForOrganizerEvent(Long eventId, String organizerUsername) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("Event", eventId));
        if (!event.getOrganizer().getUsername().equals(organizerUsername)) {
            log.warn("Organizer '{}' attempted to view bookings for event {} they do not own",
                    organizerUsername, eventId);
            throw new ForbiddenOperationException("You do not own this event");
        }
        return bookingRepository.findByEventOrganizerId(event.getOrganizer().getId()).stream()
                .filter(b -> b.getEvent().getId().equals(eventId))
                .map(EntityMapper::toResponse)
                .toList();
    }

    @Transactional
    public Page<BookingDTO> getAllForAdmin(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(EntityMapper::toResponse);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}