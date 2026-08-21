package com.isli.unit;

import com.isli.dto.ReviewDTO;
import com.isli.dto.ReviewRequestDTO;
import com.isli.entity.Booking;
import com.isli.entity.BookingStatus;
import com.isli.entity.Event;
import com.isli.entity.User;
import com.isli.exception.DuplicateResourceException;
import com.isli.exception.ReviewNotAllowedException;
import com.isli.repository.BookingRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.ReviewRepository;
import com.isli.repository.UserRepository;
import com.isli.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Event pastEvent;
    private ReviewRequestDTO request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("cara").build();
        pastEvent = Event.builder()
                .id(10L).title("Past Concert")
                .startDateTime(LocalDateTime.now().minusDays(3))
                .endDateTime(LocalDateTime.now().minusDays(2))
                .build();
        request = ReviewRequestDTO.builder().eventId(10L).rating(5).comment("Great show").build();
    }

    @Test
    void createRejectsWithoutConfirmedBooking() {
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(pastEvent));
        when(bookingRepository.findByUserIdAndEventIdAndStatus(1L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(request, "cara"))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void createRejectsWhenEventHasNotHappenedYet() {
        Event futureEvent = Event.builder()
                .id(10L)
                .startDateTime(LocalDateTime.now().plusDays(3))
                .endDateTime(LocalDateTime.now().plusDays(3).plusHours(2))
                .build();
        Booking booking = Booking.builder().id(50L).user(user).event(futureEvent).status(BookingStatus.CONFIRMED).build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(futureEvent));
        when(bookingRepository.findByUserIdAndEventIdAndStatus(1L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.create(request, "cara"))
                .isInstanceOf(ReviewNotAllowedException.class);
    }

    @Test
    void createRejectsDuplicateReview() {
        Booking booking = Booking.builder().id(50L).user(user).event(pastEvent).status(BookingStatus.CONFIRMED).build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(pastEvent));
        when(bookingRepository.findByUserIdAndEventIdAndStatus(1L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(Optional.of(booking));
        when(reviewRepository.existsByUserIdAndEventId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(request, "cara"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createSavesReviewWhenEligible() {
        Booking booking = Booking.builder().id(50L).user(user).event(pastEvent).status(BookingStatus.CONFIRMED).build();

        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(pastEvent));
        when(bookingRepository.findByUserIdAndEventIdAndStatus(1L, 10L, BookingStatus.CONFIRMED))
                .thenReturn(Optional.of(booking));
        when(reviewRepository.existsByUserIdAndEventId(1L, 10L)).thenReturn(false);
        when(reviewRepository.save(any())).thenAnswer(inv -> {
            var review = inv.getArgument(0, com.isli.entity.Review.class);
            review.setId(99L);
            return review;
        });

        ReviewDTO result = reviewService.create(request, "cara");

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getEventId()).isEqualTo(10L);
    }
}