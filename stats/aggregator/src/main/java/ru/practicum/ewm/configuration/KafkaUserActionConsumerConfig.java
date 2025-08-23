package ru.practicum.ewm.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaUserActionConsumerConfig {
    private final Environment env;

    @Bean
    public Consumer<Long, UserActionAvro> kafkaConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.CLIENT_ID_CONFIG, env.getProperty("kafka.properties.consumer.client-id"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, env.getProperty("kafka.properties.consumer.group-id"));
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("kafka.bootstrap-servers"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                env.getProperty("kafka.properties.consumer.key-deserializer"));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                env.getProperty("kafka.properties.consumer.value-deserializer"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                env.getProperty("kafka.properties.consumer.enable-auto-commit"));

        return new KafkaConsumer<>(config);
    }
}
