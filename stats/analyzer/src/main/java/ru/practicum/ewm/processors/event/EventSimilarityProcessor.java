package ru.practicum.ewm.processors.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.handler.event.EventSimilarityHandler;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    private final Consumer<Long, EventSimilarityAvro> consumer;
    private final EventSimilarityHandler handler;
    @Value("${kafka.topics.events-similarity}")
    private String topic;
    @Value("${kafka.properties.consumer.poll-timeout}")
    private int pollTimeout;

    public void start() {
        try {
            consumer.subscribe(List.of(topic));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(pollTimeout));

                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    EventSimilarityAvro eventSimilarity = record.value();
                    handler.handle(eventSimilarity);
                }

                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка чтения данных из топика {}", topic);
        } finally {
            try {
                consumer.commitAsync();
            } finally {
                consumer.close();
            }
        }
    }
}
