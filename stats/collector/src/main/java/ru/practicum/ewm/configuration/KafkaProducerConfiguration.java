package ru.practicum.ewm.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfiguration {
    private final Environment env;

    @Bean
    public Producer<Long, SpecificRecordBase> getProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                env.getProperty("kafka.producer.properties.bootstrap-servers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                env.getProperty("kafka.producer.properties.key-serializer"));
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                env.getProperty("kafka.producer.properties.value-serializer"));

        return new KafkaProducer<>(config);
    }
}
