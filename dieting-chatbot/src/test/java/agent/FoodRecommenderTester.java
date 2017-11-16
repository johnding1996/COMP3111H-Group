package agent;

import controller.ChatbotController;
import controller.Publisher;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
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

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FoodRecommender.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class FoodRecommenderTester {
    @Autowired
    private FoodRecommender recommender;

    @Autowired
    private Publisher publisher;

    private static String szhouanId = "813f61a35fbb9cc3adc28da525abf1fe";

    private static JSONObject menuJSON;
    private static JSONObject dish1, dish2;

    @BeforeClass
    public static void setupMenuJSON() {
        menuJSON = new JSONObject();
        menuJSON.put("userId", szhouanId);
        JSONArray menu = new JSONArray();
        dish1 = new JSONObject();
        dish1.put("dishName", "Dish1");
        dish1.put("foodContent", (new JSONArray())
            .put(getNutrientJSON(1058))
            .put(getNutrientJSON(2010)));
        menu.put(dish1);
        dish2 = new JSONObject();
        dish2.put("dishName", "Dish2");
        dish2.put("foodContent", (new JSONArray())
            .put(getNutrientJSON(1094))
            .put(getNutrientJSON(4572))
            .put(getNutrientJSON(4501)));
        menu.put(dish2);
        menuJSON.put("menu", menu);
        log.info(menuJSON.toString(4));
    }

    private static JSONObject getNutrientJSON(int index) {
        return (new JSONObject()).put("idx", index);
    }

    @Test
    public void testConstruct() {
        assert recommender != null;
    }

    @Test
    public void testGetUserJSON() {
        JSONObject userJSON = recommender.getUserJSON(szhouanId);
        log.info(userJSON.toString(4));
        assert userJSON.getString("name").equals("szhouan");
        assert userJSON.getInt("weight") == 57;
    }

    @Test
    public void testGetFoodJSON() {
        JSONArray foodList = recommender.getFoodJSON(
            dish1.getJSONArray("foodContent"));
        log.info(foodList.toString(4));
        assert foodList.length() == dish1.getJSONArray("foodContent").length();
    }

    @Test
    public void testGetUserBMR() {
        JSONObject userJSON = recommender.getUserJSON(szhouanId);
        double bmr = recommender.getUserBMR(userJSON);
        assert Math.abs(bmr - 1611.37) < 1;
        log.info("UserBMR: " + bmr);
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
    public void testRecommendation1() {
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                log.info("Inside: {}", fmt.toString());
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text = messages.getJSONObject(0)
                    .getString("textContent");
                assert text.startsWith("You should choose");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recommender.doRecommendation(menuJSON);
        Mockito.reset(publisher);
    }
}