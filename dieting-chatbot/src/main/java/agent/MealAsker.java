package agent;

import database.querier.FoodQuerier;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import utility.*;
import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import database.connection.SQLPool;
import database.keeper.MenuKeeper;
import database.querier.FuzzyFoodQuerier;
import database.querier.PartialFoodQuerier;

import java.util.*;

import static java.lang.Integer.parseInt;
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
    private PortionAsker portionAsker;

    @Autowired(required = false)
    private ChatbotController controller;

    static private HashMap<String, JSONObject> menus = new HashMap<>();
    private static Map<String, Integer> userStates = new HashMap<>();
    private static Map<String, NewFood> userNewFood = new HashMap<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("MealAsker register on event bus");
        }
    }

    /**
     * Change user state, for testing purpose.
     * @param userId String of user Id.
     * @param state New user state.
     */
    public void changeUserState(String userId, int state) {
        userStates.put(userId, state);
        log.info("Change state of user {} to {}", userId, state);
    }

    /**
     * Clear all QueryJSON.
     */
    public void clearMenuJSON() {
        log.info("Removing all MenuJSON object");
        menus.clear();
    }

    /**
     * Set MenuJSON for a user.
     * @param json MenuJSON to add.
     */
    public void setMenuJSON(JSONObject json) {
        menus.put(json.getString("userId"), json);
    }

    /**
     * get MenuJSON for a user.
     * @param userId String of user Id.
     * @return JSONObject, null if no such user.
     */
    public JSONObject getMenuJSON(String userId) {
        return menus.getOrDefault(userId, null);
    }

    /**
     * Store the new dish supplied by user.
     * @param newDish a new dish supplied by user to store in database.
     * @return idx i that used for database storage.
     */
    public int getFoodQuerier(JSONObject newDish){
        int i = 250;
        FoodQuerier fq = new FoodQuerier();
        boolean flag = false;
        while (!flag){
            i++;
            newDish.put("ndb_no", i);
            flag = fq.add(newDish);
        }
        fq.close();
        return i;
    }

    /**
     * update food revised by user.
     * @param dishIndex the index of dish in menu, started by 1.
     * @param name name of the dish.
     * @param userId String of user Id.
     */
    public void updateFood(int dishIndex, String name, String userId) {
        JSONObject queryJSON = getMenuJSON(userId);
        JSONObject dish = queryJSON.getJSONArray("menu").getJSONObject(dishIndex - 1);
        dish.put("name", name);
        queryJSON.getJSONArray("menu").put(dishIndex - 1, dish);
        setMenuJSON(queryJSON);
    }

    /**
     * updates new food to menus, as well as update this new dish in MenuKeeper.
     * @param name String of the food name.
     * @param energy energy amount in new food.
     * @param protein protein amount in new food.
     * @param lipid lipid amount in new food.
     * @param userId String of user Id.
     */
    public void updateNewFood(String name, int energy, int protein, int lipid, String userId){
        JSONObject newDish = new JSONObject();

        newDish.put("shrt_desc", name);
        newDish.put("energ_kcal", energy);
        newDish.put("protein", protein);
        newDish.put("lipid_tot", lipid);

        int i = getFoodQuerier(newDish);

        JSONObject foodContent = new JSONObject();
        foodContent.put("idx", i);
        foodContent.put("description", name);
        JSONObject newFood = new JSONObject();
        newFood.put("name", userId);
        newFood.put("foodContent", foodContent);

        JSONObject queryJSON = new JSONObject();
        queryJSON = getMenuJSON(userId);
        queryJSON.getJSONArray("menu").put(newFood);
        setMenuJSON(queryJSON);
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
        publisher.publish(new FormatterMessageJSON(userId));

        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        // if the input is image
        if(psr.getType().equals("image")) {
            response.appendTextMessage(
                "I am sorry that I can't understand this image");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, 0);
        }

        int userState = userStates.get(userId).intValue();
        if (menus.containsKey(userId)) {

            if(userState == 0){
                JSONObject menuJSON = menus.get(userId);
                JsonUtility.getFoodContent(menuJSON);
                response.appendTextMessage("Well, I got your menu.")
                        .appendTextMessage("The Menu I got is\n" +
                                JsonUtility.formatMenuJSON(menuJSON, false))
                        .appendTextMessage("And this is the food " +
                                "content of each dish I found:")
                        .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, true))


                        .appendTextMessage("Do you want to revise some food names? " +
                                "If no, you can type 'confirm to leave'")
                        .appendTextMessage("Plz show me your revise in this format: " +
                                "'dish index':'revised name', such as 1:beef");
                log.info("MenuJSON:\n{}", menuJSON.toString(4));
                changeUserState(userId, userState + 1);
            }

            //State for feature 6
            else if(userState == 1){
                String update = psr.get("textContent").toLowerCase();
                if(update.equals("confirm")){
                    response.appendTextMessage("Bravo! Your update has been saved")
                            .appendTextMessage("Is there any missing food on this list? " +
                                    "Tell me Yes or No");
                    changeUserState(userId, userState + 1);
                }
                else{
                    int menuNum = menus.get(userId).getJSONArray("menu").length();
                    String[] revised = psr.get("textContent").split(":");
                    boolean done = true;
                    int index = 0;
                    String newName = "";

                    if (!Validator.isInteger(revised[0]))
                        done = false;
                    else if(revised.length != 2)
                        done = false;
                    else {
                        index = parseInt(revised[0]);
                        newName = revised[1];
                        if (index < 1 || index > menuNum)
                            done = false;
                    }

                    if (!done) {
                        response.appendTextMessage("Plz enter in this format, " +
                                "'dish index':'portion in gram', " +
                                "both of the number shall be integer. " +
                                "Or type 'leave' if no more update desired.");
                    }
                    else{
                        updateFood(index, newName, userId);
                        JSONObject menuJSON = menus.get(userId);
                        response.appendTextMessage("The Menu I got is\n" +
                                JsonUtility.formatMenuJSON(menuJSON, false));
                    }
                }
            }

            //State for feature 7
            else if(userState == 2){
                String update = psr.get("textContent").toLowerCase();
                if(update.equals("yes")){
                    response.appendTextMessage("So what is the name of this food?");
                    userStates.put(userId, userState + 1);
                    userNewFood.put(userId, new NewFood());
                }
                else if(update.equals("no")){
                    userStates.remove(userId);
                    menus.remove(userId);
                    response.appendTextMessage("Alright, let's move on");
                    if (controller != null) {
                        publisher.publish(response);
                        controller.setUserState(userId, State.ASK_PORTION);
                        return;
                    }
                }
                else
                    response.appendTextMessage("Sorry, I'm not sure about this. " +
                            "Plz key in 'Yes' or 'No' at this moment");
            }

            //State for feature 7, continued
            else if(userState == 3){
                String dishName = psr.get("textContent");
                userNewFood.get(userId).name = dishName;
                userStates.put(userId, userState + 1);
                response.appendTextMessage("Okay, I need you provide some nutrition details" +
                        " So what is the energy contained in this dish? (in terms of kcal)" +
                        "Give me an integer please ~");
            }
            //State for feature 7, continued
            else if(userState == 4){
                String energy = psr.get("textContent");
                if(Validator.isInteger(energy))
                    response.appendTextMessage("Give me an integer please ~");
                else{
                    userNewFood.get(userId).energy = parseInt(energy);
                    userStates.put(userId, userState + 1);
                    response.appendTextMessage("Okay, so what is the protein contained in this dish? " +
                            "(in terms of gram) Give me an integer please ~");
                }
            }
            //State for feature 7, continued
            else if(userState == 5){
                String protein = psr.get("textContent");
                if(Validator.isInteger(protein))
                    response.appendTextMessage("Give me an integer please ~");
                else{
                    userNewFood.get(userId).protein = parseInt(protein);
                    userStates.put(userId, userState + 1);
                    response.appendTextMessage("Okay, so what is the lipid contained in this dish? " +
                            "(in terms of tot) Give me an integer please ~");
                }
            }
            //State for feature 7, continued
            else if(userState == 6){
                String lipid = psr.get("textContent");
                if(Validator.isInteger(lipid))
                    response.appendTextMessage("Give me an integer please ~");
                else{
                    userNewFood.get(userId).lipid = parseInt(lipid);
                    String n = userNewFood.get(userId).name;
                    int en = userNewFood.get(userId).energy;
                    int pro = userNewFood.get(userId).protein;
                    int lip = userNewFood.get(userId).lipid;
                    userNewFood.remove(userId);
                    updateNewFood(n, en, pro, lip, userId);

                    response.appendTextMessage("Alright, I have recorded your meal");
                    if (controller != null) {
                        publisher.publish(response);
                        controller.setUserState(userId, State.ASK_PORTION);
                        return;
                    }
                }
            }



            publisher.publish(response);

//            menus.remove(userId);
//            log.info("MenuJSON:\n{}", menuJSON.toString(4));
//            portionAsker.setMenuJSON(menuJSON);
//            if (controller != null) {
//                controller.setUserState(userId, State.ASK_PORTION);
//            }

        }




        else {
            response.appendTextMessage(
                "Oops, looks like your menu is empty. Session cancelled.");
            publisher.publish(response);
            if (controller != null) {
                controller.setUserState(userId, State.IDLE);
            }
        }
    }
}

/**
 * record user's new food information.
 */
class NewFood {
    String name;
    int energy;
    int protein;
    int lipid;
}