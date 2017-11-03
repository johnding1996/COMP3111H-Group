package agent;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import utility.TextProcessor;
import controller.FormatterMessageJSON;
import controller.ParserMessageJSON;
import controller.Publisher;
import database.connection.SQLPool;
import database.keeper.MenuKeeper;
import database.querier.FuzzyFoodQuerier;
import database.querier.PartialFoodQuerier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MealAsker
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired
    private FoodRecommender recommender;

    static private HashMap<String, JSONObject> menus = new HashMap<>();

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserInitialInputRecord register on event bus");
        }
    }

    /**
     * Clear all QueryJSON
     */
    public void clearQueryJSON() {
        log.info("Removing all QueryJSON object");
        menus.clear();
    }

    /**
     * set QueryJSON for a user
     * @param json QueryJSON to add
     */
    public void setQueryJSON(JSONObject json) {
        if (validateQueryJSON(json))
            menus.put((String)json.get("userId"), json);
        else log.info("Invalid Query JSON:\n" + json.toString(4));
    }

    /**
     * get QueryJSON for a user
     * @param userId String of user Id
     * @return JSONObject
     */
    public JSONObject getQueryJSON(String userId) {
        if (menus.containsKey(userId)) return menus.get(userId);
        else return null;
    }

    /**
     * Validate QueryJSON
     * @param json QueryJSON to check
     * @return A boolean, whether the format is valid
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

    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `AskMeal`
        String currentState = psr.get("state");
        if (!currentState.equals("AskMeal")) {
            String userId = psr.get("userId");
            if (menus.containsKey(userId))
                menus.remove(userId);
            return;
        }

        log.info("Entering user ask meal handler");
        String userId = psr.get("userId");
        String replyToken = psr.get("replyToken");

        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "push");
        // if the input is not text
        if(!psr.getMessageType().equals("text")) {
            response.appendTextMessage(
                "Sorry but I don't understand this image");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }
        
        if (menus.containsKey(userId)) {
            JSONObject queryJSON = menus.get(userId);
            response.appendTextMessage("Well, I got your menu");
            response.appendTextMessage("The Menu I got is\n" +
                formatQueryJSON(queryJSON));
            response.set("stateTransition", "confirmMeal");
            publisher.publish(response);

            menus.remove(userId);
            JSONObject menuJSON = queryJSONtoMenuJSON(queryJSON);
            log.info("MenuJSON:\n{}", menuJSON.toString(4));
            recommender.doRecommendation(menuJSON);
        } else {
            response.appendTextMessage("Oops, looks like your menu is empty");
            publisher.publish(response);
        }
    }

    /**
     * Format a QueryJSON
     * @param queryJSON A QueryJSON
     * @return formatted String
     */
    public String formatQueryJSON(JSONObject queryJSON) {
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
     * Format a MenuJSON
     * @param menu A menu in JSONArray
     * @return formatted String
     */
    public String formatMenuJSON(JSONArray menu) {
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
     * Transform a QueryJSON to MenuJSON
     * @param queryJSON A JSONObject of QueryJSON format
     * @return A MenuJSON
     */
    public JSONObject queryJSONtoMenuJSON(JSONObject queryJSON) {
        JSONObject menuJSON = new JSONObject();
        String userId = queryJSON.getString("userId");
        menuJSON.put("userId", userId);
        JSONArray menu = new JSONArray();
        JSONArray queryMenu = queryJSON.getJSONArray("menu");
        for (int i=0; i<queryMenu.length(); ++i) {
            JSONObject queryDish = queryMenu.getJSONObject(i);
            JSONObject dish = new JSONObject();
            String dishName = queryDish.getString("name");
            dish.put("dishName", dishName);
            dish.put("foodContent", getFoodContent(dishName));
            menu.put(dish);
        }
        menuJSON.put("menu", menu);
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", userId)
           .set("type", "push")
           .appendTextMessage("And this is the food content of each dish I found")
           .appendTextMessage(formatMenuJSON(menu));
        publisher.publish(fmt);
        return menuJSON;
    }

    /**
     * Return food content JSONArray given dish name
     * @param dishName String of dish name
     * @return A JSONArray containing food content
     */
    public JSONArray getFoodContent(String dishName) {
        List<String> keyWords = filterDishName(dishName);
        FuzzyFoodQuerier querier = new FuzzyFoodQuerier();
        querier.setQueryLimit(1);

        JSONArray foodContent = new JSONArray();
        for (String word : keyWords) {
            JSONObject candidate = querier.search(word)
                .getJSONObject(0);
            log.info("candidate:\n{}", candidate.toString(4));
            int index = candidate.getInt("ndb_no");
            String description = candidate.getString("shrt_desc");
            JSONObject item = new JSONObject();
            item.put("idx", index);
            item.put("description", description);
            foodContent.put(item);
        }
        querier.close();
        return foodContent;
    }

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
     * Filter dish name
     * @param dishName String of dish name
     * @return A list of words filtered
     */
    public static List<String> filterDishName(String dishName) {
        ArrayList<String> list = new ArrayList<>();
        List<String> rawList = TextProcessor.sentenceToWords(dishName);
        for (String word : rawList) {
            if (!discardWords.contains(word)) {
                list.add(word);
            }
        }
        return list;
    }
}