package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.mapper.EventSimilarityMapper;
import ru.practicum.ewm.model.EventSimilarity;
import ru.practicum.ewm.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventSimilarityHandlerImpl implements EventSimilarityHandler {
    private final EventSimilarityRepository repository;

    @Transactional
    @Override
    public void handle(EventSimilarityAvro eventSimilarity) {
        Long eventA = eventSimilarity.getEventA();
        Long eventB = eventSimilarity.getEventB();

        if (!repository.existsByEventAAndEventB(eventA, eventB)) {
            repository.save(EventSimilarityMapper.mapToEventSimilarity(eventSimilarity));
            log.info("Успешно сохранили event similarity {}", eventSimilarity);
        } else {
            EventSimilarity oldEventSimilarity = repository.findByEventAAndEventB(eventA, eventB);
            oldEventSimilarity.setScore(eventSimilarity.getScore());
            oldEventSimilarity.setTimestamp(eventSimilarity.getTimestamp());
        }
    }
}
