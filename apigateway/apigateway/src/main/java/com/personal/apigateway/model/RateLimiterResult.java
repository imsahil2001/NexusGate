package com.personal.apigateway.model;

public record RateLimiterResult(
        boolean isAllowed,
        long maxRequests,
        long windowSeconds
) {}
