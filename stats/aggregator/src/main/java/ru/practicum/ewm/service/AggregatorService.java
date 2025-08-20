package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.handler.UserActionHandler;
import ru.practicum.ewm.producer.KafkaEventSimilarityProducer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregatorService {
    private final Consumer<Long, UserActionAvro> consumer;
    @Value("${kafka.topics.user-action}")
    private String topic;
    @Value("${kafka.properties.consumer.poll-timeout}")
    private int pollTimeout;
    private final UserActionHandler handler;
    private final KafkaEventSimilarityProducer producer;
    @Value("${kafka.topics.events-similarity}")
    private String similarityTopic;

    public void start() {
        try {
            consumer.subscribe(List.of(topic));

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(pollTimeout));

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    UserActionAvro action = record.value();
                    log.info("обрабатываем действие пользователя {}", action);

                    List<EventSimilarityAvro> result = handler.calcSimilarity(action);
                    log.info("Получили список коэффициентов схожести {}", result);
                    producer.send(result, similarityTopic);
                    producer.flush();
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от пользователей", e);
        } finally {
            try {
                producer.flush();
                consumer.commitAsync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                producer.close();
            }
        }
    }
}
