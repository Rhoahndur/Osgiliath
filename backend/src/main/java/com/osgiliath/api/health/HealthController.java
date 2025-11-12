package com.osgiliath.api.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple health check endpoint for monitoring deployment status
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Application health check endpoint")
public class HealthController {

    @GetMapping
    @Operation(
        summary = "Health check",
        description = "Returns the application health status"
    )
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}
