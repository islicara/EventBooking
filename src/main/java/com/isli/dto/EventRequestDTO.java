package com.isli.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
public class EventRequestDTO {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @NotNull
    private Long venueId;

    @NotEmpty
    private Set<Long> categoryId;

    public EventRequestDTO() {}

    public EventRequestDTO(String title, String description, LocalDateTime startDateTime, LocalDateTime endDateTime, BigDecimal price, Integer totalSeats, Long venueId, Set<Long> categoryId) {
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.price = price;
        this.totalSeats = totalSeats;
        this.venueId = venueId;
        this.categoryId = categoryId;
    }

    public @NotBlank String getTitle() {
        return title;
    }

    public void setTitle(@NotBlank String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public @NotNull LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(@NotNull LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public @NotNull LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(@NotNull LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price) {
        this.price = price;
    }

    public @NotNull @Min(1) Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(@NotNull @Min(1) Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public @NotNull Long getVenueId() {
        return venueId;
    }

    public void setVenueId(@NotNull Long venueId) {
        this.venueId = venueId;
    }

    public @NotEmpty Set<Long> getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(@NotEmpty Set<Long> categoryId) {
        this.categoryId = categoryId;
    }
}
