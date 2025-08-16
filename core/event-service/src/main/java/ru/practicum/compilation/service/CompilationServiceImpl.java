package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.stats.StatsDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;


import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.StatClient;
import ru.practicum.feign.UserClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final StatClient statClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto newCompilationDto) {
        Set<Long> eventIds = newCompilationDto.getEvents();
        List<Event> events;
        if (eventIds != null && !eventIds.isEmpty()) {
            events = getSeveralEvents(eventIds.stream().toList());
        } else {
            events = Collections.emptyList();
        }
        if (newCompilationDto.getPinned() == null) {
            newCompilationDto.setPinned(false);
        }
        Compilation compilation = compilationRepository.save(CompilationMapper.toCompilation(newCompilationDto, events));
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(events));

    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id: " + compId + " не найдена"));
        Set<Long> eventIds = updateCompilationRequest.getEvents();
        if (eventIds != null && !eventIds.isEmpty()) {
            compilation.setEvents(new HashSet<>(getSeveralEvents(eventIds.stream().toList())));
        }
        Boolean pinned = updateCompilationRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }
        String title = updateCompilationRequest.getTitle();
        if (title != null && !title.isBlank()) {
            compilation.setTitle(title);
        }
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(new ArrayList<>(compilation.getEvents())));
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id: " + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getAllCompilations(Boolean pinned, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Compilation> allCompilations;
        if (pinned == null) {
            allCompilations = compilationRepository.findAll(pageRequest).toList();
        } else {
            allCompilations = compilationRepository.findAllByPinned(pageRequest, pinned);
        }
        Map<Long, EventShortDto> allEventDto = mapToEventShort(allCompilations.stream()
                .flatMap(compilation -> compilation.getEvents().stream()).distinct().toList())
                .stream().collect(Collectors.toMap(EventShortDto::getId, Function.identity()));
        List<CompilationDto> compilationDtoList = new ArrayList<>();
        for (Compilation compilation : allCompilations) {
            List<EventShortDto> listEventDto = compilation.getEvents().stream().map(event -> allEventDto.get(event.getId()))
                    .toList();
            compilationDtoList.add(CompilationMapper.toCompilationDto(compilation, listEventDto));
        }
        return compilationDtoList;

    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с id: " + compId + " не найдена"));
        return CompilationMapper.toCompilationDto(compilation, mapToEventShort(new ArrayList<>(compilation.getEvents())));
    }

    private List<Event> getSeveralEvents(List<Long> eventIds) {
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Не удалось найти некоторые события в базе данных");
        }
        return events;
    }

    private List<EventShortDto> mapToEventShort(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDateTime minTime = events.stream().map(Event::getCreatedOn).min(Comparator.comparing(Function.identity())).get();
        List<String> urisList = events.stream().map(event -> "/events/" + event.getId()).toList();

        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, UserDto> usersMap = userClient
                .getAllUsers(userIds, 0, userIds.size()).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));

        List<StatsDto> statsList = statClient.getStats(minTime.minusSeconds(1), LocalDateTime.now(), urisList, false);

        return events.stream().map(event -> {
                    Optional<StatsDto> result = statsList.stream()
                            .filter(statsDto -> statsDto.getUri().equals("/events/" + event.getId()))
                            .findFirst();
                    if (result.isPresent()) {
                        return EventMapper.mapToShortDto(event, result.get().getHits(),
                                usersMap.getOrDefault(event.getInitiatorId(),
                                        UserDto.builder().id(0L).name("Unknown").build()));
                    } else {
                        return EventMapper.mapToShortDto(event, 0L, usersMap.getOrDefault(event.getInitiatorId(),
                                UserDto.builder().id(0L).name("Unknown").build()));
                    }
                })
                .collect(Collectors.toList());
    }
}

