package com.moneywise.gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JWTAuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JWTUtil jwtUtil;

    // Endpoints that do NOT require authentication
    private final List<String> openEndpoints = List.of("/api/v1/auth", "/actuator", "/fallback");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip JWT check for open endpoints
        if (isOpenEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Validate token
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract username and forward as header to downstream services
        String username = jwtUtil.extractUsername(token);
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Name", username)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isOpenEndpoint(String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
