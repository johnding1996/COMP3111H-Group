package agent;

import controller.FormatterMessageJSON;
import controller.ParserMessageJSON;
import controller.Publisher;
import controller.TestConfiguration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MealAsker.class, FoodRecommender.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class MealAskerTester {
    @Autowired
    private MealAsker asker;

    @Autowired
    private Publisher publisher;

    @Test
    public void testAcceptImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "AskMeal")
           .set("replyToken", "token")
           .setImageMessage("1234");
        Event<ParserMessageJSON> ev =
            new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                assert fmt.get("userId").equals("agong");
                JSONArray messages = (JSONArray)fmt.get("messages");
                String text = (String) messages.getJSONObject(0)
                    .get("textContent");
                assert text.startsWith("Sorry but I");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testAnotherState() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", "agong")
           .set("state", "Feedback")
           .set("replyToken", "token")
           .setTextMessage("1234", "hello");
        Event<ParserMessageJSON> ev =
            new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                throws Throwable {
                assert false;
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testValidateQueryJSON1() {
        JSONObject dish = new JSONObject();
        JSONArray menu = new JSONArray();
        for (int i=0; i<10; ++i) {
            dish.put("name", "dishName"+i);
            menu.put(dish);
        }
        JSONObject json = new JSONObject();
        json.put("userId", "szhouan")
            .put("menu", menu);
        assert MealAsker.validateQueryJSON(json);
    }

    @Test
    public void testValidateQueryJSON2() {
        JSONObject dish = new JSONObject();
        JSONArray menu = new JSONArray();
        dish.put("nutrient", "blah");
        menu.put(dish);
        JSONObject json = new JSONObject();
        json.put("userId", "szhouan")
            .put("menu", menu);
        assert !MealAsker.validateQueryJSON(json);
    }

    @Test
    public void testValidateQueryJSON3() {
        JSONObject json = new JSONObject();
        json.put("userId", "szhouan")
            .put("notMenu", "foo");
        assert !MealAsker.validateQueryJSON(json);
    }

    @Test
    public void testValidateQueryJSON4() {
        JSONObject json = new JSONObject();
        json.put("id", "szhouan")
            .put("menu", new JSONArray());
        assert !MealAsker.validateQueryJSON(json);
    }

    @Test
    public void testQueryJSONGetterSetter1() {
        JSONObject dish = new JSONObject();
        dish.put("name", "dishName");
        JSONArray menu = new JSONArray();
        menu.put(dish);
        JSONObject json = new JSONObject();
        json.put("userId", "agong")
            .put("menu", menu);
        asker.setQueryJSON(json);
        JSONAssert.assertEquals(json, asker.getQueryJSON("agong"), false);
    }

    @Test
    public void testQueryJSONGetterSetter2() {
        asker.clearQueryJSON();
        JSONObject json = new JSONObject();
        json.put("id", "agong");
        asker.setQueryJSON(json);
        assert asker.getQueryJSON("agong") == null;
    }

    @Test
    public void testClearQueryJSON() {
        asker.clearQueryJSON();
        assert asker.getQueryJSON("abc") == null;
        JSONObject json = new JSONObject();
        json.put("userId", "abc")
            .put("menu", new JSONArray());
        asker.setQueryJSON(json);
        assert asker.getQueryJSON("abc") != null;
        asker.clearQueryJSON();
        assert asker.getQueryJSON("abc") == null;
    }

    @Test
    public void testFilterDishName() {
        String dishName = "A dish with some fried and stewed spicy chicken";
        List<String> keyWords = MealAsker.filterDishName(dishName);
        assert keyWords.size() == 1;
        assert keyWords.get(0).equals("chicken");
    }

    @Test
    public void testGetFoodContent() {
        String dishName = "Spicy Bean curd with Minced Pork served with Rice";
        JSONArray foodContent = asker.getFoodContent(dishName);
        log.info("FoodContent:\n{}", foodContent.toString(4));
        assert foodContent.length() == 4;
    }

    @Test
    public void testGetMenuJSON() {
        JSONObject queryJSON = new JSONObject();
        queryJSON.put("userId", "szhouan");
        JSONArray queryMenu = new JSONArray();
        JSONObject dish;
        dish = new JSONObject();
        dish.put("name", "Fish and potato");
        queryMenu.put(dish);
        dish = new JSONObject();
        dish.put("name", "Rice with pork");
        queryMenu.put(dish);
        dish = new JSONObject();
        dish.put("name", "Fried beef");
        queryMenu.put(dish);
        queryJSON.put("menu", queryMenu);
        log.info("queryJSON:\n{}", queryMenu.toString(4));
        JSONObject menuJSON = asker.queryJSONtoMenuJSON(queryJSON);
        log.info("menuJSON\n{}", menuJSON.toString(4));
    }
}