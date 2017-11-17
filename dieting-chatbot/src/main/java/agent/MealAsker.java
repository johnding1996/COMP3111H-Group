package agent;

import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.JsonUtility;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import database.connection.SQLPool;
import database.keeper.MenuKeeper;
import database.querier.FuzzyFoodQuerier;
import database.querier.PartialFoodQuerier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
        
        if (menus.containsKey(userId)) {
            JSONObject menuJSON = menus.get(userId);
            JsonUtility.getFoodContent(menuJSON);
            response.appendTextMessage("Well, I got your menu.")
                    .appendTextMessage("The Menu I got is\n" +
                        JsonUtility.formatMenuJSON(menuJSON, false))
                    .appendTextMessage("And this is the food " +
                        "content of each dish I found:")
                    .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, true));
            publisher.publish(response);

            menus.remove(userId);
            log.info("MenuJSON:\n{}", menuJSON.toString(4));
            portionAsker.setMenuJSON(menuJSON);
            if (controller != null) {
                controller.setUserState(userId, State.ASK_PORTION);
            }
        } else {
            response.appendTextMessage(
                "Oops, looks like your menu is empty. Session cancelled.");
            publisher.publish(response);
            if (controller != null) {
                controller.setUserState(userId, State.IDLE);
            }
        }
    }
}