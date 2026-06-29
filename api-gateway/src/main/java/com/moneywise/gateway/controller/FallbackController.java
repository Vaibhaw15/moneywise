package com.moneywise.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for the API Gateway.
 * When a downstream service is unreachable and the circuit breaker trips,
 * the gateway routes the request here instead of returning a raw 500 error.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<Map<String, Object>> authFallback() {
        return buildFallbackResponse("Auth Service");
    }

    @GetMapping("/category")
    public Mono<Map<String, Object>> categoryFallback() {
        return buildFallbackResponse("Category Service");
    }

    @GetMapping("/transaction")
    public Mono<Map<String, Object>> transactionFallback() {
        return buildFallbackResponse("Transaction Service");
    }

    private Mono<Map<String, Object>> buildFallbackResponse(String serviceName) {
        return Mono.just(Map.of(
                "error", serviceName + " is temporarily unavailable. Please try again later.",
                "status", 503,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
