package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.stats.EndpointHitDto;
import ru.practicum.dto.stats.StatsDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enums.event.State;
import ru.practicum.enums.event.StateAction;
import ru.practicum.enums.request.Status;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;

import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;

import ru.practicum.feign.ParticipationRequestClient;
import ru.practicum.feign.StatClient;
import ru.practicum.feign.UserClient;

import ru.practicum.exception.*;



import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final CategoryRepository categoryRepository;

    private final UserClient userClient;

    private final ParticipationRequestClient requestClient;

    private final StatClient statClient;

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    @Transactional
    public EventFullDto addEvent(NewEventDto eventDto, Long userId) {
        checkFields(eventDto);

        Category category = categoryRepository.findById(eventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        UserDto user = userClient.findById(userId);

        if (user.getName().equals("UNKNOWN")) {
            throw new ServerUnavailable("User Server unavailable");
        }

        if (eventDto.getCommenting() == null) {
            eventDto.setCommenting(true);
        }
        Event event = eventRepository.save(EventMapper.mapToEvent(eventDto, category, userId));
        return EventMapper.mapToFullDto(event, 0L, user);
    }

    @Override
    public List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size) {
        UserDto user = userClient.findById(userId);

        Sort sortByCreatedDate = Sort.by("createdOn");
        PageRequest pageRequest = PageRequest.of(from / size, size, sortByCreatedDate);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageRequest);

        List<String> urisList = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();


        List<StatsDto> statsList = statClient.getStats(events.getFirst().getCreatedOn().minusSeconds(1),
                LocalDateTime.now(), urisList, false);

        return events.stream().map(event -> {
                    Optional<StatsDto> result = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + event.getId()))
                            .findFirst();
                    if (result.isPresent()) {
                        return EventMapper.mapToShortDto(event, result.get().getHits(), user);
                    } else {
                        return EventMapper.mapToShortDto(event, 0L, user);
                    }
                })
                .toList();
    }

    @Override
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        UserDto user = userClient.findById(userId);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Можно просмотреть только своё событие");
        }
        String uri = "/events/" + eventId;
        List<StatsDto> statsList = statClient.getStats(event.getCreatedOn().minusSeconds(1), LocalDateTime.now(),
                List.of(uri), false);
        Optional<StatsDto> result = statsList.stream().findFirst();
        if (result.isPresent()) {
            return EventMapper.mapToFullDto(event, result.get().getHits(), user);
        } else {
            return EventMapper.mapToFullDto(event, 0L, user);
        }
    }

    @Override
    @Transactional
    public EventFullDto updateEventOfUser(UpdateEventUserRequest updateRequest, Long userId, Long eventId) {
        UserDto user = userClient.findById(userId);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Можно просмотреть только своё событие");
        }

        if (event.getState() == State.PUBLISHED) {
            throw new ConflictDataException("Нельзя изменить опубликованное событие");
        }

        if (updateRequest.getAnnotation() != null && !updateRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null && !updateRequest.getDescription().isBlank()) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            if (updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Дата начала события должна быть позже чем через 2 часа от текущего" +
                        " времени");
            }
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getCommenting() != null) {
            event.setCommenting(updateRequest.getCommenting());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(State.PENDING);
            } else if (updateRequest.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(State.CANCELED);
            }
        }

        return EventMapper.mapToFullDto(event, 0L, user);
    }

    //public Получение событий с возможностью фильтрации
    @Override
    public List<EventShortDto> getPublicEventsByFilter(HttpServletRequest httpServletRequest,
                                                       EventPublicFilter inputFilter) {
        PageRequest pageRequest = PageRequest.of(inputFilter.getFrom() / inputFilter.getSize(),
                inputFilter.getSize());

        inputFilter.setText("%" + inputFilter.getText().trim() + "%");

        BooleanExpression conditions = QEvent.event.annotation.likeIgnoreCase(inputFilter.getText())
                .or(QEvent.event.description.likeIgnoreCase(inputFilter.getText()))
                .and(QEvent.event.state.in(State.PUBLISHED));
        if (inputFilter.getCategories() != null) {
            conditions = conditions.and(QEvent.event.category.id.in(inputFilter.getCategories()));
        }
        if (inputFilter.getPaid() != null) {
            conditions.and(QEvent.event.paid.eq(inputFilter.getPaid()));
        }
        if (inputFilter.getRangeStart() != null && inputFilter.getRangeEnd() != null) {
            conditions = conditions.and(QEvent.event.eventDate.after(inputFilter.getRangeStart()))
                    .and(QEvent.event.eventDate.before(inputFilter.getRangeEnd()));
        } else {
            conditions = conditions.and(QEvent.event.eventDate.after(LocalDateTime.now()));
        }
        if (inputFilter.getOnlyAvailable()) {
            conditions = conditions.and(QEvent.event.confirmedRequests.loe(QEvent.event.participantLimit));
        }
        List<Event> events = eventRepository.findAll(conditions, pageRequest).getContent();

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> urisList = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, UserDto> users = userClient.getAllUsers(userIds, 0, userIds.size()).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
        List<StatsDto> statsList = statClient.getStats(events.getFirst().getCreatedOn().minusSeconds(1),
                LocalDateTime.now(), urisList, false);


        List<EventShortDto> result = events.stream().map(event -> {

                    Optional<StatsDto> stat = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + event.getId()))
                            .findFirst();
                    return EventMapper.mapToShortDto(event, stat.isPresent() ? stat.get().getHits() : 0L,
                            users.get(event.getInitiatorId()));
                })
                .toList();
        List<EventShortDto> resultList = new ArrayList<>(result);

        switch (inputFilter.getSort()) {
            case EVENT_DATE -> resultList.sort(Comparator.comparing(EventShortDto::getEventDate));
            case VIEWS -> resultList.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }

        var ids = resultList.stream().map(EventShortDto::getId).toList();
        Map<Long, List<ParticipationRequestDto>> confirmedRequests = requestClient.prepareConfirmedRequests(ids);

        resultList.forEach(r -> {
            var requests = confirmedRequests.get(r.getId());

            r.setConfirmedRequests(requests != null ? requests.size() : 0);
        });

        try {
            EndpointHitDto requestBody = EndpointHitDto
                    .builder().app(serviceName)
                    .ip(httpServletRequest.getRemoteAddr())
                    .uri(httpServletRequest.getRequestURI())
                    .timestamp(LocalDateTime.now())
                    .build();
            statClient.saveHit(requestBody);
            log.info("Сохранение статистики.");
        } catch (FeignException e) {
            log.error("Не удалось сохранить статистику.");
        }

        return resultList;
    }

    //public Получение подробной информации об опубликованном событии по его идентификатору
    @Override
    public EventFullDto getPublicEventById(HttpServletRequest httpServletRequest, Long id) {

        Event event = eventRepository.findById(id).orElseThrow(
                () -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", id)));

        if (event.getState() != State.PUBLISHED)
            throw new NotFoundException("Посмотреть можно только опубликованное событие.");

        UserDto user = userClient.findById(event.getInitiatorId());

        Optional<StatsDto> stat = statClient.getStats(event.getCreatedOn().minusSeconds(1),
                LocalDateTime.now(), List.of("/events/" + event.getId()), true).stream().findFirst();

        EventFullDto result = EventMapper.mapToFullDto(event, stat.isPresent() ? stat.get().getHits() : 0L, user);

        List<ParticipationRequestDto> confirmedRequests = requestClient
                .prepareConfirmedRequests(List.of(event.getId())).get(event.getId());
        result.setConfirmedRequests(confirmedRequests != null ? confirmedRequests.size() : 0);

        try {
            EndpointHitDto requestBody = EndpointHitDto
                    .builder().app(serviceName)
                    .ip(httpServletRequest.getRemoteAddr())
                    .uri(httpServletRequest.getRequestURI())
                    .timestamp(LocalDateTime.now())
                    .build();

            statClient.saveHit(requestBody);
            log.info("Сохранение статистики.");
        } catch (FeignException e) {
            log.error("Не удалось сохранить статистику.");
        }

        return result;
    }

    // admin Эндпоинт возвращает полную информацию обо всех событиях подходящих под переданные условия
    @Override
    public List<EventFullDto> getEventsForAdmin(EventAdminFilter input) {
        Sort sort = Sort.by("createdOn");
        Pageable pageable = PageRequest.of(input.getFrom() / input.getSize(), input.getSize(), sort);
        BooleanExpression conditions;
        if (input.getRangeStart() == null && input.getRangeEnd() == null) {
            conditions = QEvent.event.eventDate.after(LocalDateTime.now());
        } else {
            conditions = QEvent.event.eventDate.after(input.getRangeStart())
                    .and(QEvent.event.eventDate.before(input.getRangeEnd()));
        }
        if (input.getUsers() != null) {
            conditions = conditions.and(QEvent.event.initiatorId.in(input.getUsers()));
        }
        if (input.getStates() != null) {
            conditions = conditions.and(QEvent.event.state.in(input.getStates()));
        }
        if (input.getCategories() != null) {
            conditions = conditions.and(QEvent.event.category.id.in(input.getCategories()));
        }
        List<Event> events = eventRepository.findAll(conditions, pageable).getContent();

        List<String> urisList = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, UserDto> users = userClient.getAllUsers(userIds, 0, userIds.size()).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));

        List<StatsDto> statsList = statClient.getStats(events.getFirst().getCreatedOn().minusSeconds(1),
                LocalDateTime.now(), urisList, false);
        var ids = events.stream().map(Event::getId).toList();
        Map<Long, List<ParticipationRequestDto>> confirmedRequests = requestClient.prepareConfirmedRequests(ids);

        return events.stream().map(event -> {

                    Optional<StatsDto> stat = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + event.getId()))
                            .findFirst();
                    var requests = confirmedRequests.get(event.getId());
                    var r = EventMapper.mapToFullDto(event, stat.isPresent() ? stat.get().getHits() : 0L,
                            users.get(event.getInitiatorId()));

                    r.setConfirmedRequests(requests != null ? requests.size() : 0);
                    return r;
                })
                .toList();
    }

    // admin Редактирование данных любого события администратором. Валидация данных не требуется
    @Transactional
    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBDException(String.format("Не найдено событие в БД с ID = %d.", eventId)));

        checkStateAction(event, updateEventAdminRequest);

        if (updateEventAdminRequest.getAnnotation() != null && !updateEventAdminRequest.getAnnotation().isBlank()) {
            event.setAnnotation(updateEventAdminRequest.getAnnotation());
        }
        if (updateEventAdminRequest.getCategory() != null) {
            event.getCategory().setId(updateEventAdminRequest.getCategory());
        }
        if (updateEventAdminRequest.getDescription() != null && !updateEventAdminRequest.getDescription().isBlank()) {
            event.setDescription(updateEventAdminRequest.getDescription());
        }
        if (updateEventAdminRequest.getEventDate() != null) {
            checkDateEvent(updateEventAdminRequest.getEventDate());
            event.setEventDate(updateEventAdminRequest.getEventDate());
        }
        if (updateEventAdminRequest.getLocation() != null) {
            event.setLat(updateEventAdminRequest.getLocation().getLat());
            event.setLon(updateEventAdminRequest.getLocation().getLon());
        }
        if (updateEventAdminRequest.getPaid() != null) {
            event.setPaid(updateEventAdminRequest.getPaid());
        }
        if (updateEventAdminRequest.getCommenting() != null) {
            event.setCommenting(updateEventAdminRequest.getCommenting());
        }
        if (updateEventAdminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateEventAdminRequest.getParticipantLimit());
        }
        if (updateEventAdminRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateEventAdminRequest.getRequestModeration());
        }
        if (StateAction.REJECT_EVENT.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.CANCELED);
        }
        if (StateAction.CANCEL_REVIEW.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.CANCELED);
        }
        if (StateAction.SEND_TO_REVIEW.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.PENDING);
        }
        if (StateAction.PUBLISH_EVENT.equals(updateEventAdminRequest.getStateAction())) {
            event.setState(State.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }
        if (updateEventAdminRequest.getTitle() != null && !updateEventAdminRequest.getTitle().isBlank()) {
            event.setTitle(updateEventAdminRequest.getTitle());
        }
        event = eventRepository.save(event);

        UserDto user = userClient.findById(event.getInitiatorId());

        Optional<StatsDto> stat = statClient.getStats(event.getCreatedOn().minusSeconds(1), LocalDateTime.now(),
                List.of("/events/" + event.getId()), false).stream().findFirst();

        EventFullDto result = EventMapper.mapToFullDto(event, stat.isPresent() ? stat.get().getHits() : 0L, user);

        List<ParticipationRequestDto> confirmedRequests = requestClient
                .prepareConfirmedRequests(List.of(event.getId())).get(event.getId());
        result.setConfirmedRequests(confirmedRequests != null ? confirmedRequests.size() : 0);

        return result;

    }

    @Override
    public List<ParticipationRequestDto> getRequestsOfUserEvent(Long userId, Long eventId) {
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            log.error("userId отличается от id создателя события");
            throw new ValidationException("Событие должно быть создано текущим пользователем");
        }
        return requestClient.findAllByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestsStatus(EventRequestStatusUpdateRequest updateRequest,
                                                               Long userId, Long eventId) {
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Событие должно быть создано текущим пользователем");
        }
        if (Objects.equals(event.getConfirmedRequests(), event.getParticipantLimit())) {
            throw new ConflictDataException("Лимит участников уже исчерпан");
        }
        List<Long> requestIds = updateRequest.getRequestIds();
        log.info("Получили список id запросов на участие: {}", requestIds);
        List<ParticipationRequestDto> requestList = requestClient.findAllById(requestIds);
        if (requestList.stream()
                .anyMatch(request -> !Objects.equals(request.getEvent(), eventId))) {
            throw new ValidationException("Все запросы должны принадлежать одному событию");
        }
        List<ParticipationRequestDto> confirmedRequestsList = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        if (event.getRequestModeration() && event.getParticipantLimit() != 0) {
            switch (updateRequest.getStatus()) {
                case CONFIRMED -> requestList.forEach(request -> {
                    if (request.getStatus() != Status.PENDING) {
                        throw new ConflictDataException("Можно изменить только статус PENDING");
                    }
                    if (Objects.equals(event.getConfirmedRequests(), event.getParticipantLimit())) {
                        request.setStatus(Status.REJECTED);
                        requestClient.updateRequestStatus(request.getId(), String.valueOf(Status.REJECTED));
                        rejectedRequests.add(request);
                    } else {
                        request.setStatus(Status.CONFIRMED);
                        requestClient.updateRequestStatus(request.getId(), String.valueOf(Status.CONFIRMED));
                        Integer confirmedRequests = event.getConfirmedRequests();
                        event.setConfirmedRequests(++confirmedRequests);
                        confirmedRequestsList.add(request);
                    }
                });
                case REJECTED -> requestList.forEach(request -> {
                    if (request.getStatus() != Status.PENDING) {
                        throw new ConflictDataException("Можно изменить только статус PENDING");
                    }
                    request.setStatus(Status.REJECTED);
                    requestClient.updateRequestStatus(request.getId(), String.valueOf(Status.REJECTED));
                    rejectedRequests.add(request);
                });
            }
        }

        return new EventRequestStatusUpdateResult(confirmedRequestsList, rejectedRequests);
    }

    @Override
    public EventFullDto findEventById(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));

        UserDto user = userClient.findById(event.getInitiatorId());

        return EventMapper.mapToFullDto(event, 0L, user);
    }

    @Transactional
    @Override
    public void updateConfirmedRequests(Long eventId, Integer confirmedRequests) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие не найдено"));
        event.setConfirmedRequests(confirmedRequests);
    }

    @Override
    public boolean checkExistsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public void deleteEventsByUser(Long userId) {
        eventRepository.deleteByInitiatorId(userId);
    }

    private void checkFields(NewEventDto dto) {
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата начала события должна быть позже чем через 2 часа от текущего времени");
        }

        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }
        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }
        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0);
        }
    }

    private void checkDateEvent(LocalDateTime newDateTime) {

        LocalDateTime now = LocalDateTime.now().plusHours(1);
        if (now.isAfter(newDateTime)) {
            throw new InvalidDateTimeException(String.format("Дата начала события должна быть позже текущего времени на %s ч.", 1));
        }
    }

    private void checkStateAction(Event oldEvent, UpdateEventAdminRequest newEvent) {

        if (newEvent.getStateAction() == StateAction.PUBLISH_EVENT) {
            if (oldEvent.getState() != State.PENDING) {
                throw new OperationFailedException("Невозможно опубликовать событие. Его можно " +
                        "опубликовать только в состоянии ожидания публикации.");
            }
        }
        if (newEvent.getStateAction() == StateAction.REJECT_EVENT) {
            if (oldEvent.getState() == State.PUBLISHED) {
                throw new OperationFailedException("Событие опубликовано, поэтому отменить его невозможно.");
            }
        }
    }

}
