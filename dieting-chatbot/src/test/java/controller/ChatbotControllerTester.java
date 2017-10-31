package controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingClientImpl;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import lombok.extern.slf4j.Slf4j;

import static reactor.bus.selector.Selectors.$;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ChatbotController.class, Formatter.class})
@ContextConfiguration(classes = ChatbotControllerTesterConfiguration.class)
public class ChatbotControllerTester {
    @Autowired
    private ChatbotController controller;

    @Autowired
    private Publisher publisher;

    @Test
    public void testConstruct() {
        assert controller != null;
    }

    @Test
    public void testStateMachineGetter() {
        controller.clearStateMachines();
        StateMachine sm;

        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("Idle");
        sm.setState("RecordMeal");
        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("RecordMeal");

        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Idle");
        sm.setState("Feedback");
        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Feedback");

        controller.clearStateMachines();
        sm = controller.getStateMachine("Robin");
        assert sm.getState().equals("Idle");
        sm = controller.getStateMachine("Hartshorne");
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testSentenceToWord1() {
        String sentence = "Hello! World!";
        List<String> words = ChatbotController.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 2;
        assert words.get(0).equals("hello");
        assert words.get(1).equals("world");
    }

    @Test
    public void testSentenceToWord2() {
        String sentence = "Hello!Wo?&rld!";
        List<String> words = ChatbotController.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 1;
        assert words.get(0).equals("helloworld");
    }

    @Test
    public void testSentenceToWord3() {
        String sentence = "\'This is a very, very long, sentence~'";
        List<String> words = ChatbotController.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 7;
        assert words.get(0).equals("this");
        assert words.get(1).equals("is");
        assert words.get(2).equals("a");
        assert words.get(3).equals("very");
        assert words.get(4).equals("very");
        assert words.get(5).equals("long");
        assert words.get(6).equals("sentence");
    }

    @Test
    public void testRecommendationRequestJudge1() {
        String sentence;
        sentence = "I want some recommendations.";
        assert ChatbotController.isRecommendationRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert ChatbotController.isRecommendationRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert ChatbotController.isRecommendationRequest(sentence);
    }

    @Test
    public void testRecommendationRequestJudge2() {
        String sentence;
        sentence = "Hello";
        assert !ChatbotController.isRecommendationRequest(sentence);
        sentence = "Input personal information";
        assert !ChatbotController.isRecommendationRequest(sentence);
        sentence = "*(#Ujflkd#())";
        assert !ChatbotController.isRecommendationRequest(sentence);
    }

    @Test
    public void testInitialInputRequestJudge1() {
        String sentence;
        sentence = "I want to revise personal setting.";
        assert ChatbotController.isInitialInputRequest(sentence);
        sentence = "1093, can you check my settings?";
        assert ChatbotController.isInitialInputRequest(sentence);
        sentence = "Personal! info* please;";
        assert ChatbotController.isInitialInputRequest(sentence);
    }

    @Test
    public void testInitialInputRequestJudge2() {
        String sentence;
        sentence = "I want some recommendations.";
        assert !ChatbotController.isInitialInputRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert !ChatbotController.isInitialInputRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert !ChatbotController.isInitialInputRequest(sentence);
    }

    @Test
    public void testFeedbackRequestJudge1() {
        String sentence;
        sentence = "Feedback please";
        assert ChatbotController.isFeedbackRequest(sentence);
        sentence = "I want my weekly/monthly digest";
        assert ChatbotController.isFeedbackRequest(sentence);
        sentence = "Can you generate!a report for me";
        assert ChatbotController.isFeedbackRequest(sentence);
    }

    @Test
    public void testFeedbackRequestJudge2() {
        String sentence;
        sentence = "I want some recommendations.";
        assert !ChatbotController.isFeedbackRequest(sentence);
        sentence = "Can you help me look at this menu?";
        assert !ChatbotController.isFeedbackRequest(sentence);
        sentence = "What is your suggestion on this?";
        assert !ChatbotController.isFeedbackRequest(sentence);
    }

    @Test
    public void testStateTransition1() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", "szhouan")
           .set("type", "transition")
           .set("stateTransition", "recommendationRequest");
        Event<FormatterMessageJSON> ev =
            new Event<FormatterMessageJSON>(null, fmt);
        controller.accept(ev);
        StateMachine sm = controller.getStateMachine("szhouan");
        assert sm.getState().equals("ParseMenu");
        
        fmt.set("stateTransition", "menuMessage");
        controller.accept(ev);
        assert sm.getState().equals("AskMeal");

        fmt.set("stateTransition", "confirmMeal");
        controller.accept(ev);
        assert sm.getState().equals("Recommend");

        sm.setState("RecordMeal");
        controller.accept(ev);
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "initialInputRequest");
        controller.accept(ev);
        assert sm.getState().equals("InitialInput");

        fmt.set("stateTransition", "userInitialInput");
        controller.accept(ev);
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "feedbackRequest");
        controller.accept(ev);
        assert sm.getState().equals("Feedback");

        fmt.set("stateTransition", "sendFeedback");
        controller.accept(ev);
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testStateTransition2() {
        controller.clearStateMachines();
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", "agong")
           .set("type", "transition")
           .set("stateTransition", "invalidTransition");
        Event<FormatterMessageJSON> ev =
            new Event<FormatterMessageJSON>(null, fmt);
        controller.accept(ev);
        StateMachine sm = controller.getStateMachine("agong");
        assert sm.getState().equals("Idle");

        fmt.set("stateTransition", "confirmMeal");
        controller.accept(ev);
        assert sm.getState().equals("Idle");
    }

    @Test
    public void testTimeoutState() throws Exception {
        assert controller.taskScheduler != null;
    }

    @Test
    public void testAskWeight() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                ParserMessageJSON psr = invocation.getArgumentAt(0,
                    ParserMessageJSON.class);
                assert psr.get("state").equals("AskWeight");
                return null;
            }
        }).when(publisher).publish(Matchers.any(ParserMessageJSON.class));
    }
}