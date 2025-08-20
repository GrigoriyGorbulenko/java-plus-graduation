package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.event.EventFullDto;
import ru.practicum.ewm.event.service.EventService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/events")
public class InternalEventController {
    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventFullDto findEventById(@PathVariable Long eventId) {
        return eventService.findEventById(eventId);
    }

    @PutMapping("/{eventId}")
    public void updateConfirmedRequests(@PathVariable Long eventId, @RequestParam Integer confirmedRequests) {
        eventService.updateConfirmedRequests(eventId, confirmedRequests);
    }

    @GetMapping
    public boolean checkExistsById(@RequestParam Long eventId) {
        return eventService.checkExistsById(eventId);
    }

    @DeleteMapping
    public void deleteEventsByUser(@RequestParam Long userId) {
        eventService.deleteEventsByUser(userId);
    }
}
