package agent;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import reactor.bus.Event;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

/**
 * AgentTest: abstract tester for agent.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
public abstract class AgentTest {

    /**
     * Responsible state for the tested agent, to be overriden.
     */
    State agentState = State.INVALID;

    @Autowired
    Publisher publisher;

    @Autowired
    ChatbotController controller;

    /**
     * The agent to be tested.
     */
    Agent agent = null;

    /**
     * Testing user Id.
     */
    String userId = "defaultUser";

    /**
     * Wrapper for tracking internal state transition and agent reply.
     * @param input Input string
     * @param prefix List of expected output prefix
     * @param state Current internal state
     * @param newState Expected new state
     */
    public void checkHandler(String input, List<String> prefix, int state, int newState) {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.set("textContent", input)
           .setState(agentState.getName());
        Event<ParserMessageJSON> ev = new Event<>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                JSONArray messages = fmt.getMessageArray();
                // ignore acknowledge fmt
                if (messages.length()==0) return null;
                String text = messages.getJSONObject(0).getString("textContent");
                // ignore rejected input message or spell checker message
                if (text.startsWith("OOPS") || text.startsWith("Corrected")) return null;
                log.info("Reply Message: {}", text);
                for (int i=0; i<prefix.size(); ++i) {
                    assert messages.getJSONObject(i).getString("textContent")
                        .startsWith(prefix.get(i));
                }
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        assert agent.getUserState(userId) == state;
        agent.accept(ev);
        log.info("agent state {}, expect {}", agent.getUserState(userId), newState);
        assert agent.getUserState(userId) == newState;
        Mockito.reset(publisher);
    }

    /**
     * Wrapper for tracking internal state transition and agent reply.
     * @param input Input string
     * @param prefix Expected output prefix
     * @param state Current internal state
     * @param newState Expected new state
     */
    public void checkHandler(String input, String prefix, int state, int newState) {
        checkHandler(input, Collections.singletonList(prefix), state, newState);
    }

    /**
     * Assert that the agent does not handle the message.
     * @param psr Input ParseMessageJSON
     */
    public void checkNotExecuted(ParserMessageJSON psr) {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                assert false;
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class)); 
        agent.accept(new Event<ParserMessageJSON>(null, psr));
        Mockito.reset(publisher);
    }

    @Test
    public void testConstruction() {
        assert agent != null;
    }
}