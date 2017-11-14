package agent;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.JsonUtility;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import controller.ChatbotController;
import controller.Publisher;
import controller.State;
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

/**
 * MealAsker: interact with user to get the appropriate menu.
 * @author szhouan
 * @version v2.0.0
 */
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

    @Autowired(required = false)
    private ChatbotController controller;

    static private HashMap<String, JSONObject> menus = new HashMap<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserInitialInputRecord register on event bus");
        }
    }

    /**
     * Clear all QueryJSON.
     */
    public void clearQueryJSON() {
        log.info("Removing all QueryJSON object");
        menus.clear();
    }

    /**
     * set QueryJSON for a user.
     * @param json QueryJSON to add.
     */
    public void setQueryJSON(JSONObject json) {
        if (JsonUtility.validateQueryJSON(json))
            menus.put(json.getString("userId"), json);
        else log.info("Invalid Query JSON:\n" + json.toString(4));
    }

    /**
     * get QueryJSON for a user.
     * @param userId String of user Id.
     * @return JSONObject.
     */
    public JSONObject getQueryJSON(String userId) {
        if (menus.containsKey(userId)) return menus.get(userId);
        else return null;
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `AskMeal`
        String userId = psr.getUserId();
        State state = psr.getState();
        if (state != State.ASK_MEAL) {
            if (menus.containsKey(userId)) {
                menus.remove(userId);
                log.info("Remove menu of user {}", userId);
            }
            return;
        }

        log.info("Entering MealAsker");
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        publisher.publish(fmt);

        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        // if the input is image
        if(psr.getType().equals("image")) {
            response.appendTextMessage(
                "I am sorry that I can't understand this image");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }
        
        if (menus.containsKey(userId)) {
            JSONObject queryJSON = menus.get(userId);
            response.appendTextMessage("Well, I got your menu.");
            response.appendTextMessage("The Menu I got is\n" +
                JsonUtility.formatQueryJSON(queryJSON));
            publisher.publish(response);

            menus.remove(userId);
            JSONObject menuJSON = queryJSONtoMenuJSON(queryJSON);
            log.info("MenuJSON:\n{}", menuJSON.toString(4));
            recommender.setMenuJSON(menuJSON);
            if (controller != null) {
                controller.setUserState(userId, State.RECOMMEND);
            }
        } else {
            response.appendTextMessage(
                "Oops, looks like your menu is empty. Session cancelled.");
            publisher.publish(response);
            if (controller != null) {
                controller.setUserState(userId, State.IDLE);
            }
        }
    }

    /**
     * Transform a QueryJSON to MenuJSON, by quering food content.
     * @param queryJSON A JSONObject of QueryJSON format.
     * @return A MenuJSON.
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
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("And this is the food content of each dish I found:")
           .appendTextMessage(JsonUtility.formatMenuJSON(menu));
        publisher.publish(fmt);
        return menuJSON;
    }

    /**
     * Return food content JSONArray given dish name.
     * @param dishName String of dish name.
     * @return A JSONArray containing food content.
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
     * Filter dish name.
     * @param dishName String of dish name.
     * @return A list of words filtered.
     */
    public static List<String> filterDishName(String dishName) {
        ArrayList<String> list = new ArrayList<>();
        for (String word : TextProcessor.sentenceToWords(dishName)) {
            if (!discardWords.contains(word)) list.add(word);
        }
        return list;
    }
}