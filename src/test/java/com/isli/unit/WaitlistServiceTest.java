package com.isli.unit;

import com.isli.dto.WaitlistDTO;
import com.isli.entity.Event;
import com.isli.entity.User;
import com.isli.entity.Waitlist;
import com.isli.entity.WaitlistStatus;
import com.isli.exception.DuplicateResourceException;
import com.isli.exception.InvalidStateException;
import com.isli.repository.EventRepository;
import com.isli.repository.UserRepository;
import com.isli.repository.WaitlistRepository;
import com.isli.service.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WaitlistService waitlistService;

    private User user;
    private Event fullEvent;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("cara").build();
        fullEvent = Event.builder().id(10L).availableSeats(0).build();
    }

    @Test
    void joinRejectsWhenEventStillHasSeats() {
        Event openEvent = Event.builder().id(10L).availableSeats(5).build();
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(openEvent));

        assertThatThrownBy(() -> waitlistService.join(10L, "cara"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void joinRejectsWhenAlreadyOnWaitlist() {
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(fullEvent));
        when(waitlistRepository.existsByUserIdAndEventIdAndStatus(1L, 10L, WaitlistStatus.WAITING))
                .thenReturn(true);

        assertThatThrownBy(() -> waitlistService.join(10L, "cara"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void joinAssignsNextPositionInLine() {
        when(userRepository.findByUsername("cara")).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(fullEvent));
        when(waitlistRepository.existsByUserIdAndEventIdAndStatus(1L, 10L, WaitlistStatus.WAITING))
                .thenReturn(false);
        when(waitlistRepository.countByEventIdAndStatus(10L, WaitlistStatus.WAITING)).thenReturn(2L);
        when(waitlistRepository.save(org.mockito.ArgumentMatchers.any(Waitlist.class)))
                .thenAnswer(inv -> {
                    Waitlist w = inv.getArgument(0);
                    w.setId(500L);
                    return w;
                });

        WaitlistDTO result = waitlistService.join(10L, "cara");

        assertThat(result.getPosition()).isEqualTo(3L);
        assertThat(result.getStatus()).isEqualTo(WaitlistStatus.WAITING);
    }
}