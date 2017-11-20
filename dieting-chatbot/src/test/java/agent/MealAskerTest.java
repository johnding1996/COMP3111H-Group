package agent;

import controller.State;
import controller.TestConfiguration;

import java.util.Arrays;

import javax.annotation.PostConstruct;
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
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealAsker.class, JazzySpellChecker.class, MenuManager.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealAskerTest extends AgentTest {

    @Autowired
    private MealAsker asker;

    @PostConstruct
    public void init() {
        agent = asker;
        agentState = State.ASK_MEAL;
        userId = "agong";

        JSONObject menuJSON = new JSONObject();
        menuJSON.put("userId", userId);
        JSONArray menu = new JSONArray();
        for (int i=0; i<3; ++i) {
            JSONObject dish = new JSONObject();
            dish.put("name", "dish" + (i+1));
            menu.put(dish);
        }
        menuJSON.put("menu", menu);
    }

    @Test
    public void testTransition() {
        controller.setUserState(userId, agentState);
        asker.registerUser(userId);
        checkHandler("", Arrays.asList("Well, I got",
            "The menu I got", "And this is the", "", "Do you want"),
            0, 1);
        checkHandler("confirm", Arrays.asList("Bravo! Your update",
            "Is there any missing"), 1, 2);
        checkHandler("yes", "So what is", 2, 3);
        checkHandler("Foo", "Okay, so what is the energy", 3, 4);
        checkHandler("adfkj", "", 4, 4);
        checkHandler("-13", "", 4, 4);
        checkHandler("100000", "", 4, 4);
        checkHandler("100", "Okay, so what is the protein", 4, 5);
        checkHandler("bdskj", "", 5, 5);
        checkHandler("-190", "", 5, 5);
        checkHandler("200000", "", 5, 5);
        checkHandler("100", "Okay, so what is the lipid", 5, 6);
        checkHandler("afkj", "", 6, 6);
        checkHandler("-133", "", 6, 6);
        checkHandler("10000", "", 6, 6);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("Feedback")
           .set("textContent", "hello");
        checkNotExecuted(psr);
    }
}