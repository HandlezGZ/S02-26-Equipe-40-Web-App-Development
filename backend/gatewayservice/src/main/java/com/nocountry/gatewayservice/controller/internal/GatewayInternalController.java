package com.nocountry.gatewayservice.controller.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/gateway")
public class GatewayInternalController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/fallback/{upstream}")
    public ResponseEntity<Map<String, String>> fallback(@PathVariable String upstream) {
        return ResponseEntity.status(503).body(Map.of(
                "error", "gateway_upstream_unavailable",
                "message", "upstream service unavailable",
                "upstream", upstream
        ));
    }
}
