package ru.practicum.exception;

public class SaveStatsException extends RuntimeException {
    public SaveStatsException(String message) {
        super(message);
    }
}
