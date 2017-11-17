package agent;

import org.json.JSONException;
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

import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

/**
 * MealAsker: interact with user to get the appropriate menu.
 * @author szhouan, cliubf
 * @version v2.2.0
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

    private static HashMap<String, JSONObject> menus = new HashMap<>();
    private static Map<String, Integer> userStates = new HashMap<>();

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
     * Change user state, for testing purpose.
     * @param userId String of user Id.
     * @param state New user state.
     */
    public void changeUserState(String userId, int state) {
        userStates.put(userId, state);
        log.info("Change state of user {} to {}", userId, state);
    }

    /**
     * get most recent QueryJSON from menuKeeper (the first one).
     * @param userId String of user Id.
     * @return a json object.
     */
    public JSONObject getMenuKeeperJSON(String userId){
        MenuKeeper menuKeeper = new MenuKeeper();
        JSONObject menu = menuKeeper.get(userId, 1).getJSONObject(0);
        menuKeeper.close();
        return menu;
    }

    /**
     * set the user menuKeeper with new JSON.
     * @param userId String of user Id.
     * @param updatedJSON JSON of updated queryJSON.
     * @return a boolean value indicating whether the set is success.
     */
    public boolean setMenuKeeperJSON(String userId, JSONObject updatedJSON){
        MenuKeeper menuKeeper = new MenuKeeper();
        boolean success = menuKeeper.set(userId, updatedJSON);
        menuKeeper.close();
        return success;
    }

    /**
     * Get list of menu previously input by user.
     * @param userId String of user Id.
     * @return Menu list in String.
     */
    public String getMenu(String userId) {
        String reply = "";
        try {
            JSONArray menu = this.getMenuKeeperJSON(userId).getJSONArray("menu");
            for(int j = 0; j < menu.length(); j++){
                JSONObject food = menu.getJSONObject(j);
                reply += String.format((j + 1) + " - " + food.getString("name") + "\n");
            }
            reply += "Do you want to revise some dish name any more?" +
                    " Note that you can type 'comfirm' to finish at anytime";
        } catch (JSONException e) {
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        return reply;
    }

    /**
     * update portion size to MenuKeeper.
     * @param dishIndex the index of dish in menu, started by 1.
     * @param portion portion of the dish, default portion unit as gram.
     * @param userId String of user Id.
     */
    public void updateDatabase(int dishIndex, String name, String userId) {
        String reply = "";
        try {
            JSONObject queryJSON = this.getMenuKeeperJSON(userId);
            JSONObject dish = queryJSON.getJSONArray("menu").getJSONObject(dishIndex - 1);
            dish.put("name", name);
            queryJSON.getJSONArray("menu").put(dishIndex - 1, dish);
            boolean success = this.setMenuKeeperJSON(userId, queryJSON);
            if(success)
                log.info(String.format("Updated dish name in menu of user %s in to the caches.", userId));
            else
                log.warn(String.format("Set error occurs, for user %s.", userId));
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
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
                userStates.remove(userId);
                log.info("Remove menu of user {}", userId);
            }
            return;
        }

        log.info("Entering MealAsker");
        publisher.publish(new FormatterMessageJSON(userId));
        //Get the most up-to-date MenuJSON everytime
        menus.put(userId, getMenuJSON(userId));
        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, 0);
        }

        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        // if the input is image
        if(psr.getType().equals("image")) {
            response.appendTextMessage(
                "I am sorry that I can't understand this image");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        int userState = userStates.get(userId).intValue();
        if (menus.get(userId) != null) {
            //State for feature 5
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
                    response.appendTextMessage("Bravo! Your update has been saved");
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
                        index = Integer.parseInt(revised[0]);
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
                        updateDatabase(index, newName, userId);
                        response.appendTextMessage(getMenu(userId));
                    }
                }
            }
            //State for feature 7
            else if(userState == 2){

            }

        }
        else {
            response.appendTextMessage(
                "Oops, looks like there is something wrong with your menu. Session cancelled.");
            publisher.publish(response);
            if (controller != null) {
                controller.setUserState(userId, State.IDLE);
                return;
            }
        }
        publisher.publish(response);
    }
}