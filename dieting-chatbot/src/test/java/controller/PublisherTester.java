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
@ContextConfiguration(classes = {TestConfiguration.class, Publisher.class})
public class PublisherTester {
    @Autowired
    private Publisher publisher;

    @Test
    public void publishTester() {
        FormatterMessageJSON fmt = new FormatterMessageJSON("user3");
        fmt.appendTextMessage("Hello world");
        publisher.publish(fmt);

        ParserMessageJSON pmt = new ParserMessageJSON("userId", "idle");
        pmt.set("textContent", "Hello");
        publisher.publish(pmt);
    }
}
