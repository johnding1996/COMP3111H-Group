package agent;

import database.keeper.HistKeeper;
import database.keeper.MenuKeeper;
import database.querier.UserQuerier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;

import controller.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;
import utility.FormatterMessageJSON;
import utility.JsonUtility;
import utility.ParserMessageJSON;
import utility.Validator;

import lombok.extern.slf4j.Slf4j;

/**
 * MealRecorder: record the dish user ate.
 * 
 * State mapping:
 *      0 - ask dish
 *      1 - ask portion size
 *      2 - ask weight
 *      3 - say goodbye
 * 
 * @author cliubf, mcding, szhouan
 * @version v2.0.0
 */
@Slf4j
@Component
public class MealRecorder extends Agent {

    /**
     * This is a private attribute userManager.
     */
    @Autowired
    private UserManager userManager;

    /**
     * This is a private attribute MenuManager.
     */
    @Autowired
    private MenuManager menuManager;

    /**
     * Initialize meal recorder agent.
     */
    @Override
    public void init() {
        agentName = "MealRecorder";
        agentStates = new HashSet<>(
            Arrays.asList(State.RECORD_MEAL)
        );
        handleImage = false;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> askDish(psr))
            .addHandler(1, (psr) -> askPortion(psr))
            .addHandler(2, (psr) -> askWeight(psr))
            .addHandler(3, (psr) -> sayGoodbye(psr));
    }

    /**
     * Handler for asking dish.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askDish(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        JSONObject menuJSON = menuManager.getMenuJSON(userId);
        states.get(userId).put("menuJSON", menuJSON);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage(
            "Welcome back! Please choose what you just ate in this list:")
           .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, false))
           .appendTextMessage("Please choose one of them and input the number.");
        publisher.publish(fmt);
        return 1;
    }

    /**
     * Handler for asking portion.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askPortion(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        boolean valid = true;
        if (!Validator.isInteger(text)) {
            valid = false;
        } else {
            int dishId = Integer.parseInt(text);
            if (dishId < 1 || dishId > states.get(userId)
                .getJSONObject("menuJSON").getJSONArray("menu")
                .length()) valid = false;
        }
        if (!valid) {
            rejectUserInput(psr, "Please choose among the dishes.");
            return 1;
        }

        int dishId = Integer.parseInt(text) - 1;
        states.get(userId).put("dishId", dishId);
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Great! " +
                "I have recorded what you have just eaten!")
            .appendTextMessage("And what is the portion size of it? (in gram)");
        publisher.publish(fmt);

        return 2;
    }

    /**
     * Handler for asking weight.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askWeight(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        boolean valid = true;
        if (!Validator.isInteger(text)) {
            valid = false;
        } else {
            int portionSize = Integer.parseInt(text);
            if (portionSize <= 0 || portionSize > 5000) {
                valid = false;
            }
        }
        if (!valid) {
            rejectUserInput(psr, null);
            return 2;
        }

        int portionSize = Integer.parseInt(text);
        states.get(userId).put("portionSize", portionSize);
        int dishId = states.get(userId).getInt("dishId");
        String dishName = states.get(userId).getJSONObject("menuJSON")
            .getJSONArray("menu").getJSONObject(dishId).getString("name");
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage(
            String.format("So you have eaten %d gram of the dish %s",
            portionSize, dishName))
           .appendTextMessage("One more question: what is your weight now?");
        publisher.publish(fmt);

        return 3;
    }

    /**
     * Handler for say goodbye.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int sayGoodbye(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        boolean valid = true;
        if (!Validator.isInteger(text)) valid = false;
        else if (!Validator.validateWeight(Integer.parseInt(text)))
            valid = false;
        if (!valid) {
            rejectUserInput(psr, "This is not a valid weight.");
            return 3;
        }
        int weight = Integer.parseInt(text);
        states.get(userId).put("weight", weight);
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage(String.format("So your weight now is %d kg", weight))
           .appendTextMessage("See you ^_^");
        publisher.publish(fmt);
        updateDatabase(userId);
        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * add userInfo to history if everything is correct.
     * @param userId String of userId
     */
    public void updateDatabase (String userId) {
        MenuKeeper menuKeeper = new MenuKeeper();
        HistKeeper histKeeper = new HistKeeper();
        JSONObject histJson = new JSONObject();
        try{
            // Add hist to HistKeeper
            DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            histJson.put("timestamp", dateTimeFormat.format(new Date()));
            histJson.put("weight", states.get(userId).get("weight"));
            histJson.put("portionSize", states.get(userId).get("portionSize"));
            log.error("all" + menuKeeper.get(userId, 1).toString());
            JSONArray menu = menuKeeper.get(userId, 1).getJSONObject(0).getJSONArray("menu");
            log.error("menu" + menu.toString());
            JSONArray selectedMenu = new JSONArray();
            selectedMenu.put(menu.getJSONObject(states.get(userId).getInt("dishId")));
            log.error("selected menu" + selectedMenu.toString());
            histJson.put("menu", selectedMenu);
            histKeeper.set(userId, histJson);
            log.info(String.format("Stored the user history of user %s in to the caches.", userId));
            // Update weight in UserInfo table
            JSONObject userJSON = userManager.getUserJSON(userId);
            userJSON.put("weight", states.get(userId).get("weight"));
            userManager.storeUserJSON(userId, userJSON);

        } catch (JSONException e) {
            log.error("Error encountered when parsing the MealJSON.", e);
        }
        menuKeeper.close();
        histKeeper.close();
    }
}