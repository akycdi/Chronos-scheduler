package com.chronos.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api/mock")
public class MockController {

    private final Random random = new Random();

    @GetMapping("/delay")
    public ResponseEntity<Map<String, Object>> delay(@RequestParam(defaultValue = "1000") long ms) {
        log.info("Mock delay request received. Sleeping for {} ms", ms);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Delayed response");
        response.put("delay_ms", ms);
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/fail")
    public ResponseEntity<Map<String, Object>> fail(@RequestParam(defaultValue = "500") int status) {
        log.info("Mock failure request received. Returning status {}", status);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Mock failure response");
        response.put("requested_status", status);
        response.put("status", "failed");

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/flaky")
    public ResponseEntity<Map<String, Object>> flaky(@RequestParam(defaultValue = "0.5") double probability) {
        log.info("Mock flaky request received. Failure probability: {}", probability);

        if (random.nextDouble() < probability) {
            log.info("Flaky request failing");
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Flaky failure");
            response.put("status", "failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        log.info("Flaky request succeeding");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Flaky success");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
