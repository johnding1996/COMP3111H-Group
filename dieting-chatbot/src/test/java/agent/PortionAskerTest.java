package agent;

import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

import java.util.Arrays;

import javax.annotation.PostConstruct;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PortionAsker.class, JazzySpellChecker.class})
@ContextConfiguration(classes = {TestConfiguration.class, DatabaseMocker.class})
public class PortionAskerTest extends AgentTest {

    @Autowired
    private PortionAsker asker;

    @Autowired
    private MenuManager menuManager;

    @PostConstruct
    public void init() {
        agent  = asker;
        agentState = State.ASK_PORTION;
        userId = "cliubf";

        JSONObject menuJSON = new JSONObject();
        JSONArray menu = new JSONArray();

        for(int i = 0; i < 3; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "xiaoxigua");
            menu.put(dish);
        }

        for(int i = 0; i < 3; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "laoxigua");
            menu.put(dish);
        }

        menuJSON.put("userId", userId);
        menuJSON.put("menu", menu);
        menuManager.storeMenuJSON(userId, menuJSON);
    }

    @Test
    public void testConstruct() {
        assert asker != null;
    }

    @Test
    public void testTransition() {
        asker.registerUser(userId);
        checkHandler("", Arrays.asList("Okay, here is",
            "", "Would you like"), 0, 1);
        checkHandler("asdf", "You should simply", 1, 1);
        checkHandler("no", "Alright", 1, Agent.END_STATE);

        asker.registerUser(userId);
        checkHandler("", "Okay, here is", 0, 1);
        checkHandler("yes", "Okay, please give me your", 1, 2);
        checkHandler("fjakds", "", 2, 2);
        checkHandler("123:fa", "", 2, 2);
        checkHandler("123:3124:432", "", 2, 2);
        checkHandler("-324:34892", "", 2, 2);
        checkHandler("1:-4321", "", 2, 2);
        checkHandler("1:100", "", 2, 2);
        checkHandler("leave", "", 2, Agent.END_STATE);
    }
}