package ru.practicum.ewm.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.model.UserAction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@UtilityClass
public class UserActionMapper {

    public UserAction mapToUserAction(UserActionAvro actionAvro) {
        return UserAction.builder()
                .eventId(actionAvro.getEventId())
                .userId(actionAvro.getUserId())
                .mark(mapToMark(actionAvro.getActionType()))
                .timestamp(actionAvro.getTimestamp())
                .build();
    }

    private Float mapToMark(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4f;
            case REGISTER -> 0.8f;
            case LIKE -> 1.0f;
        };
    }
}
