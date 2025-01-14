package com.personal.apigateway.service;


import com.personal.apigateway.config.RateLimiterProperties;
import com.personal.apigateway.model.RateLimiterResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@ConditionalOnBean(RedisTemplate.class)
@Service
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties rateLimiterProperties;

    @Autowired
    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimiterProperties rateLimiterProperties){
        this.redisTemplate = redisTemplate;
        this.rateLimiterProperties = rateLimiterProperties;
    }

    public RateLimiterResult allowRequest(String key) {
        key = "IP__" + key;
        Long currentRequests = redisTemplate.opsForValue().increment(key);
        System.out.println("key name " + key);
        if (currentRequests == 1) {
            redisTemplate.expire(key, rateLimiterProperties.getTimeWindow(), TimeUnit.SECONDS);
        }
        boolean isAllowed = currentRequests != null && currentRequests <= rateLimiterProperties.getMaxRequests();
        return new RateLimiterResult(
                isAllowed,
                rateLimiterProperties.getMaxRequests(),
                rateLimiterProperties.getTimeWindow()
        );
    }

}
