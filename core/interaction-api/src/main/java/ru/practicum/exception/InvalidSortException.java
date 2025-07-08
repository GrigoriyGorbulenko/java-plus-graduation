package ru.practicum.exception;

public class InvalidSortException extends RuntimeException {
    public InvalidSortException(String message) {
        super(message);
    }
}
