package ru.practicum.ewm.starter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.processors.event.EventSimilarityProcessor;
import ru.practicum.ewm.processors.user.UserActionProcessor;


@Component
public class AnalyzerStarter implements CommandLineRunner {
    private final UserActionProcessor userActionProcessor;
    private final EventSimilarityProcessor eventSimilarityProcessor;

    public AnalyzerStarter(EventSimilarityProcessor eventSimilarityProcessor, UserActionProcessor userActionProcessor) {
        this.eventSimilarityProcessor = eventSimilarityProcessor;
        this.userActionProcessor = userActionProcessor;
    }


    @Override
    public void run(String... args) {
        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("userActionHandlerThread");
        userActionThread.start();

        eventSimilarityProcessor.start();
    }
}
