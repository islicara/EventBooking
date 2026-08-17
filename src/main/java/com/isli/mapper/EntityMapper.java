package com.isli.mapper;

import com.isli.dto.*;
import com.isli.entity.*;

import java.util.stream.Collectors;

public final class EntityMapper {

    private EntityMapper() {
    }

    public static UserDTO toResponse(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    public static VenueDTO toResponse(Venue venue) {
        return VenueDTO.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .capacity(venue.getCapacity())
                .build();
    }

    public static CategoryDTO toResponse(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static EventDTO toResponse(Event event, Double averageRating) {
        return EventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .price(event.getPrice())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .status(event.getStatus())
                .venue(toResponse(event.getVenue()))
                .organizer(toResponse(event.getOrganizer()))
                .categories(event.getCategories().stream().map(EntityMapper::toResponse).collect(Collectors.toSet()))
                .averageRating(averageRating)
                .build();
    }

    public static BookingDTO toResponse(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .seatsBooked(booking.getSeatsBooked())
                .status(booking.getStatus())
                .bookingDate(booking.getBookingDate())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .build();
    }

    public static ReviewDTO toResponse(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .eventId(review.getEvent().getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .build();
    }

    public static WaitlistDTO toResponse(Waitlist waitlist) {
        return WaitlistDTO.builder()
                .id(waitlist.getId())
                .status(waitlist.getStatus())
                .position(waitlist.getPosition())
                .eventId(waitlist.getEvent().getId())
                .userId(waitlist.getUser().getId())
                .build();
    }
}
