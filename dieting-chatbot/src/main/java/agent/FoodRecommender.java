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

import java.time.LocalDateTime;
import java.util.*;
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

    private static Map<String, Double> mealTypeToPortions;
    static {
        mealTypeToPortions = new LinkedHashMap<>();
        mealTypeToPortions.put("breakfast", 0.2);
        mealTypeToPortions.put("brunch", 0.35);
        mealTypeToPortions.put("lunch", 0.4);
        mealTypeToPortions.put("afternoon tea", 0.15);
        mealTypeToPortions.put("dinner", 0.4);
        mealTypeToPortions.put("supper", 0.15);
    }

    private static Map<String, Double> exerciseRateToIntakeRatios;
    static {
        exerciseRateToIntakeRatios = new LinkedHashMap<>();
        exerciseRateToIntakeRatios.put("no or little", 1.2);
        exerciseRateToIntakeRatios.put("light", 1.375);
        exerciseRateToIntakeRatios.put("moderate", 1.55);
        exerciseRateToIntakeRatios.put("active", 1.725);
        exerciseRateToIntakeRatios.put("intensive", 1.9);
    }

    /**
     * User menus internal memory for food recommendation.
     */
    private static Map<String, JSONObject> menus = new HashMap<>();

    /**
     * User states tracking for interaction.
     * 0 stands for just entering and asked meal type
     * 1 stands for collected meal type and asked exercise rate
     * 2 stands for collected exercise rate and published recommendation and reasons
     */
    private static Map<String, Integer> states = new HashMap<>();

    /**
     * Number of meals internal memory for food recommendation.
     * Storing a portion standing for the portion of nutrients should get from this meal.
     */
    private static Map<String, Double> mealPortions = new HashMap<>();

    /**
     * Exercise rates internal memory for food recommendation.
     * Storing a ratio standing for the daily intake over BMR for the day.
     */
    private static Map<String, Double> exerciseIntakeRatios = new HashMap<>();

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

    /**
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
                states.remove(userId);
                mealPortions.remove(userId);
                exerciseIntakeRatios.remove(userId);
                log.info("Remove menu and internal states and memory of user {}", userId);
            }
            return;
        }

        log.info("Entering FoodRecommender");
        publisher.publish(new FormatterMessageJSON(userId));

        if (psr.getType().equals("transition") || !states.containsKey(userId)) {
            if (menus.containsKey(userId)) {
                boolean isValid = checkUserInfo(userId);
                if (isValid) {
                    states.put(userId, 0);
                    askMealType(userId);
                }
            } else {
                FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                fmt.appendTextMessage("Seems that I don't have your menu yet. " +
                    "Session cancelled.");
                publisher.publish(fmt);
                if (controller != null) {
                    controller.setUserState(userId, State.IDLE);
                }
            }
        } else if (psr.getType().equals("text")) {
            String msg = psr.get("textContent");
            if (states.get(userId) == 0) {
                boolean isValid = parseMealType(userId, msg);
                if (isValid) {
                    states.put(userId, 1);
                    askExerciseRate(userId, msg);
                }
            } else if (states.get(userId) == 1) {
                boolean isValid = parseExerciseRate(userId, msg);
                if (isValid) {
                    states.put(userId, 2);
                    JSONObject menuJSON = menus.get(userId);
                    JSONObject foodScoreJSON = getMenuScore(menuJSON);
                    generateRecommendation(foodScoreJSON);
                    // reasons here
                    // ending words here
                }
            } else if (states.get(userId) == 2) {
                if (isContaining(msg, new HashSet<>(Arrays.asList("yes", "right", "true", "yep", "finished", "finish", "done")))) {
                    // No need to clear internal states and memory here
                    if (controller != null) {
                        controller.setUserState(userId, State.RECORD_MEAL);
                    }
                } else if (isContaining(msg, new HashSet<>(Arrays.asList("reference", "how", "why")))){
                    // reference here
                } else {
                    FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                    fmt.appendTextMessage("Did you mean you have finished your meal? " +
                            "If yes, please tell me and I will record what you have eaten.");
                    publisher.publish(fmt);
                }
            } else {
                log.error(String.format("Invalid internal state %d of user %s encountered in FoodRecommender.",
                        states.get(userId), userId));

            }
        }
    }

    public void askMealType(String userId) {
        LocalDateTime date = LocalDateTime.now();
        // Add 8 since our users are in UTC+8 time zone.
        double hours = (double)(date.toLocalTime().toSecondOfDay()) / 60.0 / 60.0 + 8.0;
        String mealType;
        if (hours > 4.5 && hours < 9.5) mealType = "breakfast";
        else if (hours > 9 && hours < 11) mealType = "breakfast or brunch";
        else if (hours > 10.5 && hours < 14.5) mealType = "lunch";
        else if (hours > 14 && hours < 17 ) mealType = "afternoon tea";
        else if (hours > 16.5 && hours < 20.5) mealType = "dinner";
        else mealType = "supper";
        // Store default meal portion at first
        mealPortions.put(userId, mealTypeToPortions.get(mealType));
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Ok, let us analyze what you should eat for this meal. " +
                "Before that, could you tell me which meal you are eating?");
        fmt.appendTextMessage(String.format("According to the time, I guess you are eating %s. Is that correct?", mealType));
        fmt.appendTextMessage("Please confirm that or tell me which meal you are eating directly. " +
                "The valid options are \'breakfast\', \'brunch\', \'lunch\', \'afternoon tea\', \'dinner\' and \'supper\'.");
        publisher.publish(fmt);
    }

    public boolean parseMealType(String userId, String msg) {
        boolean isValid = false;
        if (isContaining(msg, new HashSet<>(Arrays.asList("confirm", "yes", "right", "true", "yep")))) isValid = true;
        else {
            for (String mealType : mealTypeToPortions.keySet()) {
                if(isContaining(msg, mealType)) {
                    mealPortions.put(userId, mealTypeToPortions.get(mealType));
                    isValid = true;
                    break;
                }
            }
        }
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (isValid) {
            fmt.appendTextMessage("Great, I've recorded your meal type!");
        } else {
            fmt.appendTextMessage("Sorry, I don't understand what you are saying. Tell me a valid meal type please.");
        }
        publisher.publish(fmt);
        return isValid;
    }

    public void askExerciseRate(String userId, String msg) {
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Another question is about you exercise rate. " +
                "Could you tell me how much physical exercises you are doing recently?");
        fmt.appendTextMessage("Valid options include \'no or little\', \'light\', \'moderate\', \'active\', \'intensive\'.");
        publisher.publish(fmt);
    }

    public boolean parseExerciseRate(String userId , String msg) {
        boolean isValid = false;
        for (String exerciseRate : exerciseRateToIntakeRatios.keySet()) {
            if(isContaining(msg, exerciseRate)) {
                exerciseIntakeRatios.put(userId, exerciseRateToIntakeRatios.get(exerciseRate));
                isValid = true;
                break;
            }
        }
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (isValid) {
            fmt.appendTextMessage("Thanks a lot, I've recorded your exercise rate!");
        } else {
            fmt.appendTextMessage("Sorry, I don't understand ... Give me a valid exercise rate please.");
        }
        publisher.publish(fmt);
        return isValid;
    }

    /**
     * Helper function for deciding whether input message contains one of the key words.
     * @param msg User input message.
     * @return Whether user means meal finished.
     */
    public boolean isContaining(String msg, String keyWord) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (keyWord.equals(word)) return true;
        }
        return false;
    }

    /**
     * Helper function for deciding whether input message contains one of the key words.
     * @param msg User input message.
     * @return Whether user means meal finished.
     */
    public boolean isContaining(String msg, Set<String> keyWords) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (keyWords.contains(word)) return true;
        }
        return false;
    }

    public boolean checkUserInfo(String userId) {
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
        double servingSize = getDailyIntake(userJSON) / averageCalorie;
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
        // 100 factor comes from the energy_kcal are listed in unit kcal/100g
        double rawPortionSize = 100 * getMealIntake(userJSON) / averageCalorie;
        log.info("average calorie: " + averageCalorie);
        log.info("user daily intake: " + getDailyIntake(userJSON));
        log.info("user BMR: " + getUserBMR(userJSON));
        log.info("user meal intake: " + getMealIntake(userJSON));
        log.info("raw portion size: " + rawPortionSize);
        return (int)Math.round(rawPortionSize / 10) * 10;
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

    public double getMealIntake(JSONObject userJSON) {
        String userId = userJSON.getString("id");
        return getUserBMR(userJSON) * exerciseIntakeRatios.get(userId) * mealPortions.get(userId);
    }

    public double getDailyIntake(JSONObject userJSON) {
        String userId = userJSON.getString("id");
        return getUserBMR(userJSON) * exerciseIntakeRatios.get(userId);
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