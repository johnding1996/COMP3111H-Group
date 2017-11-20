package agent;

import controller.State;
import controller.TestConfiguration;
import database.keeper.HistKeeper;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FeedbackManager.class, JazzySpellChecker.class, FoodRecommender.class})
@ContextConfiguration(classes = {TestConfiguration.class, DatabaseMocker.class})
public class FeedbackManagerTest extends AgentTest {

    @Autowired
    private FeedbackManager feedbackManager;

    @PostConstruct
    public void init() {
        agent = feedbackManager;
        userId = "blahblahblahblahblahblahblahblah";
    }

    @Test
    public void testConstruct() {
        assert feedbackManager != null;
    }

    @Test
    public void testTransition() {
        feedbackManager.registerUser(userId);
        checkHandler("", "OK, how many", 0, 1);
        checkHandler("fajdsk", "Your input for", 1, 1);
        checkHandler("-34", "Your input for", 1, 1);
        checkHandler("2", "Sorry", 1, Agent.END_STATE);

        HistKeeper keeper = new HistKeeper();
        JSONObject goodHistJson = new JSONObject();
        goodHistJson.put("timestamp", "2017-10-29T13:30:52.123Z");

        goodHistJson.put("weight", 60);
        JSONArray food = new JSONArray();
        food.put(0);
        food.put(1);
        goodHistJson.put("portionSize", 100);
        goodHistJson.put("menu", food);
        keeper.close();
    }
}