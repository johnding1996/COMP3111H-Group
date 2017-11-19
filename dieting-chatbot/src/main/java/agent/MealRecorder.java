package agent;

import database.keeper.HistKeeper;
import database.keeper.MenuKeeper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.Integer;

import database.querier.UserQuerier;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.Validator;

import lombok.extern.slf4j.Slf4j;

/**
 * MealRecorder: record the dish user ate.
 * @author cliubf, mcding, szhouan
 * @version v1.1.0
 */
@Slf4j
@Component
public class MealRecorder implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("ConfirmFood register on event bus");
        }
    }

    /**
     * User states tracking for interaction.
     * 0 stands for user did not confirm food list yet.
     */
    private static Map<String, Integer> states = new HashMap<>();
    private static Map<String, Integer> menuCount = new HashMap<>();
    private static List<Integer> ids = new ArrayList<>();


    /**
     * add userInfo to history if everything is correct.
     * @param userId String of userId
     * @param portionSize portion size
     * @param weight weight
     */
    public void updateDatabase (String userId, int portionSize, int weight) {
        MenuKeeper menuKeeper = new MenuKeeper();
        HistKeeper histKeeper = new HistKeeper();
        UserQuerier userQuerier = new UserQuerier();
        JSONObject histJson = new JSONObject();
        try{
            // Add hist to HistKeeper
            DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            histJson.put("timestamp", dateTimeFormat.format(new Date()));
            histJson.put("weight", weight);
            histJson.put("portionSize", portionSize);
            log.error("all" + menuKeeper.get(userId, 1).toString());
            JSONArray menu = menuKeeper.get(userId, 1).getJSONObject(0).getJSONArray("menu");
            log.error("menu" + menu.toString());
            JSONArray selectedMenu = new JSONArray();
            for (Integer id : ids) selectedMenu.put(menu.getJSONObject(id));
            log.error("selected menu" + selectedMenu.toString());
            histJson.put("menu", selectedMenu);
            histKeeper.set(userId, histJson);
            log.info(String.format("Stored the user history of user %s in to the caches.", userId));
            // Update weight in UserInfo table
            JSONObject infoJson = userQuerier.get(userId);
            infoJson.put("weight", weight);
            userQuerier.update(infoJson);

        } catch (JSONException e) {
            log.error("Error encountered when parsing the MealJSON.", e);
        }
        menuKeeper.close();
        histKeeper.close();
        userQuerier.close();
    }

    /**
     * Get list of menu previously input by user.
     * @param userId String of user Id.
     * @return Menu list in String.
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
            reply += "Please choose one of them and enter the number";
            menuCount.put(userId, menu.length());
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

        // only handle message if state is `RecordMeal`
        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.RECORD_MEAL) {
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("Remove state of user {}", userId);
            }
            if (menuCount.containsKey(userId)) {
                menuCount.remove(userId);
                log.info("Remove menu count of user {}", userId);
            }
            return;
        }

        log.info("Entering user meal confirm handler");
        publisher.publish(new FormatterMessageJSON(userId));

        // if the input is image
        if(psr.getType().equals("image")) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage(
                        "Please input some text at this moment.");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!states.containsKey(userId)) {
            states.put(userId, 0);
            log.info("register new user {}", userId);
        }

        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        int portionSize = 0;
        int weight = 0;
        int state = states.get(userId);
        if (state == 0) {
            if (!psr.getType().equals("transition")) return;
            log.info("CONFIRM MEAL: conversation initiated");
            response.appendTextMessage(
                "Welcome back! Please choose among what you just ate in this list:");
            response.appendTextMessage(getMenu(userId));
            states.put(userId, state+1);
        } else if (state == 1) {
            ids = new ArrayList<>();
            String[] idxStrings = psr.get("textContent").split(";");
            for (String idxString : idxStrings) {
                idxString = idxString.trim();
                if (Validator.isInteger(idxString)) {
                    int x = Integer.parseInt(idxString);
                    if (x >= 1 && x <= menuCount.get(userId))
                        ids.add(x-1);
                }
            }
            if (ids.size() == 0) {
                log.info("Invalid meal option");
                response.appendTextMessage("Your input is invalid!");
            } else {
                response.appendTextMessage("Great! " +
                        "I have recorded what you have just eaten!")
                        .appendTextMessage("And what is the portion size of it? (in gram)");
                states.put(userId, state+1);
                menuCount.remove(userId);
                log.info("CONFIRM MEAL: remove user {}", userId);
            }
        } else if (state == 2) {
            String textContent = psr.get("textContent");
            if (!Validator.isInteger(textContent)) {
                response.appendTextMessage("Your input is not an integer");
            } else {
                portionSize = Integer.parseInt(textContent);
                if (portionSize <= 0) {
                    response.appendTextMessage("Your input is invalid");
                } else {
                    response.appendTextMessage(
                        String.format("So you have eaten %d gram of the dish", portionSize))
                            .appendTextMessage("One more question: what is your weight now?");
                    states.put(userId, state+1);
                }
            }
        } else if (state == 3) {
            String textContent = psr.get("textContent");
            boolean done = true;
            if (!Validator.isInteger(textContent)) done = false;
            else if (!Validator.validateWeight(Integer.parseInt(textContent))) done = false;
            if (!done) {
                response.appendTextMessage("This is not a valid weight, please input again");
            } else {
                weight = Integer.parseInt(textContent);
                response.appendTextMessage(String.format("So your weight now is %d kg", weight))
                        .appendTextMessage("See you ^_^");

                log.error("ids" + ids.toString());

                updateDatabase(userId, portionSize, weight);
                states.remove(userId);
                if (controller != null) {
                    controller.setUserState(userId, State.IDLE);
                }
            }
        }
        publisher.publish(response);
    }

    /**
     * Set internal state of a user.
     * @param userId String of userId
     * @param newState New state to set
     */
    protected void setUserState(String userId, int newState) {
        states.put(userId, newState);
    }
}