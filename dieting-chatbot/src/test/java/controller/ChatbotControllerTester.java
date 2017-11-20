package controller;

import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.UserSource;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ChatbotControllerTester {
    @Autowired
    private ChatbotController controller;

    @Autowired
    private LineMessagingClient client;

    @Autowired
    private Publisher publisher;

    @Autowired
    private TaskScheduler taskScheduler;

    @Before
    public void setup() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    public void testConstruct() {
        assert controller != null;
    }

    @Test
    public void testSetUserState() {
        controller.setUserState("szhouan", State.RECOMMEND);
        assert controller.getUserState("szhouan") == State.RECOMMEND;
        controller.setUserState("szhouan", State.ASK_MEAL);
        assert controller.getUserState("szhouan") == State.ASK_MEAL;
    }

    @Test
    public void testAccept1() {
        controller.setUserState("user1", State.ASK_MEAL);
        FormatterMessageJSON fmt = new FormatterMessageJSON("user1");
        fmt.appendTextMessage("Hello world");
        Event<FormatterMessageJSON> ev = new Event<>(null, fmt);
        controller.accept(ev);
        assert messageString.equals("Hello world");
    }

    @Test
    public void testAccept2() {
        controller.setUserState("user3", State.ASK_MEAL);
        for (int i=0; i<10; ++i) {
            FormatterMessageJSON fmt = new FormatterMessageJSON("user3");
            fmt.appendTextMessage("Hello world" + i);
            Event<FormatterMessageJSON> ev = new Event<>(null, fmt);
            controller.accept(ev);
            assert messageString.equals("Hello world" + i);
        }
    }

    @Test
    public void testAcknowledge() {
        controller.setUserState("foo", State.ASK_MEAL);
        FormatterMessageJSON fmt = new FormatterMessageJSON("foo");
        Event<FormatterMessageJSON> ev = new Event<>(null, fmt);
        String str = "You should not change this";
        messageString = str;
        controller.accept(ev);
        assert messageString.equals(str);
    }

    @Test
    public void testTextMessage() {
        controller.setUserState("user2", State.RECOMMEND);
        MessageEvent<TextMessageContent> event =
            getTextMessageEvent("user2", "testing");
        controller.handleTextMessageEvent(event);
        assert parserString.equals("testing");

        controller.setUserState("user2", State.IDLE);
        String str = "No change";
        parserString = str;
        controller.handleTextMessageEvent(event);
        assert parserString.equals(str);
    }

    @Test
    public void testCancelSession() {
        String userId = "cancelUser";
        controller.setUserState(userId, State.RECOMMEND);
        MessageEvent<TextMessageContent> event =
            getTextMessageEvent(userId, " CANCEL ");
        controller.handleTextMessageEvent(event);
        assert controller.getUserState(userId) == State.IDLE;
    }

    @Test
    public void testFollowEvent() {
        String userId = "user32910";
        controller.setUserState(userId, State.IDLE);
        FollowEvent event = new FollowEvent(null,
            new UserSource("U"+userId), null);
        controller.handleFollowEvent(event);
        assert controller.getUserState(userId) == State.FOLLOWING;
    }

    @Test
    public void testUnfollowEvent() {
        String userId = "user339";
        controller.setUserState(userId, State.RECOMMEND);
        UnfollowEvent event = new UnfollowEvent(
            new UserSource("U"+userId), null);
        controller.handleUnfollowEvent(event);
        assert controller.getUserState(userId) == State.UNFOLLOWING;
    }

    @Test
    public void testNoReplyCallback() {
        String userId = "user354";
        overrideTaskScheduler();
        formatterString = "should be changed";
        MessageEvent<TextMessageContent> event =
            getTextMessageEvent(userId, "No reply?");
        controller.handleTextMessageEvent(event);
        assert formatterString != null;
    }

    @Test
    public void testTimeout() {
        String userId = "user54";
        overrideTaskScheduler();
        controller.setUserState(userId, State.RECOMMEND);
        assert controller.getUserState(userId) == State.RECORD_MEAL;
    }

    private String messageString = null;
    private String parserString = null;
    private String formatterString = null;

    @Before
    public void setClient() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Message msg = invocation.getArgumentAt(0,
                    PushMessage.class).getMessages().get(0);
                if (msg instanceof TextMessage) {
                    messageString = ((TextMessage)msg).getText();
                    log.info("TEST: {}", messageString);
                }
                return null;
            }
        }).when(client).pushMessage(Matchers.any(PushMessage.class));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                ParserMessageJSON psr = invocation.getArgumentAt(0,
                    ParserMessageJSON.class);
                log.info(psr.toString());
                parserString = psr.get("textContent");
                return null;
            }
        }).when(publisher).publish(Matchers.any(ParserMessageJSON.class));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info(fmt.toString());
                JSONObject msg = fmt.getMessageArray().getJSONObject(0);
                if (msg.get("type").equals("text")) {
                    formatterString = msg.getString("textContent");
                }
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
    }

    @After
    public void resetClient() {
        Mockito.reset(client);
        Mockito.reset(publisher);
    }

    /**
     * Return a text message event with given user Id and text.
     * @param userId String of user Id
     * @param text text string
     */
    private MessageEvent<TextMessageContent> getTextMessageEvent(
        String userId, String text) {
        return new MessageEvent<TextMessageContent>(
            null, new UserSource("U"+userId),
            new TextMessageContent("", text), null
        );
    }

    /**
     * Override taskScheduler so that future task is executed immediately.
     */
    private void overrideTaskScheduler() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Runnable runnable = invocation.getArgumentAt(0,
                    Runnable.class);
                Mockito.reset(taskScheduler);
                runnable.run();
                return null;
            }
        }).when(taskScheduler).schedule(
            Matchers.any(Runnable.class), Matchers.any(Date.class));
    }
}