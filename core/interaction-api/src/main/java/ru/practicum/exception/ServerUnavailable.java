package ru.practicum.exception;

public class ServerUnavailable extends RuntimeException {
    public ServerUnavailable(String message) {
        super(message);
    }
}
