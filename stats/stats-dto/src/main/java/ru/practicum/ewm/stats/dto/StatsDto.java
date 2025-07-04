package ru.practicum.ewm.stats.dto;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class StatsDto {
     String app;
     String uri;
     Long hits;
}
