package com.example.streamengine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getJobStatus() {
        String dispatched = redisTemplate.opsForValue().get("metrics:jobs_dispatched");
        String processed = redisTemplate.opsForValue().get("metrics:jobs_processed");
        
        Map<String, String> status = new HashMap<>();
        status.put("jobs_dispatched", dispatched != null ? dispatched : "0");
        status.put("jobs_processed", processed != null ? processed : "0");
        status.put("status", "RUNNING");
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<Object, Object>> getMetrics() {
        // We will fetch real-time per-user metrics from a Redis Hash maintained by Celery
        Map<Object, Object> userMetrics = redisTemplate.opsForHash().entries("metrics:user_counts");
        return ResponseEntity.ok(userMetrics);
    }
}
