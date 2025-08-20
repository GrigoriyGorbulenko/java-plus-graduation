package ru.practicum.ewm.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.handler.UserActionAnalyzerHandler;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor implements Runnable {
    private final Consumer<Long, UserActionAvro> consumer;
    private final UserActionAnalyzerHandler handler;
    @Value("${kafka.topics.user-action}")
    private String topic;
    @Value("${kafka.properties.consumer.poll-timeout}")
    private int pollTimeout;

    @Override
    public void run() {
        try {
            consumer.subscribe(List.of(topic));

            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(pollTimeout));

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    UserActionAvro action = record.value();
                    log.info("Получили действие пользователя {}", action);

                    handler.handle(action);
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
