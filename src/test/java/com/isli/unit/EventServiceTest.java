package com.isli.unit;

import com.isli.dto.EventDTO;
import com.isli.dto.EventRequestDTO;
import com.isli.entity.Event;
import com.isli.entity.EventStatus;
import com.isli.entity.User;
import com.isli.entity.Venue;
import com.isli.exception.ForbiddenOperationException;
import com.isli.exception.InvalidStateException;
import com.isli.repository.CategoryRepository;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.VenueRepository;
import com.isli.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private Event draftEvent;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).username("organizer1").build();
        Venue venue = Venue.builder().id(1L).name("Palace").address("Rr. X").city("Tirana").capacity(200).build();

        draftEvent = Event.builder()
                .id(10L).title("Concert")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .endDateTime(LocalDateTime.now().plusDays(5).plusHours(2))
                .price(BigDecimal.TEN).totalSeats(100).availableSeats(100)
                .status(EventStatus.DRAFT).venue(venue).organizer(organizer)
                .categories(Set.of())
                .build();
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
        EventRequestDTO request = EventRequestDTO.builder()
                .title("Bad Event")
                .startDateTime(LocalDateTime.now().plusDays(5))
                .endDateTime(LocalDateTime.now().plusDays(4))
                .price(BigDecimal.TEN).totalSeats(10).venueId(1L)
                .categoryId(Set.of(1L))
                .build();

        assertThatThrownBy(() -> eventService.create(request, "organizer1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publishRejectsWhenEventNotOwnedByRequester() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.publish(10L, "someone-else"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void publishRejectsWhenEventAlreadyPublished() {
        draftEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.publish(10L, "organizer1"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void publishMovesDraftEventToPublished() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(draftEvent));
        when(eventRepository.findAverageRating(10L)).thenReturn(null);

        EventDTO result = eventService.publish(10L, "organizer1");

        assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void cancelRejectsWhenAlreadyCancelled() {
        draftEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> eventService.cancel(10L, "organizer1", false))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void cancelAllowsAdminRegardlessOfOwnership() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(draftEvent));
        when(eventRepository.findAverageRating(10L)).thenReturn(4.5);

        EventDTO result = eventService.cancel(10L, "some-admin", true);

        assertThat(result.getStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(result.getAverageRating()).isEqualTo(4.5);
    }
}