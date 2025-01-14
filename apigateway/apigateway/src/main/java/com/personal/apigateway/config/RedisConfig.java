package com.personal.apigateway.config;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

//@ConfigurationProperties(prefix = "spring.data.redis")
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.port}")
    private String port;

    @Value("${apigateway.redis-post-construct:false}")
    private boolean postConstruct;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getPostConstruct() {
        return postConstruct;
    }

    public void setPostConstruct(boolean postConstruct) {
        this.postConstruct = postConstruct;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * this helps in actual interacting with redis, it ensures same level key saving i.e string
     * so that java and redis service work in sync
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // as this would serialize the machine's and java code redis instance
        // otherwise machine's and java redis will work independently
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }


    /**
     * this creates actual connection to redis specifically for reactive programming
     *
     * @param defaultRedisConfig
     * @return
     */
    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(RedisConfiguration defaultRedisConfig) {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl().build();
        return new LettuceConnectionFactory(defaultRedisConfig, clientConfig);
    }


    /**
     * This is basic redis configuration
     * this will run first to set up the redis connection --> basic connection
     *
     * @return
     */
    @Bean
    public RedisConfiguration defaultRedisConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPassword(RedisPassword.of(password));
        return config;
    }
   


    @PostConstruct
    public void logRedisConfiguration() {
        if (this.postConstruct) {
            System.out.println("Redis Configuration:");
            System.out.println("Host: --> " + this.host);
            System.out.println("Port: --> " + this.port);
        }
    }

}