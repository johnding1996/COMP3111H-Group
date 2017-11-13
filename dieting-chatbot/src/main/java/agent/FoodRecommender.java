package agent;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;

import org.json.JSONArray;
import org.json.JSONException;
import controller.Publisher;
import controller.State;
import controller.ChatbotController;
import database.querier.FoodQuerier;
import database.querier.FuzzyFoodQuerier;
import database.querier.UserQuerier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * FoodRecommender: calculate scores for each dish and generate reasons for recommendation.
 * @author agong, szhouan
 * @version v1.1.0
 */
@Slf4j
@Component
public class FoodRecommender
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    private static JSONArray configuration;
    static {
        configuration = new JSONArray();
        configuration.put(getConfigJSON("lipid_tot", 70, 15))
                     .put(getConfigJSON("carbohydrt", 260, 15))
                     .put(getConfigJSON("sugar_tot", 90, 15))
                     .put(getConfigJSON("protein", 50, 15))
                     .put(getConfigJSON("fiber_td", 30, 5))
                     .put(getConfigJSON("vit_c", 35, 10))
                     .put(getConfigJSON("sodium", 1600, 10))
                     .put(getConfigJSON("potassium", 2000, 5))
                     .put(getConfigJSON("calcium", 800, 5));
        log.info("Configuration for recommender:\n{}",
            configuration.toString(4));
    }

    static private HashMap<String, JSONObject> menus = new HashMap<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("FoodRecommender register on eventBus");
        }
    }

    /*
     * Helper function for building a JSON object representing a config for nutrient.
     * @return A JSONObject containing config info for a nutrient type.
     */
    private static JSONObject getConfigJSON(String name,
        double y, double proportion) {
        JSONObject json = new JSONObject();
        json.put("name", name)
            .put("y", y)
            .put("proportion", proportion);
        return json;
    }

    /**
     * Set MenuJSON for a user.
     * @param json menuJSON to add.
     */
    public void setMenuJSON(JSONObject json) {
        menus.put(json.getString("userId"), json);
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `Recommend`
        String userId = psr.getUserId();
        State state = controller==null ?
            State.INVALID : controller.getUserState(userId);
        if (state != State.RECOMMEND) {
            if (menus.containsKey(userId)) {
                menus.remove(userId);
                log.info("Remove menu of user {}", userId);
            }
            return;
        }

        log.info("Entering FoodRecommender");
        publisher.publish(new FormatterMessageJSON(userId));

        if (psr.getType().equals("transition")) {
            if (menus.containsKey(userId)) {
                doRecommendation(menus.get(userId));
            } else {
                FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                fmt.appendTextMessage("Seems that I don't have your menu. " +
                    "Session cancelled.");
                publisher.publish(fmt);
                if (controller != null) {
                    controller.setUserState(userId, State.IDLE);
                }
            }
        } else if (psr.getType().equals("text")) {
            String msg = psr.get("textContent");
            if (isMealFinished(msg)) {
                menus.remove(userId);
                if (controller != null) {
                    controller.setUserState(userId, State.RECORD_MEAL);
                }
            } else {
                FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                fmt.appendTextMessage("Did you mean you have finished your meal? " +
                    "If yes, please tell me and I will record what you have eaten.");
                publisher.publish(fmt);
            }
        }
    }

    /**
     * Helper function for deciding whether user means meal finished.
     * @param msg User input sentence.
     * @return Whether user means meal finished.
     */
    public boolean isMealFinished(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (finishMealKeywords.contains(word)) return true;
        }
        return false;
    }
    static private final HashSet<String> finishMealKeywords;
    static {
        finishMealKeywords = new HashSet<>(
            Arrays.asList(
                "finish", "finished", "done"
            )
        );
    }

    /**
     * Wrapper for doing recommendation.
     * @param menuJSON A JSONObject of MenuJSON format.
     * @return Whether recommendation succeed.
     */
    public boolean doRecommendation(JSONObject menuJSON) {
        String userId = menuJSON.getString("userId");
        JSONObject userJSON = getUserJSON(userId);
        if (userJSON == null) {
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Sorry, I don't have your personal " +
               "information yet, please set your personal information first. " +
               "Session cancelled.");
            publisher.publish(fmt);
            if (controller != null) {
                controller.setUserState(userId, State.IDLE);
            }
            return false;
        }
        JSONObject foodScoreJSON = getMenuScore(menuJSON);
        generateRecommendation(foodScoreJSON);
        return true;
    }

    /**
     * Give recommendation given a MenuJSON.
     * @param menuJSON A JSONObject of MenuJSON format.
     * @return A JSONObject of FoodScoreJSON format.
     */
    public JSONObject getMenuScore(JSONObject menuJSON) {
        JSONObject foodScoreJSON = new JSONObject();
        String userId = menuJSON.getString("userId");
        foodScoreJSON.put("userId", userId);
        JSONArray results = new JSONArray();
        JSONArray menu = menuJSON.getJSONArray("menu");
        for (int i=0; i<menu.length(); ++i) {
            JSONObject dish = menu.getJSONObject(i);
            JSONObject dishResult = new JSONObject();
            String dishName = dish.getString("dishName");
            JSONArray foodContent = dish.getJSONArray("foodContent");
            dishResult.put("dishName", dishName);
            dishResult.put("score", calculateScore(userId, foodContent));
            dishResult.put("portionSize", calculatePortionSize(userId, foodContent));
            results.put(dishResult);
        }
        foodScoreJSON.put("results", results);
        log.info("Calculated food score JSON:\n{}",
            foodScoreJSON.toString(4));
        return foodScoreJSON;
    }

    /**
     * Generate recommendation based on FoodScoreJSON.
     * @param foodScoreJSON A JSONObject of FoodScoreJSON format.
     */
    public void generateRecommendation(JSONObject foodScoreJSON) {
        String userId = foodScoreJSON.getString("userId");
        JSONArray results = foodScoreJSON.getJSONArray("results");

        // find best dish
        double highestScore = -1;
        JSONObject bestDish = null;
        for (int i=0; i<results.length(); ++i) {
            JSONObject dishResult = results.getJSONObject(i);
            double currentScore = dishResult.getDouble("score");
            if (currentScore <= highestScore) continue;
            highestScore = currentScore;
            bestDish = dishResult;
        }
        log.info("Best dish:\n{}", bestDish.toString(4));

        String replyText = generateReplyText(bestDish);

        // publish message
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage(replyText)
           .appendTextMessage("And please tell me when you finish your meal :)");
        publisher.publish(fmt);
        return;
    }

    /**
     * Generate reply text with reasons given the best dish result.
     * @param dishResult A JSONObject containing info of the best dish.
     * @return A String of reply text.
     */
    public String generateReplyText(JSONObject dishResult) {
        String dishName = dishResult.getString("dishName");
        int portionSize = dishResult.getInt("portionSize");
        return String.format("You should choose this dish: %s\n" +
            "And the recommended portion size is %d gram",
            dishName, portionSize);
    }

    /**
     * Helper function for getting a JSONArray of FoodJSON given foodContent.
     * @param foodContent A JSONArray of food content.
     * @return A JSONArray of FoodJSON.
     */
    public JSONArray getFoodJSON(JSONArray foodContent) {
        FoodQuerier foodQuerier = new FuzzyFoodQuerier();
        JSONArray foodList = new JSONArray();
        for (int i=0; i<foodContent.length(); ++i) {
            int index = foodContent.getJSONObject(i).getInt("idx");
            foodList.put(foodQuerier.get(index));
        }
        foodQuerier.close();
        return foodList;
    }

    /**
     * Helper function for getting a UserJSON given user Id.
     * @param userId String of user Id.
     * @return A UserJSON.
     */
    public JSONObject getUserJSON(String userId) {
        UserQuerier userQuerier = new UserQuerier();
        JSONObject userJSON = userQuerier.get(userId);
        userQuerier.close();
        return userJSON;
    }

    /**
     * Calculate score given food content of a dish.
     * @param userId String of userId.
     * @param foodContent A JSONArray representing the content of a dish.
     * @return Score of the dish.
     */
    public double calculateScore(String userId, JSONArray foodContent) {
        JSONArray foodList = getFoodJSON(foodContent);
        JSONObject userJSON = getUserJSON(userId);
        double score = 0;

        double averageCalorie = getAverageNutrient(foodList, "energ_kcal");
        double servingSize = 2000 / averageCalorie;

        for (int i=0; i<configuration.length(); ++i) {
            JSONObject config = configuration.getJSONObject(i);
            String name = config.getString("name");
            double y = config.getDouble("y");
            double proportion = config.getDouble("proportion");
            double k = servingSize * getAverageNutrient(foodList, name);
            double t = k / y - 1;
            score += Math.exp(-t * t / 2) * proportion;
        }

        return score;
    }

    /**
     * Calculate portion size given food content of a dish.
     * @param userId String of userId.
     * @param foodContent A JSONArray representing the content of a dish.
     * @return Recommended portion size in gram.
     */
    public int calculatePortionSize(String userId, JSONArray foodContent) {
        JSONArray foodList = getFoodJSON(foodContent);
        JSONObject userJSON = getUserJSON(userId);

        double averageCalorie = getAverageNutrient(foodList, "energ_kcal");
        double userBMR = getUserBMR(userJSON) * 1.2;
        double rawPortionSize = 100 * userBMR / (3 * averageCalorie);
        log.info("averageCalorie: " + averageCalorie);
        log.info("user BMR: " + userBMR);
        log.info("raw portion size: " + rawPortionSize);
        int roundedPortionSize = (int)Math.round(rawPortionSize / 50) * 50;
        return roundedPortionSize;
    }

    /**
     * Helper function for calculating average nutrient content.
     * @param list A JSONArray whose entries are FoodJSON.
     * @param nutrientName Name of nutrient in string.
     * @return The average amount of nutrient.
     */
    public double getAverageNutrient(JSONArray list, String nutrientName) {
        double sum = 0;
        int cnt = 0;
        for (int i=0; i<list.length(); ++i) {
            try {
                sum += list.getJSONObject(i).getDouble(nutrientName);
                ++cnt;
            } catch (JSONException e) {
                log.info(e.toString());
            }
        }
        if (cnt > 0) return sum / cnt;
        else return 0;
    }

    /**
     * Calculate BMR for a user.
     * @param userJSON JSONObject of UserJSON format.
     * @return BMR value.
     */
    public double getUserBMR(JSONObject userJSON) {
        String gender = userJSON.getString("gender");
        double weight = userJSON.getDouble("weight");
        double height = userJSON.getDouble("height");
        int age = userJSON.getInt("age");
        switch (gender) {
            case "male":
                return 66.47 + 13.7*weight + 5*height - 6.8*age;
            case "female":
                return 655.1 + 9.6*weight + 1.8*height - 4.7*age;
            default:
                log.info("Invalid gender {}", gender);
                return 0;
        }
    }
}