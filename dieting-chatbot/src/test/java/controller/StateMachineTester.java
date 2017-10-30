package controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.MessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.LineBotMessages;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import controller.StateMachine;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { StateMachine.class, String.class })
public class StateMachineTester {
    private StateMachine stateMachine = new StateMachine("def");

    @Test
    public void testUserId() throws Exception {
        log.info(stateMachine.getUserId());
        assert stateMachine.getUserId().equals("def") : "Wrong user id";
    }

    @Test
    public void testSetUserId() throws Exception {
        stateMachine.setUserId("newId");
        assert stateMachine.getUserId().equals("newId") : "Id not changed after set";
    }

    @Test
    public void testInitialization() throws Exception {
        stateMachine.initialize();
        assert stateMachine.getState().equals("Idle");
    }

    @Test
    public void testSetGetState() throws Exception {
        stateMachine.setState("Recommend");
        assert stateMachine.getState().equals("Recommend");
        stateMachine.setState("InvalidState");
        assert stateMachine.getState().equals("Recommend");
        stateMachine.setState("ParseMenu");
        assert stateMachine.getState().equals("ParseMenu");
    }

    @Test
    public void testStateTransition() throws Exception {
        stateMachine.initialize();
        log.info("Good here");
        stateMachine.toNextState("recommendationRequest");
        assert stateMachine.getState().equals("ParseMenu");
        stateMachine.toNextState("menuMessage");
        assert stateMachine.getState().equals("AskMeal");
        stateMachine.toNextState("confirmMeal");
        assert stateMachine.getState().equals("Recommend");
        stateMachine.setState("RecordMeal");
        assert stateMachine.getState().equals("RecordMeal");
        stateMachine.toNextState("confirmMeal");
        assert stateMachine.getState().equals("Idle");
        stateMachine.toNextState("initialInputRequest");
        assert stateMachine.getState().equals("InitialInput");
        stateMachine.toNextState("userInitialInput");
        assert stateMachine.getState().equals("Idle");
        stateMachine.toNextState("feedbackRequest");
        assert stateMachine.getState().equals("Feedback");
        stateMachine.toNextState("sendFeedback");
        assert stateMachine.getState().equals("Idle");
    }

    @Test
    public void testInvalidTransition() throws Exception {
        stateMachine.initialize();
        stateMachine.toNextState("menuMessage");
        assert stateMachine.getState().equals("Idle");
        stateMachine.toNextState("foo bar");
        assert stateMachine.getState().equals("Idle");
        stateMachine.setState("InitialInput");
        stateMachine.toNextState("initialInputRequest");
        assert stateMachine.getState().equals("InitialInput");
        stateMachine.setState("Recommend");
        stateMachine.toNextState("bar foo");
        assert stateMachine.getState().equals("Recommend");
    }

    @Test
    public void testStateTransitionReturnValue() throws Exception {
        stateMachine.initialize();
        assert stateMachine.toNextState("recommendationRequest");
        assert stateMachine.toNextState("menuMessage");
        assert !stateMachine.toNextState("userInitialInput");
        assert !stateMachine.toNextState("invalidTransition");
        assert stateMachine.toNextState("confirmMeal");
    }

    @Test
    public void testStateObjectGetter() throws Exception {
        stateMachine.initialize();
        assert stateMachine.getStateObject().getName()
                .equals("Idle");
        stateMachine.toNextState("feedbackRequest");
        assert stateMachine.getStateObject().getName()
                .equals("Feedback");
    }
}