package ru.practicum.ewm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.grpc.stats.event.ActionTypeProto;
import ru.practicum.ewm.grpc.stats.event.UserActionProto;
import ru.practicum.ewm.producer.KafkaUserActionProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {
    private final KafkaUserActionProducer producer;
    @Value("${kafka.topic}")
    private String topic;

    @Override
    public void handle(UserActionProto userActionProto) {
        log.info("Отправляем сообщение {} в topic {}", userActionProto, topic);
        producer.send(toAvro(userActionProto), userActionProto.getEventId(),
                mapToInstant(userActionProto), topic);
    }

    @Override
    public UserActionAvro toAvro(UserActionProto userActionProto) {
        return UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(getActionType(userActionProto.getActionType()))
                .setTimestamp(mapToInstant(userActionProto))
                .build();
    }

    private ActionTypeAvro getActionType(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> null;
        };
    }

    private Instant mapToInstant(UserActionProto userActionProto) {
        return Instant.ofEpochSecond(userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos());
    }
}
