package controller;

import java.util.HashMap;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import com.linecorp.bot.client.LineMessagingClient;

import lombok.extern.slf4j.Slf4j;
import reactor.Environment;
import reactor.bus.EventBus;

@Slf4j
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
    public ChatbotController createController() {
        ChatbotController controller = Mockito.spy(
            ChatbotController.class);
        Mockito.doAnswer(new Answer<State>() {
            @Override
            public State answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                return states.getOrDefault(userId, State.IDLE);
            }
        }).when(controller).getUserState(Matchers.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                State newState = invocation.getArgumentAt(1, State.class);
                states.put(userId, newState);
                return null;
            }
        }).when(controller).setKeeperState(Matchers.anyString(),
        Matchers.any(State.class));
        return controller;
    }

    @Bean
    public LineMessagingClient createLineMessagingClient() {
        return Mockito.mock(LineMessagingClient.class);
    }

    private static HashMap<String, State> states = new HashMap<>();
}