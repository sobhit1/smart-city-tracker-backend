package com.project.smart_city_tracker_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    /**
     * Constructs a new BadRequestException with the specified detail message.
     *
     * @param message The detail message explaining why the exception was thrown.
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new BadRequestException with the specified detail message and cause.
     *
     * @param message The detail message explaining why the exception was thrown.
     * @param cause   The cause of the exception (can be null).
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}