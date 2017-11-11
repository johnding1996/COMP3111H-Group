package database.connection;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * RedisPool Spring Boot Factory Class.
 */
@Component
public class RedisPoolFactory {
    /**
     * Create a new Redis Pool Factory.
     * @return new Redis Pool Factory
     */
    @Bean
    public RedisPoolFactory createRedisPoolFactory() {
        RedisPool.initialization();
        return new RedisPoolFactory();
    }
}