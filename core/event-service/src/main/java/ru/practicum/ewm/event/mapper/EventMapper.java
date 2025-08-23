package ru.practicum.ewm.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.dto.category.CategoryDto;
import ru.practicum.ewm.dto.event.Location;
import ru.practicum.ewm.dto.user.UserDto;
import ru.practicum.ewm.dto.user.UserShortDto;
import ru.practicum.ewm.enums.event.State;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.dto.event.EventShortDto;
import ru.practicum.ewm.dto.event.NewEventDto;
import ru.practicum.ewm.event.model.Event;



import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {
    public Event mapToEvent(NewEventDto eventDto, Long catId, Long initiatorId) {
        return Event.builder()
                .eventDate(eventDto.getEventDate())
                .annotation(eventDto.getAnnotation())
                .paid(eventDto.getPaid())
                .categoryId(catId)
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

    public EventFullDto mapToFullDto(Event event, Double rating, UserDto userDto, CategoryDto categoryDto) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(categoryDto)
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
                .rating(rating)
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .commenting(event.getCommenting())
                .build();
    }

    public EventShortDto mapToShortDto(Event event, Double rating, UserDto userDto, CategoryDto categoryDto) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(categoryDto)
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
                .rating(rating)
                .commenting(event.getCommenting())
                .build();
    }
}
