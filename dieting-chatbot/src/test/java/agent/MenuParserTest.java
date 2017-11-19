package agent;

import controller.State;
import controller.TestConfiguration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MenuParser.class, JazzySpellChecker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MenuParserTest extends AgentTest {

    @Autowired
    private MenuParser parser;
    
    @PostConstruct
    public void init() {
        agent = parser;
        agentState = State.PARSE_MENU;
        userId = "agong";
    }

    @Test
    public void testWrongState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
        psr.setState("Recommend");
        checkNotExecuted(psr);
    }

    @Test
    public void testTextMenuMessage() {
        controller.setUserState(userId, agentState);
        parser.registerUser(userId);
        checkHandler("", "Long time no", 0, 1);
        checkHandler("dish1\ndish2\n", "", 1, Agent.END_STATE);
        assert controller.getUserState(userId) == State.ASK_MEAL;
    }

    @Test
    public void testUrlMenuMessage() {
        controller.setUserState(userId, agentState);
        parser.registerUser(userId);
        checkHandler("", "Long time no", 0, 1);
        checkHandler(System.getenv("JSON_MENU_URL"), "", 1, Agent.END_STATE);
        assert controller.getUserState(userId) == State.ASK_MEAL;
    }
}