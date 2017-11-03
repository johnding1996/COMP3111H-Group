package agent;

import database.keeper.HistKeeper;
import database.keeper.MenuKeeper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
public class ConfirmFood implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("ConfirmFood register on event bus");
        }
    }

    /**
     * User states tracking for interaction
     * false stands for user did not confirm food list yet
     */
    private static Map<String, Boolean> userStates = new HashMap<>();
    private static Map<String, Integer> menuCount = new HashMap<>();

    /**
     * Change user state
     * @param userId String of user Id
     * @param state New user state
     */
    public void changeUserState(String userId, boolean state) {
        if (userStates.containsKey(userId)) {
            userStates.put(userId, state);
            log.info("Change state of user {} to {}", userId, state);
        }
    }

    /**
     * add userInfo to history if everything is correct
     * @param idxs list of indices
     */
    public void updateDatabase (List<Integer> idxs, String userId)
            throws NumberFormatException {
        List<String> foodNames = new ArrayList<>();
        MenuKeeper menuKeeper = new MenuKeeper();
        HistKeeper histKeeper = new HistKeeper();
        try {
            JSONArray menu = menuKeeper.get(userId, 1)
                .getJSONObject(0).getJSONArray("menu");
            for(int j = 0; j < menu.length(); j++){
                foodNames.add(menu.getJSONObject(j).getString("name"));
            }
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        for (Integer idx : idxs) {
            JSONObject foodJson = new JSONObject();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            foodJson.put("date", dateFormat.format(new Date()));
            foodJson.put("number_of_meal", 1);
            foodJson.put("food", foodNames.get(idx));
            DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            foodJson.put("timestamp", dateTimeFormat.format(new Date()));
            histKeeper.set(userId, foodJson);
            log.info("Food JSON:\n{}", foodJson.toString(4));
        }

        log.info(String.format("Stored the meal history of user %s in to the caches.", userId));
        menuKeeper.close();
        histKeeper.close();
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
            reply += "Please enter in a list of " +
                "indices separated by ';' (e.g. 1;3;4).";
            menuCount.put(userId, menu.length());
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        return reply;
    }

    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `RecordMeal`
        String currentState = psr.get("state");
        if (!currentState.equals("RecordMeal")) return;

        log.info("Entering user meal confirm handler");
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
            userStates.put(userId, false);
        }
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "push");
        log.info(psr.toString());

        boolean selection = userStates.get(userId);
        if (!selection) {
            log.info("CONFIRM MEAL: conversation initiated");
            response.appendTextMessage(
                "Welcome back! Please choose among what you just ate in this list:");
            response.appendTextMessage(getMenu(userId));
            userStates.put(userId, true);
        } else {
            String[] idxStrings = psr.getTextContent().split(";");
            List<Integer> idxs = new ArrayList<>();
            for (String idxString : idxStrings) {
                idxString = idxString.trim();
                if (Validator.isInteger(idxString)) {
                    int x = Integer.parseInt(idxString);
                    if (x >= 1 && x <= menuCount.get(userId).intValue())
                        idxs.add(x - 1);
                }
            }
            if (idxs.size() == 0) {
                log.info("Invalid meal option");
                response.appendTextMessage("Your input is invalid!");
            } else {
                response.set("stateTransition", "confirmMeal")
                        .appendTextMessage("Great! " +
                        "I have recorded what you have just eaten!");
                updateDatabase(idxs, userId);
                userStates.remove(userId);
                menuCount.remove(userId);
                log.info("CONFIRM MEAL: remove user {}", userId);
            }
        }
        publisher.publish(response);
    }
}
