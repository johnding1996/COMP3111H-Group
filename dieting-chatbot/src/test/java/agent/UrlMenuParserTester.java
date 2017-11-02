package agent;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class UrlMenuParserTester {
    @Test
    public void testBuildJSONUrl1() {
        JSONArray menu = new JSONArray();
        for (int i=0; i<10; ++i) {
            JSONObject dish = new JSONObject();
            dish.put("name", "dish"+i);
            menu.put(dish);
        }
        log.info(menu.toString(4));
        JSONArray thatMenu = UrlMenuParser.buildMenu(menu);
        log.info(thatMenu.toString(4));
        JSONAssert.assertEquals(thatMenu, menu, false);
    }

    @Test
    public void testBuildJSONUrl2() {
        JSONArray menu = new JSONArray();
        for (int i=0; i<10; ++i) {
            JSONObject dish = new JSONObject();
            if (i % 2 == 0) dish.put("name", "dish"+i);
            else dish.put("noname", "dish"+i);
            menu.put(dish);
        }
        JSONArray thatMenu = UrlMenuParser.buildMenu(menu);
        assert thatMenu.length() == menu.length()/2;
        for (int i=0; i<thatMenu.length(); ++i)
            JSONAssert.assertEquals(thatMenu.getJSONObject(i),
                menu.getJSONObject(2*i), false);
    }

    @Test
    public void testBuildJSONUrl3() {
        JSONArray menu = new JSONArray();
        for (int i=0; i<10; ++i) {
            JSONObject dish = new JSONObject();
            if (i % 2 == 1) dish.put("name", "dish"+i);
            else dish.put("noname", i);
            menu.put(dish);
        }
        JSONArray thatMenu = UrlMenuParser.buildMenu(menu);
        assert thatMenu.length() == menu.length()/2;
        for (int i=0; i<thatMenu.length(); ++i)
            JSONAssert.assertEquals(thatMenu.getJSONObject(i),
                menu.getJSONObject(2*i+1), false);
    }

    @Test
    public void testBuildJSONUrl4() {
        JSONArray menu = new JSONArray();
        JSONObject dish = new JSONObject();
        for (int i=0; i<10; ++i) menu.put(dish);
        JSONArray thatMenu = UrlMenuParser.buildMenu(menu);
        assert thatMenu == null;
    }

    @Test
    public void testBuildJSON() {
        JSONArray menu = UrlMenuParser.buildMenu(System.getenv("JSON_MENU_URL"));
        log.info(menu.toString(4));
        assert menu.length() == 3;
        assert menu.getJSONObject(0).get("name")
            .equals("Spicy Bean curd with Minced Pork served with Rice");
        assert menu.getJSONObject(1).get("name")
            .equals("Sweet and Sour Pork served with Rice");
        assert menu.getJSONObject(2).get("name")
            .equals("Chili Chicken on Rice");
    }
}