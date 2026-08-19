package com.isli.service;

import com.isli.dto.EventDTO;
import com.isli.dto.EventRequestDTO;
import com.isli.dto.EventSearchCriteria;
import com.isli.entity.*;
import com.isli.exception.ForbiddenOperationException;
import com.isli.exception.InvalidStateException;
import com.isli.exception.ResourceNotFoundException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.CategoryRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.VenueRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EventService {

    private static final Logger log = LogManager.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventDTO create(EventRequestDTO request, String organizerUsername) {
        log.trace("Entering create() organizer={}, title='{}'", organizerUsername, request.getTitle());
        validateDates(request);
        User organizer = findUser(organizerUsername);
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> ResourceNotFoundException.of("Venue", request.getVenueId()));
        Set<Category> categories = resolveCategories(request.getCategoryId());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .price(request.getPrice())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .status(EventStatus.DRAFT)
                .venue(venue)
                .organizer(organizer)
                .categories(categories)
                .build();
        event = eventRepository.save(event);
        log.info("Organizer '{}' created event '{}' (id={})", organizerUsername, event.getTitle(), event.getId());
        return toResponse(event);
    }

    @Transactional
    public EventDTO update(Long id, EventRequestDTO request, String organizerUsername) {
        validateDates(request);
        Event event = findEventForOwner(id, organizerUsername);
        if (event.getStatus() == EventStatus.CANCELLED) {
            log.warn("Rejected update: event {} is CANCELLED", id);
            throw new InvalidStateException("Cannot update a cancelled event");
        }

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> ResourceNotFoundException.of("Venue", request.getVenueId()));
        Set<Category> categories = resolveCategories(request.getCategoryId());

        int seats = request.getTotalSeats() - event.getTotalSeats();
        log.debug("Event {} seat delta on update: {} -> {} ({})",
                id, event.getTotalSeats(), request.getTotalSeats(), seats);

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setPrice(request.getPrice());
        event.setTotalSeats(request.getTotalSeats());
        event.setAvailableSeats(event.getAvailableSeats() + seats);
        event.setVenue(venue);
        event.setCategories(categories);

        log.info("Organizer '{}' updated event {}", organizerUsername, id);
        return toResponse(event);
    }

    @Transactional
    public EventDTO publish(Long id, String organizerUsername) {
        Event event = findEventForOwner(id, organizerUsername);
        if (event.getStatus() != EventStatus.DRAFT) {
            log.warn("Rejected publish: event {} is not DRAFT (status={})", id, event.getStatus());
            throw new InvalidStateException("Only a DRAFT event can be published");
        }
        event.setStatus(EventStatus.PUBLISHED);
        log.info("Organizer '{}' published event {}", organizerUsername, id);
        return toResponse(event);
    }

    @Transactional
    public EventDTO cancel(Long id, String requesterUsername, boolean isAdmin) {
        Event event = eventRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Event", id));
        if (!isAdmin && !event.getOrganizer().getUsername().equals(requesterUsername)) {
            log.warn("User '{}' attempted to cancel event {} they do not own", requesterUsername, id);
            throw new ForbiddenOperationException("You do not own this event");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            log.warn("Event {} is already cancelled - rejecting duplicate cancel", id);
            throw new InvalidStateException("Event is already cancelled");
        }
        event.setStatus(EventStatus.CANCELLED);
        log.info("Event {} cancelled by '{}' (admin={})", id, requesterUsername, isAdmin);
        return toResponse(event);
    }

    @Transactional
    public EventDTO getById(Long id) {
        log.trace("Fetching event {}", id);
        Event event = eventRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Event", id));
        return toResponse(event);
    }

    @Transactional
    public Page<EventDTO> search(EventSearchCriteria c, Pageable pageable) {
        log.debug("Searching events: city={}, categoryId={}, from={}, to={}, minPrice={}, maxPrice={}",
                c.getCity(), c.getCategoryId(), c.getFrom(), c.getTo(), c.getMinPrice(), c.getMaxPrice());
        return eventRepository.search(
                EventStatus.PUBLISHED,
                c.getCity(),
                c.getCategoryId(),
                c.getFrom(),
                c.getTo(),
                c.getMinPrice(),
                c.getMaxPrice(),
                pageable
        ).map(this::toResponse);
    }

    @Transactional
    public Page<EventDTO> getByOrganizer(String organizerUsername, Pageable pageable) {
        User organizer = findUser(organizerUsername);
        return eventRepository.findByOrganizerId(organizer.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public Page<EventDTO> getAllForAdmin(Pageable pageable) {
        return eventRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public List<EventDTO> getByStatus(EventStatus status) {
        log.trace("Fetching events with status={}", status);
        return eventRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    private Event findEventForOwner(Long id, String organizerUsername) {
        Event event = eventRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Event", id));
        if (!event.getOrganizer().getUsername().equals(organizerUsername)) {
            log.warn("User '{}' attempted to modify event {} they do not own", organizerUsername, id);
            throw new ForbiddenOperationException("You do not own this event");
        }
        return event;
    }

    private Set<Category> resolveCategories(Set<Long> ids) {
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(ids));
        if (categories.size() != ids.size()) {
            log.warn("Event create/update referenced unknown category id(s) out of {}", ids);
            throw new ResourceNotFoundException("One or more category ids do not exist");
        }
        return categories;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void validateDates(EventRequestDTO request) {
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }
    }

    private EventDTO toResponse(Event event) {
        Double avgRating = eventRepository.findAverageRating(event.getId());
        return EntityMapper.toResponse(event, avgRating);
    }
}