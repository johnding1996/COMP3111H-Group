package agent;

import javax.annotation.PostConstruct;

import database.keeper.MenuKeeper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;

import java.util.HashMap;
import java.util.Map;

import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.Validator;

/**
 * PortionAsker: ask portion size of each dish.
 * @author cliubf, szhouan
 * @version v1.3.0
 */
@Slf4j
@Component
public class PortionAsker
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    public Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    @Autowired
    private FoodRecommender recommender;

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("PortionAsker register on event bus");
        }
    }

    /**
     * User states tracking for interaction.
     * false stands for user did not confirm food list yet.
     */
    private static Map<String, Integer> userStates = new HashMap<>();
    private static Map<String, Integer> menuCount = new HashMap<>();

//    /**
//     * User menus internal memory for food recommendation.
//     */
//    private HashMap<String, JSONObject> menus = new HashMap<>();


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
     * Change user number of meals in menuCount.
     * @param userId String of user Id.
     * @param count user's number of meals in menu.
     */
    public void changeMenusCount(String userId, int count){
        menuCount.put(userId, count);
        log.info("Change menuCount of user {} to {}", userId, count);
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
            reply += "Do you want to input portion size?";
            menuCount.put(userId, menu.length());
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
     * @return a string confirming the set is success with desired portion.
     */
    public String updateDatabase(int dishIndex, double portion, String userId) {
        String reply = "";
        try {
            JSONObject queryJSON = this.getMenuKeeperJSON(userId);
            JSONObject dish = queryJSON.getJSONArray("menu").getJSONObject(dishIndex - 1);
            dish.put("portionSize", portion);
            queryJSON.getJSONArray("menu").put(dishIndex - 1, dish);
            boolean success = this.setMenuKeeperJSON(userId, queryJSON);
            if(success){
                String name = this.getMenuKeeperJSON(userId)
                        .getJSONArray("menu")
                        .getJSONObject(dishIndex - 1)
                        .getString("name");
                int port = this.getMenuKeeperJSON(userId)
                        .getJSONArray("menu")
                        .getJSONObject(dishIndex - 1)
                        .getInt("portionSize");
                reply = "Roger, " + port + " gram " + name;
                log.info(String.format("Updated portion size in menu of user %s in to the caches.", userId));
            }
            else
                log.warn(String.format("Set error occurs, for user %s.", userId));
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        return reply;
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `AskPortion`
        String userId = psr.getUserId();
        State state = psr.getState();
        //This need to be changed, state should be ask portion
        if (state != State.ASK_PORTION) {
            if (userStates.containsKey(userId)) {
                userStates.remove(userId);
                menuCount.remove(userId);
//                menus.remove(userId);
                log.info("Clear user {}", userId);
            }
            return;
        }

        // Acknowledge that the psr is handled
        log.info("Entering PortionAsker");
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
        if (userState == 0) {
            log.info("ASK PORTION: conversation initiated");
            response.appendTextMessage("Okay, this is what you just ate:");
            response.appendTextMessage(getMenu(userId));
            response.appendTextMessage("Would you like to update me with how much you just enjoy?" +
                    " key in 'Yes' or 'No'");
            userStates.put(userId, userState + 1);
        }
        else if (userState == 1) {
            String update = psr.get("textContent").toLowerCase();
            if(update.equals("yes")){
                response.appendTextMessage("Okay, so give me about your update in this format: " +
                        "'dish index':'portion in gram', such as 1:100");
                response.appendTextMessage("Typically, an apple is around 100g");
                response.appendTextMessage("Note that if you finish all updates you desired, " +
                        "you just need to type 'leave' to end the session");
                userStates.put(userId, userState + 1);
            }
            else if(update.equals("no")){
                userStates.remove(userId);
                menuCount.remove(userId);
                response.appendTextMessage("Alright, let's move on");
                if (controller != null) {
                    controller.setUserState(userId, State.RECOMMEND);
                }
            }
            else
                response.appendTextMessage("Sorry, I'm not sure about this. " +
                        "Plz key in 'Yes' or 'No' at this moment");
        }
        else if (userState == 2){
            String info = psr.get("textContent").toLowerCase();
            if (info.equals("leave")){
                userStates.remove(userId);
                menuCount.remove(userId);
                response.appendTextMessage("Alright, we are going to process your update");
                //recommender.setMenuJSON(menus.remove(userId));
                if (controller != null) {
                    publisher.publish(response);
                    controller.setUserState(userId, State.RECOMMEND);
                    return;
                }
            }
            else{
                int menuNum = menuCount.get(userId).intValue();
                String[] portion = psr.get("textContent").split(":");
                boolean done = true;
                if (!Validator.isInteger(portion[0]))
                    done = false;
                else if (!Validator.isInteger(portion[1]))
                    done = false;
                int index = Integer.parseInt(portion[0]);
                int port = Integer.parseInt(portion[1]);
                if (index < 1 || index > menuNum)
                    done = false;
                else if (port < 1 || port > 7000)
                    done = false;

                if (!done) {
                    response.appendTextMessage("Plz enter in this format, " +
                            "'dish index':'portion in gram', " +
                            "both of the number shall be integer. " +
                            "Or type 'leave' if no more update desired.");
                }
                else{
                    response.appendTextMessage(updateDatabase(index, port, userId));
                }
            }
        }
        publisher.publish(response);
    }

//    /**
//     * Set MenuJSON for a user.
//     * @param json menuJSON to add.
//     */
//    public void setMenuJSON(JSONObject json) {
//        menus.put(json.getString("userId"), json);
//    }
}
