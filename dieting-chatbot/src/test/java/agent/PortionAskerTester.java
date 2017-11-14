package agent;

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
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

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
        String userId = "xiaoxigua";
        ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
        psr.setState("AskPortion");
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
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
                assert text.startsWith("I am sorry that");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }


}