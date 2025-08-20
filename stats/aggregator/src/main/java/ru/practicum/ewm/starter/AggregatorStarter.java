package ru.practicum.ewm.starter;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.service.AggregatorService;

@Component
@RequiredArgsConstructor
public class AggregatorStarter implements CommandLineRunner {
    private final AggregatorService service;

    @Override
    public void run(String... args) {
        service.start();
    }
}
