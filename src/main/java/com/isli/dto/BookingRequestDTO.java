package com.isli.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public class BookingRequestDTO {
    @NotNull
    private Long eventId;

    @NotNull
    @Min(1)
    private Integer seatsBooked;

    public BookingRequestDTO() {}

    public BookingRequestDTO(Long eventId, Integer seatsBooked) {
        this.eventId = eventId;
        this.seatsBooked = seatsBooked;
    }

    public @NotNull Long getEventId() {
        return eventId;
    }

    public void setEventId(@NotNull Long eventId) {
        this.eventId = eventId;
    }

    public @NotNull @Min(1) Integer getSeatsBooked() {
        return seatsBooked;
    }

    public void setSeatsBooked(@NotNull @Min(1) Integer seatsBooked) {
        this.seatsBooked = seatsBooked;
    }
}
