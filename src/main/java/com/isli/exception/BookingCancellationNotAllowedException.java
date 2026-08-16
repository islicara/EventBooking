package com.isli.exception;

public class BookingCancellationNotAllowedException extends RuntimeException {
    public BookingCancellationNotAllowedException(String message) {
        super(message);
    }
}
