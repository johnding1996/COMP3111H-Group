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
@SpringBootTest(classes = {MenuParser.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MenuParserTester {
    @Autowired
    private MenuParser parser;

    @Autowired
    private Publisher publisher;
    
    @Test
    public void testConstruct() {
        assert parser != null;
    }

    @Test
    public void testClearState() {
        String userId = "FOOOOOOj";
        addUser(userId, "ha");
        assert parser.getUserState(userId) >= 0;
        parser.clearUserStates();
        assert parser.getUserState(userId) == -1;
    }

    @Test
    public void testGetState() {
        parser.clearUserStates();
        String userId = "szhouan";
        addUser(userId, "ha");
        assert parser.getUserState(userId) == 1;
    }

    @Test
    public void testWrongState() {
        parser.clearUserStates();
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "Idle");
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                assert false;
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        Mockito.reset(publisher);
    }

    @Test
    public void testImageMessage() {
        parser.clearUserStates();
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "ParseMenu")
           .set("replyToken", "token")
           .setImageMessage("1234");
        Mockito.doAnswer(getAnswerWithAssert("Sorry but I"))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        Mockito.reset(publisher);
    }

    @Test
    public void testTextMenuMessage() {
        parser.clearUserStates();
        ParserMessageJSON psr;

        psr = getParserMessageJSON("agong",
            "recommend");
        Mockito.doAnswer(getAnswerWithAssert("Long time no"))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        assert parser.getUserState("agong") == 1;

        psr = getParserMessageJSON("agong",
            "dish1\ndish2\n");
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info("Inside: {}", fmt.toString());
                String transition = (String)fmt.get("stateTransition");
                assert transition.equals("menuMessage");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        assert parser.getUserState("agong") == -1;

        Mockito.reset(publisher);
    }

    @Test
    public void testUrlMenuMessage() {
        parser.clearUserStates();

        addUser("szhouan", "ha");
        ParserMessageJSON psr;
        psr = getParserMessageJSON("szhouan", System.getenv("JSON_MENU_URL"));
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info("Inside: {}", fmt.toString());
                String transition = (String)fmt.get("stateTransition");
                assert transition.equals("menuMessage");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        assert parser.getUserState("szhouan") == -1;

        Mockito.reset(publisher);
    }
    private void addUser(String userId, String text) {
        Event<ParserMessageJSON> ev = new Event<>(null,
            getParserMessageJSON(userId, text));
        parser.accept(ev);
    }

    private ParserMessageJSON getParserMessageJSON(String userId,
        String text) {

        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", userId)
           .set("replyToken", "1234")
           .set("state", "ParseMenu")
           .setTextMessage("1234", text);
        return psr;
    }

    private Answer<Void> getAnswerWithAssert(String prefix) {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info("Inside: {}", fmt.toString());
                JSONArray messages = (JSONArray) fmt.get("messages");
                String text = (String) messages.getJSONObject(0)
                    .get("textContent");
                assert text.startsWith(prefix);
                return null;
            }
        };
    }
}