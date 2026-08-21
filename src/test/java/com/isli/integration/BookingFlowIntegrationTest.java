package com.isli.integration;

import com.isli.dto.BookingRequestDTO;
import com.isli.dto.WaitlistDTO;
import com.isli.entity.Booking;
import com.isli.entity.BookingStatus;
import com.isli.entity.Category;
import com.isli.entity.Event;
import com.isli.entity.EventStatus;
import com.isli.entity.Role;
import com.isli.entity.User;
import com.isli.entity.Venue;
import com.isli.exception.SeatsUnavailableException;
import com.isli.repository.BookingRepository;
import com.isli.repository.CategoryRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.VenueRepository;
import com.isli.service.BookingService;
import com.isli.service.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingFlowIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private WaitlistService waitlistService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Event publishedEvent;
    private User attendee;

    @BeforeEach
    void seedData() {
        User organizer = userRepository.save(User.builder()
                .username("organizer-it").password(passwordEncoder.encode("pass12345"))
                .email("organizer-it@example.com").role(Role.ORGANIZER).active(true).build());

        attendee = userRepository.save(User.builder()
                .username("attendee-it").password(passwordEncoder.encode("pass12345"))
                .email("attendee-it@example.com").role(Role.ATTENDEE).active(true).build());

        Venue venue = venueRepository.save(Venue.builder()
                .name("IT Test Hall").address("Rr. Test").city("Tirana").capacity(50).build());

        Category category = categoryRepository.save(Category.builder().name("IT-Music").build());

        publishedEvent = eventRepository.save(Event.builder()
                .title("Integration Test Concert")
                .description("Seeded for integration test")
                .startDateTime(LocalDateTime.now().plusDays(10))
                .endDateTime(LocalDateTime.now().plusDays(10).plusHours(2))
                .price(BigDecimal.valueOf(25))
                .totalSeats(2)
                .availableSeats(2)
                .status(EventStatus.PUBLISHED)
                .venue(venue)
                .organizer(organizer)
                .categories(Set.of(category))
                .build());
    }

    @Test
    void bookingFlowDecrementsSeatsAndPersistsAcrossRealDatabase() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .eventId(publishedEvent.getId()).seatsBooked(2).build();

        var response = bookingService.book(request, "attendee-it");

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        Event reloaded = eventRepository.findById(publishedEvent.getId()).orElseThrow();
        assertThat(reloaded.getAvailableSeats()).isZero();

        Booking persisted = bookingRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getUser().getUsername()).isEqualTo("attendee-it");
        assertThat(persisted.getSeatsBooked()).isEqualTo(2);
    }

    @Test
    void bookingFlowRejectsOverbookingAgainstRealSeatCount() {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .eventId(publishedEvent.getId()).seatsBooked(5).build();

        assertThatThrownBy(() -> bookingService.book(request, "attendee-it"))
                .isInstanceOf(SeatsUnavailableException.class);

        Event reloaded = eventRepository.findById(publishedEvent.getId()).orElseThrow();
        assertThat(reloaded.getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void cancellationFlowRestoresSeatsAndPromotesWaitlistedUser() {
        User secondAttendee = userRepository.save(User.builder()
                .username("waiter-it").password(passwordEncoder.encode("pass12345"))
                .email("waiter-it@example.com").role(Role.ATTENDEE).active(true).build());

        var firstBooking = bookingService.book(
                BookingRequestDTO.builder().eventId(publishedEvent.getId()).seatsBooked(2).build(),
                "attendee-it");

        WaitlistDTO waitlistEntry = waitlistService.join(publishedEvent.getId(), "waiter-it");
        assertThat(waitlistEntry.getPosition()).isEqualTo(1L);

        bookingService.cancel(firstBooking.getId(), "attendee-it", false);

        Event reloaded = eventRepository.findById(publishedEvent.getId()).orElseThrow();
        assertThat(reloaded.getAvailableSeats()).isEqualTo(1);

        boolean waiterNowHasConfirmedBooking = bookingRepository
                .findByUserId(secondAttendee.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED && b.getEvent().getId().equals(publishedEvent.getId()));
        assertThat(waiterNowHasConfirmedBooking).isTrue();
    }
}