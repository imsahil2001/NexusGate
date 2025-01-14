package com.personal.apigateway.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "apigateway.rate-limiter")
@Validated
public class RateLimiterProperties {

    @Min(value = 100, message = "max-requests must be at least 100")
    private int maxRequests = 100; // Default value: 100

    @Min(value = 60, message = "time-window must be at least 60")
    private long timeWindow = 60; // Default value: 60

    public int getMaxRequests() {
        return maxRequests;
    }
    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }
    public long getTimeWindow() {
        return timeWindow;
    }
    public void setTimeWindow(long timeWindow) {
        this.timeWindow = timeWindow;
    }
}
