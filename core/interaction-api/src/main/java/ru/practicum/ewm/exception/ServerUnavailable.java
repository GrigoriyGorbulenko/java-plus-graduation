package ru.practicum.ewm.exception;

public class ServerUnavailable extends RuntimeException {
    public ServerUnavailable(String message) {
        super(message);
    }
}
