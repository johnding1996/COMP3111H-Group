package agent;

import controller.Publisher;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
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
import utility.JsonUtility;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealAsker.class, PortionAsker.class, FoodRecommender.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealAskerTester {
    @Autowired
    private MealAsker asker;

    @Autowired
    private Publisher publisher;

    private String userId = "agong";

    @Before
    public void init() {
        JSONObject menuJSON = new JSONObject();
        menuJSON.put("userId", userId);
        JSONArray menu = new JSONArray();
        for (int i=0; i<3; ++i) {
            JSONObject dish = new JSONObject();
            dish.put("name", "dish" + (i+1));
            menu.put(dish);
        }
        menuJSON.put("menu", menu);
        asker.setMenuJSON(menuJSON);
        assert asker.getMenuJSON(userId) != null;
        log.info("set MenuJSON:\n{}", menuJSON.toString(4));
    }

    @Test
    public void testAccept() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
        psr.setState("AskMeal");
        assertReplyContent("Well, I got", psr);
    }

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
        psr.setState("AskMeal");
        assertReplyContent("I am sorry that", psr);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("Feedback")
           .set("textContent", "hello");
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

    /**
     * Assert reply for a psr starts with prefix.
     * @param prefix Expected prefix
     * @param psr ParserMessageJSON
     */
    private void assertReplyContent(String prefix, ParserMessageJSON psr) {
        Event<ParserMessageJSON> ev = new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                if (messages.length() == 0) return null;
                String text = (String) messages.getJSONObject(0)
                    .get("textContent");
                assert text.startsWith(prefix);
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }
}