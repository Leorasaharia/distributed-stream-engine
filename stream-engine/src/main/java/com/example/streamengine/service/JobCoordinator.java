package com.example.streamengine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobCoordinator {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a task to the Python Celery worker via Redis.
     * We emulate the Celery protocol v2 JSON.
     */
    public void dispatchToCelery(String eventJson) {
        try {
            String taskId = UUID.randomUUID().toString();

            // Celery protocol v2 expects "body" to be a valid JSON representation of args and kwargs.
            // Example body: [[eventPayloadMap], {}, {"callbacks": null, "errbacks": null, "chain": null, "chord": null}]
            // For simplicity, we can decode the incoming JSON to a Map.
            Map<String, Object> eventMap = objectMapper.readValue(eventJson, Map.class);
            
            Object[] args = new Object[]{eventMap};
            Map<String, Object> kwargs = new HashMap<>();
            Map<String, Object> embed = new HashMap<>();
            
            Object[] bodyArray = new Object[]{args, kwargs, embed};
            String bodyBase64 = java.util.Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(bodyArray));

            Map<String, Object> headers = new HashMap<>();
            headers.put("lang", "py");
            headers.put("task", "tasks.process_event");
            headers.put("id", taskId);
            headers.put("root_id", taskId);
            headers.put("parent_id", null);
            headers.put("group", null);

            Map<String, Object> properties = new HashMap<>();
            properties.put("correlation_id", taskId);
            properties.put("reply_to", UUID.randomUUID().toString());
            properties.put("delivery_info", Map.of("exchange", "", "routing_key", "celery"));
            properties.put("delivery_mode", 2);
            properties.put("delivery_tag", UUID.randomUUID().toString());
            properties.put("body_encoding", "base64");

            Map<String, Object> celeryPayload = new HashMap<>();
            celeryPayload.put("body", bodyBase64);
            celeryPayload.put("content-encoding", "utf-8");
            celeryPayload.put("content-type", "application/json");
            celeryPayload.put("headers", headers);
            celeryPayload.put("properties", properties);

            String payloadStr = objectMapper.writeValueAsString(celeryPayload);
            
            // Push to Celery's default Redis queue
            redisTemplate.opsForList().leftPush("celery", payloadStr);
            
            log.info("Dispatched event task {} to Celery", taskId);
            
            // Increment jobs dispatched metric
            redisTemplate.opsForValue().increment("metrics:jobs_dispatched");

        } catch (Exception e) {
            log.error("Failed to dispatch task to Celery", e);
        }
    }
}
