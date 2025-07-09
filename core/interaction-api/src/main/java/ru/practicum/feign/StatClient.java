package ru.practicum.feign;


import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.stats.EndpointHitDto;
import ru.practicum.dto.stats.StatsDto;


import java.time.LocalDateTime;
import java.util.List;


@FeignClient(name = "stats-server")
public interface StatClient {
    final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @PostMapping("/hit")
    String saveHit(@RequestBody EndpointHitDto endpointHitDto) throws FeignException;

    @GetMapping("/stats")
    List<StatsDto> getStats(@RequestParam @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime start,
                            @RequestParam @DateTimeFormat(pattern = TIME_PATTERN) LocalDateTime end,
                            @RequestParam(defaultValue = "") List<String> uris,
                            @RequestParam(defaultValue = "false") boolean unique) throws FeignException;
}
