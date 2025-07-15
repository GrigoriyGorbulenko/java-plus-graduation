package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Embedded;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.UpdateObject;
import ru.practicum.enums.event.StateAction;


import java.time.LocalDateTime;

import static ru.practicum.utils.date.DateTimeFormat.TIME_PATTERN;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventAdminRequest {

    @Size(min = 20, max = 2000, message = "Для описания требуется от 20 до 2000 символов.", groups = {UpdateObject.class})
    String annotation;

    Long category;

    @Size(min = 20, max = 7000, message = "Для описания требуется от 20 до 7000 символов.", groups = {UpdateObject.class})
    String description;

    @JsonFormat(pattern = TIME_PATTERN)
    LocalDateTime eventDate;

    @Embedded
    Location location;

    Boolean paid;

    @PositiveOrZero
    Integer participantLimit;

    Boolean requestModeration;

    StateAction stateAction;

    @Size(min = 3, max = 120, message = "Для заголовка требуется от 3 до 120 символов.", groups = {UpdateObject.class})
    String title;
    Boolean commenting;
}
