package utility;

import database.querier.FuzzyFoodQuerier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
     * Format a MenuJSON.
     * @param menuJSON Input MenuJSON
     * @param withFoodContent Whether print food content
     * @return formatter String
     */
    public static String formatMenuJSON(JSONObject menuJSON,
        boolean withFoodContent) {

        String ret = "";
        JSONArray menu = menuJSON.getJSONArray("menu");
        for (int i=0; i<menu.length(); ++i) {
            JSONObject dish = menu.getJSONObject(i);
            String dishName = dish.getString("name");
            ret += String.format("Dish %d - %s\n", i+1, dishName);
            if (!withFoodContent) continue;
            JSONArray foodContent = dish.getJSONArray("foodContent");
            for (int j=0; j<foodContent.length(); ++j) {
                String desc = foodContent.getJSONObject(j)
                    .getString("description");
                ret += String.format("--- %s\n", desc);
            }
        }
        return ret;
    }

    /**
     * Get food content for a menuJSON, and modify in-place.
     * @param menuJSON input MenuJSON
     */
    public static void getFoodContent(JSONObject menuJSON) {
        JSONArray menu = menuJSON.getJSONArray("menu");
        FuzzyFoodQuerier querier = new FuzzyFoodQuerier();
        querier.setQueryLimit(1);
        for (int i=0; i<menu.length(); ++i) {
            JSONObject dish = menu.getJSONObject(i);
            String name = dish.getString("name");
            List<String> keyWords = filterDishName(name);

            JSONArray foodContent = new JSONArray();
            for (String word : keyWords) {
                JSONObject candidate = querier.search(word)
                    .getJSONObject(0);
                // log.info("candidate:\n{}", candidate.toString(4));
                int index = candidate.getInt("ndb_no");
                String description = candidate.getString("shrt_desc");
                JSONObject item = new JSONObject();
                item.put("idx", index);
                item.put("description", description);
                foodContent.put(item);
            }
            dish.put("foodContent", foodContent);
        }
        querier.close();
    }

    /**
     * private hashSet sroring the discard words.
     */
    private static final HashSet<String> discardWords;
    static {
        List<String> list = Arrays.asList(
            "of", "with", "and", "the", "a", "on", "in",
            "served", "fried", "minced", "stewed", "baked",
            "roasted", "grilled", "dish", "some",
            "sweet", "sour", "spicy", "salty"
        );
        discardWords = new HashSet<>(list);
    }

    /**
     * Filter dish name.
     * @param dishName String of dish name.
     * @return A list of words filtered.
     */
    public static List<String> filterDishName(String dishName) {
        ArrayList<String> list = new ArrayList<>();
        for (String word : TextProcessor.getTokens(dishName)) {
            if (!discardWords.contains(word)) list.add(word);
        }
        return list;
    }
}