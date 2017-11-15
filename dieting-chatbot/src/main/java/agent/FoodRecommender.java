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
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * FoodRecommender: calculate scores for each dish and generate reasons for recommendation.
 * @author mcding, agong, szhouan
 * @version v2.0.0
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

    private static JSONArray nutrientDailyIntakes;
    static {
        nutrientDailyIntakes = new JSONArray();
        nutrientDailyIntakes.put(packSingleIntakeJSON("lipid_tot", 70, 15, "g", "fat"))
                     .put(packSingleIntakeJSON("carbohydrt", 260, 15, "g", "carbohydrate"))
                     .put(packSingleIntakeJSON("sugar_tot", 90, 15, "g", "sugar"))
                     .put(packSingleIntakeJSON("protein", 50, 15, "g", "protein"))
                     .put(packSingleIntakeJSON("fiber_td", 30, 5, "g", "dietary fiber"))
                     .put(packSingleIntakeJSON("vit_c", 35, 10, "mg", "vitamin C"))
                     .put(packSingleIntakeJSON("sodium", 1600, 10, "mg", "sodium"))
                     .put(packSingleIntakeJSON("potassium", 2000, 5, "mg", "potassium"))
                     .put(packSingleIntakeJSON("calcium", 800, 5, "mg", "calcium"));
    }

    private static Map<String, Double> mealTypeToPortions;
    static {
        mealTypeToPortions = new LinkedHashMap<>();
        mealTypeToPortions.put("breakfast", 0.2);
        mealTypeToPortions.put("brunch", 0.35);
        mealTypeToPortions.put("lunch", 0.4);
        mealTypeToPortions.put("tea", 0.15);
        mealTypeToPortions.put("dinner", 0.4);
        mealTypeToPortions.put("supper", 0.15);
    }

    private static Map<String, Double> exerciseRateToIntakeRatios;
    static {
        exerciseRateToIntakeRatios = new LinkedHashMap<>();
        exerciseRateToIntakeRatios.put("little", 1.2);
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
    private static JSONObject packSingleIntakeJSON(String name, double y, double proportion, String unit, String desc) {
        JSONObject json = new JSONObject();
        json.put("name", name)
            .put("y", y)
            .put("proportion", proportion)
            .put("unit", unit)
            .put("desc", desc);
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
        State state = psr.getState();
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
                    sleep(4);
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
                    sleep(2);
                    askExerciseRate(userId);
                }
            } else if (states.get(userId) == 1) {
                boolean isValid = parseExerciseRate(userId, msg);
                if (isValid) {
                    states.put(userId, 2);
                    sleep(2);
                    openingWords(userId);
                    sleep(3);
                    JSONObject menuJSON = menus.get(userId);
                    JSONObject foodScoreJSON = getMenuScore(menuJSON);
                    generateRecommendation(foodScoreJSON);
                    sleep(4);
                    closingWords(userId);
                }
            } else if (states.get(userId) == 2) {
                if (isContaining(msg, new HashSet<>(Arrays.asList("yes", "right", "true", "yep", "finished", "finish", "done")))) {
                    // No need to clear internal states and memory here
                    if (controller != null) {
                        controller.setUserState(userId, State.RECORD_MEAL);
                    }
                } else if (isContaining(msg, new HashSet<>(Arrays.asList("reference", "how", "why")))){
                    reference(userId);
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

    /**
     * Ask user to for meal type.
     * @param userId String of user id.
     */
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

    /**
     * Parse the user input meal type.
     * @param userId String of user id
     * @param msg user input message
     * @return whether parsing successfully or not
     */
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

    /**
     * Ask user for exercise rate.
     * @param userId String of user id.
     */
    public void askExerciseRate(String userId) {
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Another question is about you exercise rate. " +
                "Could you tell me how much physical exercises you are doing recently?");
        fmt.appendTextMessage("Valid options include \'little\', \'light\', \'moderate\', \'active\', \'intensive\'.");
        publisher.publish(fmt);
    }

    /**
     * Parse user input exercise rate.
     * @param userId String of user id.
     * @param msg user input message.
     * @return whether parsing successfully or not.
     */
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
     * @param keyWord a single key word to check.
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
     * @param keyWords a set of key words to check.
     * @return Whether user means meal finished.
     */
    public boolean isContaining(String msg, Set<String> keyWords) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (keyWords.contains(word)) return true;
        }
        return false;
    }

    /**
     * Wrapper function to check user has input the menu information or not.
     * @param userId String of user id.
     * @return whether check passed or not.
     */
    public boolean checkUserInfo(String userId) {
        JSONObject userJSON = getUserJSON(userId);
        if (userJSON == null) {
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Sorry, I don't have your personal " +
               "information yet, so I could not give you recommendation. " +
               "Please set your personal information first. " +
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
            dishResult.put("energy", calculateEnergyIntakes(userId, foodContent));
            dishResult.put("nutrient", calculateNutrientIntakes(userId, foodContent));
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
        int verboseConfig = 2;
        int nDishes = results.length();
        int nNutrients = nutrientDailyIntakes.length();
        // Order and index of total scores
        double[] totalScore = new double[nDishes];
        for (int i=0; i<nDishes; ++i) {
            totalScore[i] = results.getJSONObject(i).getJSONObject("score").getDouble("total");
        }
        double[] totalIndex = getIndex(totalScore);
        // Order and index of each nutrient's scores
        double[][] nutrientIndex = new double[nDishes][];
        for (int i=0; i<nDishes; ++i) {
            JSONObject dish = results.getJSONObject(i);
            double[] score = new double[nNutrients];
            for (int j=0; j<nNutrients; ++j) {
                String nutrient = nutrientDailyIntakes.getJSONObject(j).getString("name");
                score[j] = dish.getJSONObject("score").getDouble(nutrient);
            }
            nutrientIndex[i] = getIndex(score);
        }
        // Start generating texts
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        // Suggestion
        JSONObject bestDish = results.getJSONObject((int)totalIndex[0]);
        String bestDishName = bestDish.getString("dishName");
        int bestDishPortionSize = (int)Math.round(bestDish.getJSONObject("energy").getDouble("portionSize")/5)*5;
        fmt.appendTextMessage(String.format("We suggest you to eat this dish: %s\n" +
                        "And the recommended portion size is %d gram", bestDishName, bestDishPortionSize));
        // Reason for the portion size
        fmt.appendTextMessage(String.format("According to your age, weight and height, your BMR is around %d kcal/day. " +
                "Based on yout exercise rate, your daily intake should be %d kcal, " +
                "and you should intake %d kcal for this meal, " +
                "since %s has on average %d kcal energy per 100 g, " +
                "you should take around %d gram of it.",
                (int)bestDish.getJSONObject("energy").getDouble("BMR"),
                (int)bestDish.getJSONObject("energy").getDouble("dailyIntake"),
                (int)bestDish.getJSONObject("energy").getDouble("mealIntake"),
                bestDishName,
                (int)bestDish.getJSONObject("energy").getDouble("energ_kcal"),
                bestDishPortionSize));
        // Reason for choosing the highest score dish (it must exist)
        List<String> bestReasons = new ArrayList<>();
        for (int k=0; k<verboseConfig; k++) {
            String nutrient = nutrientDailyIntakes.getJSONObject((int)nutrientIndex[(int)totalIndex[0]][k]).getString("name");
            String nutrientDescription = nutrientDailyIntakes.getJSONObject((int)nutrientIndex[(int)totalIndex[0]][k]).getString("desc");
            JSONObject intakeJSON = bestDish.getJSONObject("nutrient").getJSONObject(nutrient);
            String resonNutrient = String.format("you can get %d %s of %s from %s which is " +
                    "relatively close to your recommended intake of %s through this meal, %d %s",
                    (int)intakeJSON.getDouble("actual"),
                    intakeJSON.getString("unit"),
                    nutrientDescription,
                    bestDishName,
                    nutrientDescription,
                    (int)intakeJSON.getDouble("expect"),
                    intakeJSON.getString("unit"));
            bestReasons.add(resonNutrient);
        }
        fmt.appendTextMessage(String.format("We recommend %s instead of others, since ", bestDishName) +
                String.join(", ", bestReasons) + ".");
        // Reason for not choosing the lowest score (if not only one dish)
        // If only one dish, publish now and return
        if (nDishes == 1) {
            publisher.publish(fmt);
            return;
        }
        JSONObject worstDish = results.getJSONObject((int)totalIndex[totalIndex.length-1]);
        String worstDishName = worstDish.getString("dishName");
        List<String> worstReasons = new ArrayList<>();
        for (int k=0; k<verboseConfig; k++) {
            String nutrient = nutrientDailyIntakes.getJSONObject((int)nutrientIndex[(int)totalIndex[totalIndex.length-1]][nNutrients-k-1]).getString("name");
            String nutrientDescription = nutrientDailyIntakes.getJSONObject((int)nutrientIndex[(int)totalIndex[totalIndex.length-1]][nNutrients-k-1]).getString("desc");
            JSONObject intakeJSON = worstDish.getJSONObject("nutrient").getJSONObject(nutrient);
            String resonNutrient = String.format("you would get %d %s of %s from %s which is " +
                            "far from to your recommended intake of %s through this meal, %d %s",
                    (int)intakeJSON.getDouble("actual"),
                    intakeJSON.getString("unit"),
                    nutrientDescription,
                    worstDishName,
                    nutrientDescription,
                    (int)intakeJSON.getDouble("expect"),
                    intakeJSON.getString("unit"));
            worstReasons.add(resonNutrient);
        }
        fmt.appendTextMessage(String.format("We dose not recommend %s, because ", worstDishName) +
                String.join(", ", worstReasons) + ".");
        // Finally publish the reasons
        publisher.publish(fmt);
    }

    /**
     * Print opening words.
     * @param userId String of user id.
     */
    public void openingWords(String userId) {
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("That's all the information we want. Hold tight, " +
                "we are generating a personalized recommendation for you!\n... ... ...");
        publisher.publish(fmt);
    }

    /**
     * Print closing words.
     * @param userId String of user id.
     */
    public void closingWords(String userId) {
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Above are all the recommendations we give. " +
                "Please tell me when you finish your meal so that we can record it for later analysis.");
        fmt.appendTextMessage("If you are curious about how we generate this recommendation, and the mechanism behind, " +
                "feel free to let me know!");
        fmt.appendTextMessage("Wish you have a good meal!");
        publisher.publish(fmt);
    }

    /**
     * Print reference words.
     * @param userId String of user id.
     */
    public void reference(String userId) {
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Thanks a lot for your interests!");
        fmt.appendTextMessage("We generate recommendation by assigning each dish with a score and order them.");
        fmt.appendTextMessage("The score of each dish is calculated based on the well-known formula basal metabolic rate (BMR) " +
                "(which makes use of the your age, recent weight, and height), " +
                "and the recommended daily intake of 9 nutrients " +
                "(fat, carbohydrate, sugar, protein, dietary fiber, vitamin C, and 3 kinds of minerals).");
        fmt.appendTextMessage("To give more personalized recommendation, we also takes your exercise rate and current meal type in to consideration, " +
                "to calculate your expected meal energy intake.");
        fmt.appendTextMessage("Generally, the higher the score a dish has, the more nutrients it supplies meet our recommended value. " +
                "We also carefully analyze 9 sub-scores for each dish you input, and generate persuasive reasons based on them.");
        publisher.publish(fmt);
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
        if (userJSON == null) {
            log.error("Obtain a null userJSON from database.");
        }
        userQuerier.close();
        return userJSON;
    }

    /**
     * Calculate score given food content of a dish.
     * @param userId String of userId.
     * @param foodContent A JSONArray representing the content of a dish.
     * @return Score of the dish.
     */
    public JSONObject calculateScore(String userId, JSONArray foodContent) {
        JSONArray foodList = getFoodJSON(foodContent);
        JSONObject userJSON = getUserJSON(userId);
        JSONObject scoreJSON = new JSONObject();
        double totalScore = 0;
        double averageCalorie = getAverageNutrient(foodList, "energ_kcal");
        for (int i = 0; i< nutrientDailyIntakes.length(); ++i) {
            JSONObject config = nutrientDailyIntakes.getJSONObject(i);
            String name = config.getString("name");
            double y = config.getDouble("y");
            double proportion = config.getDouble("proportion");
            double k = getDailyIntake(userJSON) * getAverageNutrient(foodList, name) / averageCalorie;
            double t = k / y - 1;
            double score = Math.exp(-t * t / 2) * proportion;
            scoreJSON.put(name, score);
            totalScore += score;
        }
        scoreJSON.put("total", totalScore);
        return scoreJSON;
    }

    /**
     * Calculate score given food content of a dish.
     * @param userId String of userId.
     * @param foodContent A JSONArray representing the content of a dish.
     * @return Score of the dish.
     */
    public JSONObject calculateNutrientIntakes(String userId, JSONArray foodContent) {
        JSONArray foodList = getFoodJSON(foodContent);
        JSONObject userJSON = getUserJSON(userId);
        JSONObject intakeJSON = new JSONObject();
        double averageCalorie = getAverageNutrient(foodList, "energ_kcal");
        for (int i = 0; i< nutrientDailyIntakes.length(); ++i) {
            JSONObject dailyIntakes = nutrientDailyIntakes.getJSONObject(i);
            String nutrient = dailyIntakes.getString("name");
            JSONObject nutrientIntakeJSON = new JSONObject();
            double recommendedIntake = getMealIntake(userJSON) * dailyIntakes.getDouble("y") / getDailyIntake(userJSON);
            double actualIntake = getMealIntake(userJSON) * getAverageNutrient(foodList, nutrient) / averageCalorie;
            String unit = dailyIntakes.getString("unit");
            nutrientIntakeJSON.put("expect", recommendedIntake);
            nutrientIntakeJSON.put("actual", actualIntake);
            nutrientIntakeJSON.put("unit", unit);
            intakeJSON.put(nutrient, nutrientIntakeJSON);
        }
        return intakeJSON;
    }

    /**
     * Calculate portion size given food content of a dish.
     * @param userId String of userId.
     * @param foodContent A JSONArray representing the content of a dish.
     * @return Recommended portion size in gram.
     */
    public JSONObject calculateEnergyIntakes(String userId, JSONArray foodContent) {
        JSONArray foodList = getFoodJSON(foodContent);
        JSONObject userJSON = getUserJSON(userId);
        JSONObject sizeJSON = new JSONObject();
        double averageCalorie = getAverageNutrient(foodList, "energ_kcal");
        // 100 factor comes from the energy_kcal are listed in unit kcal/100g
        double rawPortionSize = 100 * getMealIntake(userJSON) / averageCalorie;
        log.info("average calorie: " + averageCalorie);
        log.info("user daily intake: " + getDailyIntake(userJSON));
        log.info("user BMR: " + getUserBMR(userJSON));
        log.info("user meal intake: " + getMealIntake(userJSON));
        log.info("raw portion size: " + rawPortionSize);
        sizeJSON.put("energ_kcal", averageCalorie);
        sizeJSON.put("BMR", getUserBMR(userJSON));
        sizeJSON.put("dailyIntake", getDailyIntake(userJSON));
        sizeJSON.put("mealIntake", getMealIntake(userJSON));
        sizeJSON.put("portionSize", rawPortionSize);
        return sizeJSON;
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
     * Get the user recommended meal intake.
     * @param userJSON String of user id.
     * @return recommend meal intake of that user.
     */
    public double getMealIntake(JSONObject userJSON) {
        String userId = userJSON.getString("id");
        return getUserBMR(userJSON) * exerciseIntakeRatios.get(userId) * mealPortions.get(userId);
    }

    /**
     * Get the user recommended daily intake.
     * @param userJSON String of user id.
     * @return recommend daily intake of that user.
     */
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

    /**
     * Sleep for a few seconds, used for pursuing the order or messages.
     * @param seconds the number of seconds to sleep.
     */
    public void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e ) {
            log.warn("Sleeping in FoodRecommender got interrupted.");
        }
    }

    /**
     * Helper function to get the index of a double array after it is ordered in descending order.
     * @param array input double array.
     * @return index array, note that it is double array.
     */
    public double[] getIndex(double[] array) {
        int size = array.length;
        double temp;
        double[] index = new double[size];
        for (int i=0; i<size; i++) {
            index[i] = i;
        }
        for(int i=0; i < size; i++){
            for(int j=1; j < (size-i); j++){
                if(array[j-1] < array[j]){
                    //swap elements
                    temp = array[j-1];
                    array[j-1] = array[j];
                    array[j] = temp;
                    //swap indices
                    temp = index[j-1];
                    index[j-1] = index[j];
                    index[j] = temp;
                }

            }
        }
        log.info("getIndex: input: " + Arrays.toString(array) +
                "index:" + Arrays.toString(index));
        return index;
    }

    /**
     * Helper function to modify internal exerciseIntakeRatios memory for testing.
     * @param userId String of user id.
     * @param ratio ratio.
     */
    public void addExerciseIntakeRatios(String userId, double ratio) {
        exerciseIntakeRatios.put(userId, ratio);
    }

    /**
     * Helper function to modify internal mealPortions memory for testing.
     * @param userId String of user id.
     * @param portion portion.
     */
    public void addMealPortions(String userId, double portion) {
        mealPortions.put(userId, portion);
    }
}