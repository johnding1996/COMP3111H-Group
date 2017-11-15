package agent;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
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
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MenuParser.class, ImageMenuParser.class, JazzySpellChecker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MenuParserTester {
    @Autowired
    private MenuParser parser;

    @Autowired
    private Publisher publisher;

    @Autowired
    private ChatbotController controller;
    
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
        String userId = "agong";
        ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
        psr.setState("Recommend");
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
        String userId = "agong";
        ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
        psr.setState("ParseMenu");
        Mockito.doAnswer(getAnswerWithAssert("Sorry but I"))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        Mockito.reset(publisher);
    }

    @Test
    public void testTextMenuMessage() {
        String userId = "agong";
        parser.clearUserStates();

        Mockito.doAnswer(getAnswerWithAssert("Long time no"))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        addUser(userId, "foo");
        assert parser.getUserState(userId) == 1;

        ParserMessageJSON psr = getParserMessageJSON(userId,
            "dish1\ndish2\n");
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        assert controller.getUserState(userId) == State.ASK_MEAL;
        assert parser.getUserState(userId) == -1;

        Mockito.reset(publisher);
    }

    @Test
    public void testUrlMenuMessage() {
        String userId = "szhouan";
        parser.clearUserStates();

        addUser(userId, "ha");
        ParserMessageJSON psr;
        psr = getParserMessageJSON(userId, System.getenv("JSON_MENU_URL"));
        parser.accept(new Event<ParserMessageJSON>(null, psr));
        assert parser.getUserState(userId) == -1;
        assert controller.getUserState(userId) == State.ASK_MEAL;

        Mockito.reset(publisher);
    }
    private void addUser(String userId, String text) {
        controller.setUserState(userId, State.PARSE_MENU);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
        psr.setState("ParseMenu");
        Event<ParserMessageJSON> ev = new Event<>(null, psr);
        parser.accept(ev);
    }

    private ParserMessageJSON getParserMessageJSON(String userId, String text) {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.set("textContent", text);
        psr.setState(controller.getUserState(userId).getName());
        return psr;
    }

    private Answer<Void> getAnswerWithAssert(String prefix) {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info("\nInside: {}", fmt.toString());
                JSONArray messages = (JSONArray) fmt.getMessageArray();
                if (messages.length() == 0) return null;
                String text = (String) messages.getJSONObject(0)
                    .get("textContent");
                assert text.startsWith(prefix);
                return null;
            }
        };
    }
}