package ru.practicum.exception;

import org.springframework.http.HttpStatusCode;

public class RestClientRuntimeException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public RestClientRuntimeException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
