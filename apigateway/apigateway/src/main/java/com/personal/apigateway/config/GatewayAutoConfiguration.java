package com.personal.apigateway.config;

import com.personal.apigateway.filter.GatewayFilter;
import com.personal.apigateway.service.LoadBalancer;
import com.personal.apigateway.service.RateLimiterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "apigateway.enabled", havingValue = "true", matchIfMissing = true)
public class GatewayAutoConfiguration {

    @Bean
    public GatewayFilter gatewayFilter(RateLimiterService rateLimiterService,
                                       LoadBalancer loadBalancer, WebClient.Builder webClientBuilder) {
        return new GatewayFilter(rateLimiterService, loadBalancer, webClientBuilder);
    }

    @Bean
    public FilterRegistrationBean<GatewayFilter> gatewayFilterRegistration(GatewayFilter gatewayFilter) {
        FilterRegistrationBean<GatewayFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(gatewayFilter);
        registrationBean.addUrlPatterns("/gateway/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
