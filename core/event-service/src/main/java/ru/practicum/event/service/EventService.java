package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;


import java.util.List;

public interface EventService {
    EventFullDto addEvent(NewEventDto eventDto, Long userId);

    List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size);

    EventFullDto getEventOfUser(Long userId, Long eventId);

    EventFullDto updateEventOfUser(UpdateEventUserRequest updateRequest, Long userId, Long eventId);

    List<EventShortDto> getPublicEventsByFilter(HttpServletRequest httpServletRequest, EventPublicFilter inputFilter);

    EventFullDto getPublicEventById(HttpServletRequest httpServletRequest, Long id);

    List<EventFullDto> getEventsForAdmin(EventAdminFilter admin);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<ParticipationRequestDto> getRequestsOfUserEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(EventRequestStatusUpdateRequest updateRequest, Long userId,
                                                        Long eventId);

    EventFullDto findEventById(Long eventId);

    @Transactional
    void updateConfirmedRequests(Long eventId, Integer confirmedRequests);

    boolean checkExistsById(Long eventId);

    void deleteEventsByUser(Long userId);
}
