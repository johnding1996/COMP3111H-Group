package agent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import utility.Validator;
import utility.JsonUtility;
import controller.State;
import database.querier.FoodQuerier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

/**
 * MealAsker: interact with user to get the appropriate menu.
 * @author szhouan
 * @version v2.1.0
 */
@Slf4j
@Component
public class MealAsker extends Agent {

    @Autowired
    private MenuManager menuManager;

    private HashMap<Integer, JSONObject> askerConfig = new HashMap<>();

    /**
     * Initialize initial input recorder agent.
     */
    @Override
    public void init() {
        agentName = "MealAsker";
        agentStates = new HashSet<>(
            Arrays.asList(State.ASK_MEAL)
        );
        handleImage = false;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> showMenu(psr))
            .addHandler(1, (psr) -> acceptMenuChange(psr))
            .addHandler(2, (psr) -> askFoodName(psr))
            .addHandler(3, (psr) -> askNutrient(psr))
            .addHandler(4, (psr) -> askNutrient(psr))
            .addHandler(5, (psr) -> askNutrient(psr))
            .addHandler(6, (psr) -> addNewFood(psr));
        
        askerConfig.put(4, (new JSONObject())
            .put("name", "energy").put("unit", "kcal/100g"));
        askerConfig.put(5, (new JSONObject())
            .put("name", "protein").put("unit", "g/100g"));
        askerConfig.put(6, (new JSONObject())
            .put("name", "lipid").put("unit", "g/100g"));
    }

    /**
     * Handler for showing menu.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int showMenu(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        JSONObject menuJSON = menuManager.getMenuJSON(userId);
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (menuJSON == null) {
            fmt.appendTextMessage("Looks like your menu is empty. Session cancelled.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        } else {
            // query database for food content
            JsonUtility.getFoodContent(menuJSON);

            // check no empty food content
            if (!checkNoEmptyFoodContent(menuJSON)) {
                fmt.appendTextMessage("Your menu input is invalid. Session cancelled.");
                publisher.publish(fmt);
                controller.setUserState(userId, State.IDLE);
                return END_STATE;
            }

            fmt.appendTextMessage("Well, I got your menu.")
               .appendTextMessage("The menu I got is\n" +
                        JsonUtility.formatMenuJSON(menuJSON, false))
               .appendTextMessage("And this is the food " +
                        "content of each dish I found:")
               .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, true))
               .appendTextMessage("Do you want to revise some dish names? " +
                    "You can type 'confirm' to go to next step. " +
                    "Please show me your revision in this format: " +
                    "<dish index>:<revised name>, such as '1:beef'");

            states.get(userId).put("menuJSON", menuJSON);

            publisher.publish(fmt);
            return 1;
        }
    }
    
    /**
     * Handler for accepting menu change from user.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int acceptMenuChange(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (TextProcessor.getMatch(TextProcessor.getTokens(text),
            Arrays.asList("confirm", "yes")) != null) {
            fmt.appendTextMessage("Bravo! Your update has been saved")
               .appendTextMessage("Is there any missing food on this list? " +
                    "Tell me Yes or No");
            publisher.publish(fmt);

            // update menu keeper
            menuManager.storeMenuJSON(userId,
                states.get(userId).getJSONObject("menuJSON"));
            return 2;
        }

        JSONObject menuJSON = states.get(userId).getJSONObject("menuJSON");
        JSONArray menu = menuJSON.getJSONArray("menu");
        String[] tokens = psr.get("textContent").split(":");
        boolean valid = true;
        int index = 0;
        String newName = "";

        if (!Validator.isInteger(tokens[0]) || tokens.length != 2) {
            valid = false;
        } else {
            index = Integer.parseInt(tokens[0]);
            newName = tokens[1];
            if (index < 1 || index > menu.length()) valid = false;
        }

        if (!valid) {
            rejectUserInput(psr, "Please enter in this format: " +
                "<dish index>:<new name>, " +
                "or type 'confirm' if no more update is desired.");
        } else {
            menu.getJSONObject(index-1).put("name", newName);
            JsonUtility.getFoodContent(menuJSON);

            // check no empty food content
            if (!checkNoEmptyFoodContent(menuJSON)) {
                fmt.appendTextMessage("Your menu input is invalid. Session cancelled.");
                publisher.publish(fmt);
                controller.setUserState(userId, State.IDLE);
                return END_STATE;
            }

            fmt.appendTextMessage("The Menu I got is\n" +
                JsonUtility.formatMenuJSON(menuJSON, true))
               .appendTextMessage("You could keep updating or confirm the menu");
            publisher.publish(fmt);
        }
        return 1;
    }
    
    /**
     * Handler for asking food name.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askFoodName(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").trim().toLowerCase();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if(text.equals("yes")) {
            fmt.appendTextMessage("So what is the name of this dish?");
            publisher.publish(fmt);

            states.get(userId).put("userNewFood", new JSONObject());
            return 3;
        } else if(text.equals("no")) {
            fmt.appendTextMessage("Alright, let's move on.");
            publisher.publish(fmt);

            controller.setUserState(userId, State.ASK_PORTION);
            return END_STATE;
        } else {
            rejectUserInput(psr, "Please tell me yes or no.");
            return 2;
        }
    }

    /**
     * Handler for asking new food nutrient.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askNutrient(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");
        int state = getUserState(userId);

        int nutrient = 0;
        JSONObject config = null;
        if (state == 3) {
            states.get(userId).getJSONObject("userNewFood").put("name", text);
        } else {
            config = askerConfig.get(state);
            if(!Validator.isInteger(text)) {
                rejectUserInput(psr, "Please use an integer");
                return state;
            } else {
                nutrient = Integer.parseInt(text);
                if (nutrient <= 0 || nutrient > 1000) {
                    rejectUserInput(psr, "Your input is not in valid range.");
                    return state;
                }
                states.get(userId).getJSONObject("userNewFood")
                    .put(config.getString("name"), nutrient);
            }
        }

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);

        config = askerConfig.get(state+1);
        fmt.appendTextMessage(String.format("Okay, so what is the %s contained " +
            "in this dish? (in terms of %s). Give me an integer please ~",
            config.getString("name"), config.getString("unit")));
        publisher.publish(fmt);
        return state+1;
    }
    
    /**
     * Handler for adding new food.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int addNewFood(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");
        int state = getUserState(userId);

        if (!Validator.isInteger(text)) {
            rejectUserInput(psr, "Your input is not an integer.");
            return state;
        } else {
            int nutrient = Integer.parseInt(text);
            JSONObject config = askerConfig.get(state);
            if (nutrient > 0 && nutrient <= 1000) {
                states.get(userId).getJSONObject("userNewFood")
                .put(config.getString("name"), nutrient);
            } else {
                rejectUserInput(psr, "Your input is not in valid range.");
                return state;
            }
        }

        // add new food to database and keeper
        storeNewFood(userId);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Alright, I have recorded the new food.");
        publisher.publish(fmt);

        controller.setUserState(userId, State.ASK_PORTION);
        return END_STATE;
    }

    /**
     * Store new food to database and menu keeper.
     * @param userId String of user Id.
     */
    void storeNewFood(String userId){
        JSONObject food = states.get(userId).getJSONObject("userNewFood");
        String name = food.getString("name");
        int energy = food.getInt("energy");
        int protein = food.getInt("protein");
        int lipid = food.getInt("lipid");

        JSONObject newDish = new JSONObject();
        newDish.put("shrt_desc", name)
               .put("energ_kcal", energy)
               .put("protein", protein)
               .put("lipid_tot", lipid);
        int id = getNewFoodId(newDish);

        JSONObject foodContent = new JSONObject();
        foodContent.put("idx", id);
        foodContent.put("description", name);
        JSONObject newFood = new JSONObject();
        newFood.put("name", name);
        newFood.put("foodContent", foodContent);
        JSONObject menuJSON = states.get(userId).getJSONObject("menuJSON");
        menuJSON.getJSONArray("menu").put(newFood);

        menuManager.storeMenuJSON(userId, menuJSON);
    }

    /**
     * Store the new dish to database, and return its id.
     * @param newDish a new dish supplied by user to store in database.
     * @return id of the row inserted
     */
    int getNewFoodId(JSONObject newDish){
        int id = 250;
        FoodQuerier querier = new FoodQuerier();
        boolean flag = false;
        while (!flag){
            newDish.put("ndb_no", ++id);
            flag = querier.add(newDish);
        }
        querier.close();
        return id;
    }

    /**
     * Check empty food content for a MenuJSON.
     * @param menuJSON input MenuJSON
     * @return whether there is no dish with empty food content
     */
    boolean checkNoEmptyFoodContent(JSONObject menuJSON) {
        JSONArray menu = menuJSON.getJSONArray("menu");
        for (int i=0; i<menu.length(); ++i) {
            JSONArray foodContent = menu.getJSONObject(i).getJSONArray("foodContent");
            if (foodContent.length() == 0) {
                return false;
            }
        }
        return true;
    }
}