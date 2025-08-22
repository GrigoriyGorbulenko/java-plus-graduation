package ru.practicum.ewm.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import org.apache.kafka.clients.consumer.Consumer;

import java.util.Properties;
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {
    private final Environment environment;

    @Bean
    public Consumer<Long, UserActionAvro> userActionConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.CLIENT_ID_CONFIG, environment.getProperty("kafka.properties.consumer.user-action-client-id"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("kafka.properties.consumer.user-action-group-id"));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("kafka.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                environment.getProperty("kafka.properties.consumer.key-deserializer"));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                environment.getProperty("kafka.properties.consumer.user-action-deserializer"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                environment.getProperty("kafka.properties.consumer.enable-auto-commit"));

        return new KafkaConsumer<>(config);
    }

    @Bean
    public Consumer<Long, EventSimilarityAvro> eventSimilarityConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.CLIENT_ID_CONFIG, environment.getProperty("kafka.properties.consumer.similarity-client-id"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, environment.getProperty("kafka.properties.consumer.similarity-group-id"));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty("kafka.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                environment.getProperty("kafka.properties.consumer.key-deserializer"));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                environment.getProperty("kafka.properties.consumer.event-similarity-deserializer"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                environment.getProperty("kafka.properties.consumer.enable-auto-commit"));

        return new KafkaConsumer<>(config);
    }
}
