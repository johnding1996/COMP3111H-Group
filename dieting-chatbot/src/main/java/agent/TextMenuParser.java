package agent;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * TextMenuParser: handles text menu.
 * @author cliubf, szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class TextMenuParser {
    /**
     * Build menu information from text.
     * @param text String of user input text.
     * @return A JSON array; null if no dish is successfully parsed.
     */
    public static JSONArray buildMenu(String text) {
        JSONArray arr = new JSONArray();
        String[] lines = text.split("\n");
        for (String line : lines) {
            JSONObject dish = new JSONObject();
            dish.put("name", line);
            arr.put(dish);
        }
        return arr.length()==0 ? null : arr;
    }
}