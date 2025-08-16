package ru.practicum.feign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.exception.ServerUnavailable;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "findEventByIdFallback")
    @GetMapping("/{eventId}")
    EventFullDto findEventById(@PathVariable Long eventId) throws FeignException;

    @PutMapping("/{eventId}")
    void updateConfirmedRequests(@PathVariable Long eventId, @RequestParam Integer confirmedRequests)
            throws FeignException;

    @GetMapping
    boolean checkExistsById(@RequestParam Long eventId) throws FeignException;

    @DeleteMapping
    void deleteEventsByUser(@RequestParam Long userId) throws FeignException;

    @GetMapping("/{eventId}")
    default EventFullDto findEventById(Long eventId, Throwable throwable) {
        throw new ServerUnavailable("Event Server unavailable");
    }
}
