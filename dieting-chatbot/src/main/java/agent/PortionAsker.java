package agent;

import database.keeper.MenuKeeper;

import java.util.*;
import java.lang.Integer;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;
import org.json.JSONObject;

import controller.ParserMessageJSON;
import controller.Publisher;
import controller.FormatterMessageJSON;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;

import utility.Validator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PortionAsker implements Consumer<Event<ParserMessageJSON>> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("PortionAsker register on event bus");
        }
    }

    /**
     * User states tracking for interaction
     * false stands for user did not confirm food list yet
     */
    private static Map<String, Integer> userStates = new HashMap<>();
    private static Map<String, Integer> menuCount = new HashMap<>();

    /**
     * Change user state
     * @param userId String of user Id
     * @param state New user state
     */
    public void changeUserState(String userId, int state) {
        if (userStates.containsKey(userId)) {
            userStates.put(userId, state);
            log.info("Change state of user {} to {}", userId, state);
        }
    }

    /**
     * Get list of menu previously input by user
     * @param userId String of user Id
     * @return Menu list in String
     */
    public String getMenu(String userId) {
        MenuKeeper keeper = new MenuKeeper();
        String reply = "";
        try {
            JSONArray menu = keeper.get(userId, 1)
                    .getJSONObject(0).getJSONArray("menu");
            for(int j = 0; j < menu.length(); j++){
                JSONObject food = menu.getJSONObject(j);
                reply += String.format("%d - %s\n", j + 1,
                        food.getString("name"));
            }
            reply += "Do you want to input portion size?";
            menuCount.put(userId, menu.length());
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        return reply;
    }

    /**
     * update portion size to MenuKeeper
     * @param dishIndex the index of dish in menu, started by 1
     * @param portion portion of the dish, default portion unit as gram
     */
    public void updateDatabase (int dishIndex, double portion, String userId) {
        MenuKeeper menuKeeper = new MenuKeeper();
        JSONObject dish;
        JSONObject queryJSON;
        try {
            queryJSON = menuKeeper.get(userId, 1).getJSONObject(0);
            dish = queryJSON.getJSONArray("menu").getJSONObject(dishIndex - 1);
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }

        dish.get("portionSize") = portion;
        dish.get("portionUnit") = "gram";
        queryJSON.getJSONArray("menu").getJSONObject(dishIndex - 1) = dish;
        boolean success = menuKeeper.set(userId, queryJSON);
        if(success)
            log.info(String.format("Updated portion size in menu of user %s in to the caches.", userId));
        else
            log.warn(String.format("Set error occurs, for user %s.", userId));
        menuKeeper.close();
    }

    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `AskPortion`
        String currentState = psr.get("state");
        if (!currentState.equals("AskPortion")) {
            String userId = psr.get("userId");
            if (userStates.containsKey(userId))
                userStates.remove(userId);
            if (menuCount.containsKey(userId))
                menuCount.remove(userId);
            return;
        }

        log.info("Entering user meal portion-asker handler");
        String userId = psr.get("userId");

        // if the input is not text
        if(!psr.getMessageType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON();
            response.set("userId", userId)
                    .set("type", "push")
                    .appendTextMessage(
                            "Please input some text at this moment.");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, 0);
        }
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "push");
        log.info(psr.toString());

        int state = userStates.get(userId).intValue();
        if (state == 0) {
            log.info("ASK PORTION: conversation initiated");
            response.appendTextMessage("Okay, this is what you just ate:");
            response.appendTextMessage(getMenu(userId));
            response.appendTextMessage("Would you like to update me with how much you just enjoy?" +
                    " key in 'Yes' or 'No'");
            userStates.put(userId, state + 1);
        }
        else if (state == 1) {
            String update = psr.getTextContent().toLowerCase();
            if(update.equals("yes")){
                response.appendTextMessage("Okay, so give me about your update in this format: " +
                        "1:100g, 'dish index':'portion in gram'");
                response.appendTextMessage("Typically, an apple is around 100g");
                response.appendTextMessage("Note that if you finish all updates you desired, " +
                        "you just need to type 'leave' to end the session");
                userStates.put(userId, state + 1);
            }
            else if(update.equals("no")){
                userStates.remove(userId);
                menuCount.remove(userId);
            }
            else
                response.appendTextMessage("Sorry, I'm not sure about this. " +
                        "Plz key in 'Yes' or 'No' at this moment");
        }
        else if (state == 2){
            String info = psr.getTextContent().toLowerCase();
            if (info.equals("leave")){
                userStates.remove(userId);
                menuCount.remove(userId);
                response.appendTextMessage("Alright, we are going to process your update");
            }
            else{
                //TODO
            }
        }
        publisher.publish(response);
    }
}
