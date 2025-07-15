package ru.practicum.feign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ServerUnavailable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface ParticipationRequestClient {

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "prepareConfirmedRequestsFallback")
    @GetMapping("/prepare-confirmed-requests")
    Map<Long, List<ParticipationRequestDto>> prepareConfirmedRequests(@RequestParam List<Long> eventIds)
            throws FeignException;

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "findAllByEventIdFallback")
    @GetMapping("/{eventId}")
    List<ParticipationRequestDto> findAllByEventId(@PathVariable Long eventId) throws FeignException;

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "findAllByIdFallback")
    @GetMapping
    List<ParticipationRequestDto> findAllById(@RequestParam List<Long> requestIds) throws FeignException;

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "updateRequestStatusFallback")
    @PutMapping("/{requestId}")
    void updateRequestStatus(@PathVariable Long requestId, @RequestParam String status) throws FeignException;

    @DeleteMapping
    void deleteRequestsOfUser(@RequestParam Long userId) throws FeignException;

    @GetMapping("/prepare-confirmed-requests")
    default Map<Long, List<ParticipationRequestDto>> prepareConfirmedRequestsFallback(List<Long> eventIds,
                                                                                      Throwable throwable) {
        return new HashMap<>();
    }

    @GetMapping("/{eventId}")
    default List<ParticipationRequestDto> findAllByEventIdFallback(Long eventId, Throwable throwable) {
        return new ArrayList<>();
    }

    @GetMapping
    default List<ParticipationRequestDto> findAllByIdFallback(List<Long> requestIds, Throwable throwable) {
        return new ArrayList<>();
    }

    @PutMapping("/{requestId}")
    default void updateRequestStatusFallback(Long requestId, String status, Throwable throwable) {
        throw new ServerUnavailable("Request Server unavailable");
    }
}