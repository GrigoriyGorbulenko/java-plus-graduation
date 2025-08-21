package ru.practicum.ewm.handler.user;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionAnalyzerHandler {
    void handle(UserActionAvro action);
}
