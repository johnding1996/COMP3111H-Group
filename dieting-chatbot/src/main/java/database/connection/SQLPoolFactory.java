package database.connection;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * SQLPool Spring Boot Factory Class.
 */
@Component
public class SQLPoolFactory {
    /**
     * Create a new SQL Pool Factory.
     * @return new SQL Pool Factory
     */
    @Bean
    public SQLPoolFactory createRedisPoolFactory() {
        SQLPool.initialization();
        return new SQLPoolFactory();
    }
}