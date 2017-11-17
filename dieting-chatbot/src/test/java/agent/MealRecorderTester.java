package agent;

import controller.Publisher;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealRecorder.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealRecorderTester {
    @Autowired
    private MealRecorder recorder;

    @Autowired
    private Publisher publisher;

    private static final String userId = "cliubfxiaoxigua";

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
        psr.setState("RecordMeal");
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals("cliubfxiaoxigua");
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                if (messages.length() == 0) return null;
                String text = (String) messages.getJSONObject(0)
                        .get("textContent");
                assert text.startsWith("Please input some");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recorder.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testFalseState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("RecordMeal");
        recorder.setUserState(psr.get("userId"), 0);
        List<String> prefixList = new ArrayList<>();
        prefixList.add("Welcome back");
        prefixList.add("please enter in");
        assertReplyContent(prefixList, psr);
    }


    @Test
    public void testTrueState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("RecordMeal")
           .set("textContent", "1;2;3");
        recorder.setUserState(psr.get(userId), 1);
        List<String> prefixList = Collections.singletonList("Great! I have");
        assertReplyContent(prefixList, psr);
    }

    /**
     * Assert replies for a psr have expected prefix.
     * @param prefixList List of prefix
     * @param psr ParserMessageJSON
     */
    private void assertReplyContent(List<String> prefixList, ParserMessageJSON psr) {
        Event<ParserMessageJSON> ev = new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = fmt.getMessageArray();
                if (messages.length() == 0) return null;
                for (int i=0; i<prefixList.size(); ++i) {
                    String text = messages.getJSONObject(i).getString("textContent");
                    assert text.startsWith(prefixList.get(i));
                }
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recorder.accept(ev);
        Mockito.reset(publisher);
    }
}