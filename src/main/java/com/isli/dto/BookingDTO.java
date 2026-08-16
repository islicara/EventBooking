package com.isli.dto;

import com.isli.entity.BookingStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class BookingDTO {
    private Long id;
    private Integer seatsBooked;
    private BookingStatus status;
    private LocalDateTime bookingDate;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String username;

    public BookingDTO() {}

    public BookingDTO(Long id, Integer seatsBooked, BookingStatus status, LocalDateTime bookingDate, Long eventId, String eventTitle, Long userId, String username) {
        this.id = id;
        this.seatsBooked = seatsBooked;
        this.status = status;
        this.bookingDate = bookingDate;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.userId = userId;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSeatsBooked() {
        return seatsBooked;
    }

    public void setSeatsBooked(Integer seatsBooked) {
        this.seatsBooked = seatsBooked;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
