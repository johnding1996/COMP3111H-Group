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
@SpringBootTest(classes = {PortionAsker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class PortionAskerTester {
    @Autowired
    private PortionAsker asker;

    @Autowired
    private Publisher publisher;

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
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
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text = (String) messages.getJSONObject(0)
                        .get("textContent");
                assert text.startsWith("Please input some");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
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
        Mockito.reset(publisher);
    }

    @Test
    public void testInternalState0() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "233333");
        asker.changeUserState(psr.get("userId"), 0);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Okay, this is");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testInternalState1Yes() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "Yes");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Okay, so give me");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testInternalState1No() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "NO");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Alright, let's move on");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testInternalState1Other() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "thomaszhou");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Sorry, I'm not sure");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testInternalState2Leave() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "leave");
        asker.changeUserState(psr.get("userId"), 2);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Alright, we are going to");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testInternalState2Incorrect() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "xiaoxigua")
                .set("state", "AskPortion")
                .set("replyToken", "token")
                .setTextMessage("1234", "thomaszhou");
        asker.changeUserState(psr.get("userId"), 2);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.get("userId").equals("xiaoxigua");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Plz enter in this format");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }
}
