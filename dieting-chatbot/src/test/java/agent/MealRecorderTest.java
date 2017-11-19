package agent;

import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealRecorder.class, JazzySpellChecker.class, UserManager.class})
@ContextConfiguration(classes = {TestConfiguration.class, FoodRecommenderTest.class})
public class MealRecorderTest extends AgentTest {

    @Autowired
    private MealRecorder recorder;

    @PostConstruct
    public void init() {
        agent = recorder;
        agentState = State.RECORD_MEAL;
        userId = "cliubfxiaoxigua";
    }

    @Before
    public void setupMenuJSON() {
        JSONObject menuJSON = new JSONObject();
        menuJSON.put("userId", userId);
        JSONArray menu = new JSONArray();
        for (int i=0; i<10; ++i) {
            JSONObject dish = new JSONObject();
            dish.put("name", "Dish" + i);
            menu.put(dish);
        }
        menuJSON.put("menu", menu);
        FoodRecommenderTest.menus.put(userId, menuJSON);
    }

    @Test
    public void testTransition() {
        recorder.registerUser(userId);
        checkHandler("",
            Arrays.asList("Welcome back", "", "Please choose"),
            0, 1);
        checkHandler("Foo", "", 1, 1);
        checkHandler("100", "", 1, 1);
        checkHandler("-1", "", 1, 1);
        checkHandler("1",
            Arrays.asList("Great!", "And what is"),
            1, 2);
        checkHandler("whatever", "", 2, 2);
        checkHandler("-100", "", 2, 2);
        checkHandler("10000", "", 2, 2);
        checkHandler("100",
            Arrays.asList("So you have", "One more"),
            2, 3);
        checkHandler("whatever", "", 3, 3);
        checkHandler("50",
            Arrays.asList("So your weight", "See you"),
            3, Agent.END_STATE);
    }
}