package com.example.streamengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIngestionService {

    private final JobCoordinator jobCoordinator;

    @KafkaListener(topics = "events-topic", groupId = "engine-group")
    public void consumeEvent(String eventMessage) {
        log.info("Consumed event from Kafka: {}", eventMessage);
        
        // Forward the event for processing using the Job Coordinator
        jobCoordinator.dispatchToCelery(eventMessage);
    }
}
