package agent;

import controller.FormatterMessageJSON;
import controller.ParserMessageJSON;
import controller.Publisher;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealAsker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealAskerTester {
    @Autowired
    private MealAsker asker;

    @Autowired
    private Publisher publisher;

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "AskMeal")
           .set("replyToken", "token")
           .setImageMessage("1234");
        Event<ParserMessageJSON> ev =
            new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                assert fmt.get("userId").equals("agong");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text = (String) messages.getJSONObject(0)
                    .get("textContent");
                assert text.startsWith("Sorry but I");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "Feedback")
           .set("replyToken", "token")
           .setTextMessage("1234", "hello");
        Event<ParserMessageJSON> ev =
            new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                assert false;
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }
}