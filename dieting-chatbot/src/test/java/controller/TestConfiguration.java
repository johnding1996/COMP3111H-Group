package controller;

import database.querier.UserQuerier;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import reactor.Environment;
import reactor.bus.EventBus;

@Configuration
public class TestConfiguration {
    @Bean
    public TaskScheduler createTaskScheduler() {
        return Mockito.mock(ThreadPoolTaskScheduler.class);
    }

    @Bean
    public Environment env() {
        return Mockito.mock(Environment.class);
    }

    @Bean
    public EventBus createEventBus(Environment env) {
        return Mockito.mock(EventBus.class);
    }

    @Bean
    public Publisher createPublisher() {
        return Mockito.mock(Publisher.class);
    }

    @Bean
    public UserQuerier createUserQuerier() {
        return Mockito.mock(UserQuerier.class);
    }
}