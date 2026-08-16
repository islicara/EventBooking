package com.isli.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public class ReviewRequestDTO {
    @NotNull
    private Long eventId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 1000)
    private String comment;

    public ReviewRequestDTO() {}

    public ReviewRequestDTO(Long eventId, Integer rating, String comment) {
        this.eventId = eventId;
        this.rating = rating;
        this.comment = comment;
    }

    public @NotNull Long getEventId() {
        return eventId;
    }

    public void setEventId(@NotNull Long eventId) {
        this.eventId = eventId;
    }

    public @NotNull @Min(1) @Max(5) Integer getRating() {
        return rating;
    }

    public void setRating(@NotNull @Min(1) @Max(5) Integer rating) {
        this.rating = rating;
    }

    public @Size(max = 1000) String getComment() {
        return comment;
    }

    public void setComment(@Size(max = 1000) String comment) {
        this.comment = comment;
    }
}
