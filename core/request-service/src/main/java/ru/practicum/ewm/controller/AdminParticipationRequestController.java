package ru.practicum.ewm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.enums.request.Status;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/requests")
public class AdminParticipationRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping("/prepare-confirmed-requests")
    public Map<Long, List<ParticipationRequestDto>> prepareConfirmedRequests(@RequestParam List<Long> eventIds) {
        log.info("Получили список eventIds {} для получения подтверждённых заявок", eventIds);
        return requestService.prepareConfirmedRequests(eventIds);
    }

    @GetMapping("/{eventId}")
    public List<ParticipationRequestDto> findAllByEventId(@PathVariable Long eventId) {
        return requestService.findAllByEventId(eventId);
    }

    @GetMapping
    public List<ParticipationRequestDto> findAllById(@RequestParam List<Long> requestIds) {
        return requestService.findAllById(requestIds);
    }

    @PutMapping("/{requestId}")
    public void updateRequestStatus(@PathVariable Long requestId, @RequestParam Status status) {
        requestService.updateRequestStatus(requestId, status);
    }

    @DeleteMapping
    public void deleteRequestsOfUser(@RequestParam Long userId) {
        requestService.deleteByRequesterId(userId);
    }

    @GetMapping("/{eventId}/{userId}/check-user-confirmed")
    public boolean checkExistsByEventIdAndRequesterIdAndStatus(@PathVariable Long eventId,@PathVariable Long userId,
                                                               @RequestParam Status status) {
        return requestService.checkExistsByEventIdAndRequesterIdAndStatus(eventId, userId, status);
    }
}
