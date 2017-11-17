package agent;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import controller.TestConfiguration;
import database.querier.UserQuerier;
import java.util.Arrays;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FoodRecommender.class, JazzySpellChecker.class,
    UserManager.class})
@ContextConfiguration(classes = {TestConfiguration.class, FoodRecommenderTest.class})
public class FoodRecommenderTest extends AgentTest {

    static HashMap<String, JSONObject> menus = new HashMap<>();

    @Bean
    public MenuManager createMenuManager() {
        MenuManager menuManager = Mockito.spy(MenuManager.class);
        Mockito.doAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                return menus.getOrDefault(userId, null);
            }
        }).when(menuManager).getMenuJSON(Matchers.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                JSONObject menuJSON = invocation.getArgumentAt(1, JSONObject.class);
                menus.put(userId, menuJSON);
                return null;
            }
        }).when(menuManager).storeMenuJSON(Matchers.anyString(),
            Matchers.any(JSONObject.class));
        return menuManager;
    }

    @Autowired(required = false)
    private FoodRecommender recommender;

    private static String szhouanId = "813f61a35fbb9cc3adc28da525abf1fe";
    private static JSONObject menuJSON;
    private static JSONObject dish1, dish2;

    @PostConstruct
    public void init() {
        agent = recommender;
        agentState = State.RECOMMEND;
        userId = szhouanId;
    }

    @Before
    public void setupMenuJSON() {
        menuJSON = new JSONObject();
        menuJSON.put("userId", szhouanId);
        JSONArray menu = new JSONArray();
        dish1 = new JSONObject();
        dish1.put("name", "Dish1");
        dish1.put("foodContent", (new JSONArray())
            .put(getNutrientJSON(1058))
            .put(getNutrientJSON(2010)));
        menu.put(dish1);
        dish2 = new JSONObject();
        dish2.put("name", "Dish2");
        dish2.put("foodContent", (new JSONArray())
            .put(getNutrientJSON(1094))
            .put(getNutrientJSON(4572))
            .put(getNutrientJSON(4501)));
        menu.put(dish2);
        menuJSON.put("menu", menu);
        log.info(menuJSON.toString(4));
        menus.put(userId, menuJSON);
    }

    private static JSONObject getNutrientJSON(int index) {
        return (new JSONObject()).put("idx", index);
    }

    @Test
    public void testConstruct() {
        assert recommender != null;
    }

    @Test
    public void testGetFoodJSON() {
        JSONArray foodList = recommender.getFoodJSON(
            dish1.getJSONArray("foodContent"));
        log.info(foodList.toString(4));
        assert foodList.length() == dish1.getJSONArray("foodContent").length();
    }

    @Test
    public void testGetAverageNutrient() {
        JSONArray foodList = recommender.getFoodJSON(
            dish2.getJSONArray("foodContent"));
        double averageFiber = recommender.getAverageNutrient(
            foodList, "fiber_td");
        assert averageFiber < 0.1;
        log.info(foodList.toString(4));
    }

    @Test
    public void testTransition() {
        recommender.registerUser(userId);
        checkHandler("", 
            Arrays.asList("Ok, let us analyze", "According to", "Please CONFIRM"),
            0, 1);
        checkHandler("whatever", "", 1, 1);
        checkHandler("confirm",
            Arrays.asList("Great, I've recorded", "Another question", "Valid options"),
            1, 2);
        recommender.setUserState(userId, 1);
        checkHandler("breakfast",
            Arrays.asList("Great, I've recorded", "Another question", "Valid options"),
            1, 2);
        checkHandler("whatever", "", 2, 2);
        checkHandler("little", "", 2, 3);
    }
}