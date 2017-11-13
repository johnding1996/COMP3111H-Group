package utility;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * JsonUtility: JSON builder, formatter and parser for various JSON types.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
public class JsonUtility {
    /**
     * Format a QueryJSON.
     * @param queryJSON A QueryJSON.
     * @return formatted String.
     */
    public static String formatQueryJSON(JSONObject queryJSON) {
        String ret = "";
        JSONArray menu = queryJSON.getJSONArray("menu");
        for (int i=0; i<menu.length(); ++i) {
            JSONObject dish = menu.getJSONObject(i);
            String dishName = dish.getString("name");
            ret += String.format("Dish %d -- %s\n", i+1, dishName);
        }
        return ret;
    }

    /**
     * Format a MenuJSON.
     * @param menu A menu in JSONArray.
     * @return formatted String.
     */
    public static String formatMenuJSON(JSONArray menu) {
        String ret = "";
        for (int i=0; i<menu.length(); ++i) {
            JSONObject dish = menu.getJSONObject(i);
            String dishName = dish.getString("dishName");
            ret += "* " + dishName + "\n";
            String tab = "---";
            JSONArray foodContent = dish.getJSONArray("foodContent");
            for (int j=0; j<foodContent.length(); ++j) {
                String description = foodContent.getJSONObject(j)
                    .getString("description");
                ret += tab + description.toLowerCase() + "\n";
            }
        }
        return ret;
    }

    /**
     * Validate QueryJSON.
     * @param json QueryJSON to check.
     * @return A boolean, whether the format is valid.
     */
    public static boolean validateQueryJSON(JSONObject json) {
        try {
            String userId = (String)json.get("userId");
            JSONArray menu = (JSONArray)json.get("menu");
            for (int i=0; i<menu.length(); ++i) {
                JSONObject dish = menu.getJSONObject(i);
                String dishName = (String)dish.get("name");
                assert dishName != null;
            }
            return true;
        } catch (Exception e) {
            log.info("Invalid QueryJSON: {}", e.toString());
            return false;
        }
    }
}