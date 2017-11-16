package agent;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class TextMenuParserTester {
    @Test
    public void testBuildTextMenu1() {
        String[] menu = {"dish1", "dish2", "dish3"};
        JSONArray arr = TextMenuParser.buildMenu(
            String.join("\n", menu));
        assert arr.length() == menu.length;
        for (int i=0; i<arr.length(); ++i) {
            JSONObject dish = arr.getJSONObject(i);
            String dishName = (String)dish.get("name");
            assert dishName.equals(menu[i]);
        }
    }

    @Test
    public void testBuildTextMenu2() {
        String dishName = "Foo-Bar-Baz";
        JSONArray arr = TextMenuParser.buildMenu(dishName);
        assert arr.length() == 1;
        JSONObject dish = arr.getJSONObject(0);
        String name = (String)dish.get("name");
        assert name.equals(dishName);
    }
}