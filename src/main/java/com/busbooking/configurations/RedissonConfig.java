package com.busbooking.configurations;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for Redisson, the Redis client used for Distributed Locking. */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private String redisPort;

    /**
     * Creates and configures the RedissonClient bean. This client is used by the BookingService to
     * acquire and manage distributed locks, ensuring Zero Overbooking in a clustered/microservice
     * environment.
     *
     * @return A configured RedissonClient instance.
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = String.format("redis://%s:%s", redisHost, redisPort);
        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }
}
