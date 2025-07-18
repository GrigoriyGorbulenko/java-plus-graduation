package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.Location;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.event.State;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.event.model.Event;



import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {
    public Event mapToEvent(NewEventDto eventDto, Category category, Long initiatorId) {
        return Event.builder()
                .eventDate(eventDto.getEventDate())
                .annotation(eventDto.getAnnotation())
                .paid(eventDto.getPaid())
                .category(category)
                .confirmedRequests(0)
                .createdOn(LocalDateTime.now())
                .description(eventDto.getDescription())
                .state(State.PENDING)
                .title(eventDto.getTitle())
                .lat(eventDto.getLocation().getLat())
                .lon(eventDto.getLocation().getLon())
                .participantLimit(eventDto.getParticipantLimit())
                .requestModeration(eventDto.getRequestModeration())
                .initiatorId(initiatorId)
                .commenting(eventDto.getCommenting())
                .build();
    }

    public EventFullDto mapToFullDto(Event event, Long views, UserDto userDto) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserShortDto.builder()
                        .name(userDto.getName())
                        .id(userDto.getId())
                        .build())
                .location(Location.builder().lat(event.getLat()).lon(event.getLon()).build())
                .paid(event.getPaid())
                .views(views)
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .commenting(event.getCommenting())
                .build();
    }

    public EventShortDto mapToShortDto(Event event, Long views, UserDto userDto) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .publishedOn(event.getPublishedOn())
                .id(event.getId())
                .initiator(UserShortDto.builder()
                        .name(userDto.getName())
                        .id(userDto.getId())
                        .build())
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .commenting(event.getCommenting())
                .build();
    }
}
