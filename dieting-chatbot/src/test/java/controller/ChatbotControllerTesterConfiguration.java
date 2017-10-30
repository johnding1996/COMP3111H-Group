package controller;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class ChatbotControllerTesterConfiguration {
    @Bean
    public TaskScheduler createTaskScheduler() {
        return Mockito.mock(ThreadPoolTaskScheduler.class);
    }
}