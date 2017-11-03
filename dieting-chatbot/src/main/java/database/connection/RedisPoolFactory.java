package database.connection;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RedisPoolFactory {
    @Bean
    public RedisPoolFactory createRedisPoolFactory() {
        RedisPool.initialization();
        return new RedisPoolFactory();
    }
}