package database.connection;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SQLPoolFactory {
    @Bean
    public SQLPoolFactory createRedisPoolFactory() {
        SQLPool.initialization();
        return new SQLPoolFactory();
    }
}