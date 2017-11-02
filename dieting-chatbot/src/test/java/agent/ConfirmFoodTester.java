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
@SpringBootTest(classes = {ConfirmFood.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class ConfirmFoodTester {
    @Autowired
    private ConfirmFood confirmFood;

    @Autowired
    private Publisher publisher;

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "cliubfxiaoxigua")
                .set("state", "RecordMeal")
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
                assert fmt.get("userId").equals("cliubfxiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text = (String) messages.getJSONObject(0)
                        .get("textContent");
                assert text.startsWith("Please input some");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        confirmFood.accept(ev);
    }

    @Test
    public void testFalseState() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "cliubfxiaoxigua")
                .set("state", "Feedback")
                .set("replyToken", "token")
                .setTextMessage("1234", "1;2;3");
        confirmFood.changeUserState(psr.get("userId"), false);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("cliubfxiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Welcome back");
                String text2 = (String) messages.getJSONObject(0).get("textContent");
                assert text2.startsWith("Please enter in");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        confirmFood.accept(ev);
    }


    @Test
    public void testTrueState() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "cliubfxiaoxigua")
                .set("state", "Feedback")
                .set("replyToken", "token")
                .setTextMessage("1234", "1;2;3");
        confirmFood.changeUserState(psr.get("userId"), true);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("cliubfxiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Great! I have");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        confirmFood.accept(ev);
    }


}