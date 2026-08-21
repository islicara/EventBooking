package com.isli.unit;

import com.isli.dto.BookingDTO;
import com.isli.dto.BookingRequestDTO;
import com.isli.entity.Booking;
import com.isli.entity.BookingStatus;
import com.isli.entity.Event;
import com.isli.entity.EventStatus;
import com.isli.entity.User;
import com.isli.entity.Venue;
import com.isli.entity.Waitlist;
import com.isli.entity.WaitlistStatus;
import com.isli.exception.BookingCancellationNotAllowedException;
import com.isli.exception.ForbiddenOperationException;
import com.isli.exception.InvalidStateException;
import com.isli.exception.SeatsUnavailableException;
import com.isli.repository.BookingRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.WaitlistRepository;
import com.isli.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WaitlistRepository waitlistRepository;

    @InjectMocks
    private BookingService bookingService;

    private User attendee;
    private Event event;

    @BeforeEach
    void setUp() {
        attendee = User.builder().id(1L).username("cara").build();

        Venue venue = Venue.builder().id(1L).name("Palace").address("Rr. X").city("Tirana").capacity(200).build();
        User organizer = User.builder().id(2L).username("organizer1").build();

        event = Event.builder()
                .id(10L)
                .title("Concert")
                .startDateTime(LocalDateTime.now().plusDays(10))
                .endDateTime(LocalDateTime.now().plusDays(10).plusHours(2))
                .price(BigDecimal.TEN)
                .totalSeats(100)
                .availableSeats(5)
                .status(EventStatus.PUBLISHED)
                .venue(venue)
                .organizer(organizer)
                .build();
    }

    @Test
    void bookDecrementsAvailableSeatsAndPersistsConfirmedBooking() {
        BookingRequestDTO request = BookingRequestDTO.builder().eventId(10L).seatsBooked(2).build();
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDTO result = bookingService.book(request, "cara");

        assertThat(event.getAvailableSeats()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getSeatsBooked()).isEqualTo(2);
    }

    @Test
    void bookRejectsWhenNotEnoughSeats() {
        BookingRequestDTO request = BookingRequestDTO.builder().eventId(10L).seatsBooked(10).build();
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> bookingService.book(request, "cara"))
                .isInstanceOf(SeatsUnavailableException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookRejectsWhenEventNotPublished() {
        event.setStatus(EventStatus.DRAFT);
        BookingRequestDTO request = BookingRequestDTO.builder().eventId(10L).seatsBooked(1).build();
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> bookingService.book(request, "cara"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cancelRejectsWithinCancellationCutoff() {
        event.setStartDateTime(LocalDateTime.now().plusHours(2));
        Booking booking = Booking.builder()
                .id(100L).seatsBooked(2).status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now()).user(attendee).event(event)
                .build();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(100L, "cara", false))
                .isInstanceOf(BookingCancellationNotAllowedException.class);
    }

    @Test
    void cancelRejectsWhenNotOwnerAndNotAdmin() {
        Booking booking = Booking.builder()
                .id(100L).seatsBooked(2).status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now()).user(attendee).event(event)
                .build();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(100L, "someone-else", false))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void cancelRestoresSeatsAndPromotesNextWaitlistEntry() {
        Booking booking = Booking.builder()
                .id(100L).seatsBooked(2).status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now()).user(attendee).event(event)
                .build();
        User waitingUser = User.builder().id(3L).username("waiting-guy").build();
        Waitlist waitlistEntry = Waitlist.builder()
                .id(500L).status(WaitlistStatus.WAITING).position(1L)
                .user(waitingUser).event(event)
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(waitlistRepository.findByEventIdAndStatus(10L, WaitlistStatus.WAITING))
                .thenReturn(Optional.of(waitlistEntry));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancel(100L, "cara", false);

        assertThat(event.getAvailableSeats()).isEqualTo(6);
        assertThat(waitlistEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        ArgumentCaptor<Booking> savedBookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(1)).save(savedBookingCaptor.capture());
        assertThat(savedBookingCaptor.getValue().getUser()).isEqualTo(waitingUser);
        assertThat(savedBookingCaptor.getValue().getSeatsBooked()).isEqualTo(1);
    }

    @Test
    void cancelAdminBypassesCancellationCutoff() {
        event.setStartDateTime(LocalDateTime.now().plusHours(1));
        Booking booking = Booking.builder()
                .id(100L).seatsBooked(1).status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now()).user(attendee).event(event)
                .build();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        BookingDTO result = bookingService.cancel(100L, "admin-user", true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
}