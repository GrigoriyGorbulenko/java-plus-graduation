package ru.practicum.ewm.handler.event;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityHandler {
    void handle(EventSimilarityAvro eventSimilarity);
}
