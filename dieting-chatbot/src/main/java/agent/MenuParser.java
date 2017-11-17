package agent;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import database.keeper.MenuKeeper;
import org.json.JSONArray;
import org.json.JSONObject;

import controller.Publisher;
import controller.State;
import controller.ChatbotController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * MenuParser: handle menu in text, url and image.
 * @author cliubf, szhouan
 * @version v2.0.0
 */
@Slf4j
@Component
public class MenuParser
    implements Consumer<Event<ParserMessageJSON>> {
    
    // User state tracking
    private static HashMap<String, Integer> states = new HashMap<String, Integer>();
    
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private MealAsker mealAsker;

    @Autowired(required = false)
    private ChatbotController controller;

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("MenuParser register on event bus");
        }
        if (mealAsker == null) {
            log.info("Cannot find mealAsker bean");
        }
    }

    /**
     * Validate the parsed menu, and interact with user.
     * @param userId String of user Id.
     * @param response FormatterMessageJSON for replying user.
     * @param menuArray Parsed JSONArray as menu.
     */
    public void checkAndReply(String userId,
        FormatterMessageJSON response, JSONArray menuArray) {
        if(menuArray == null) {
            response.appendTextMessage("Looks like the menu is empty, " +
                "please try again");
            publisher.publish(response);
        }
        else {
            JSONObject menuJSON = new JSONObject();
            menuJSON.put("userId", userId)
                    .put("menu", menuArray);
            // set queryJSON for meal asker
            if (mealAsker != null) {
                mealAsker.setMenuJSON(menuJSON);
            }

            // keep menu in redis
            MenuKeeper keeper = new MenuKeeper();
            keeper.set(userId, menuJSON);
            keeper.close();

            states.remove(userId);
            publisher.publish(response);
            if (controller != null) {
                controller.setUserState(userId, State.ASK_MEAL);
            }
        }
    }
    
    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `ParseMenu`
        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.PARSE_MENU) {
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("Clear user {}", userId);
            }
            return;
        }

        log.info("Entering MenuParser");
        publisher.publish(new FormatterMessageJSON(userId));

        // do not handle image for now
        if(psr.getType().equals("image")) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage(
                "Sorry but I don't understand this image, give me some text please ~");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!states.containsKey(userId)) {
            log.info("register new user {}", userId);
            states.put(userId, 0);
        }
        Integer state = states.get(userId);
        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        if (state == 0) {
            if (!psr.getType().equals("transition")) return;
            response.appendTextMessage(
                "Long time no see! What is your menu today? " +
                "You could use text or URL.");
            states.put(userId, 1);
            publisher.publish(response);
        } else if (state == 1) {
            JSONArray menuArray;
            String text = psr.get("textContent");
            if (ResourceUtils.isUrl(text)) {
                menuArray = UrlMenuParser.buildMenu(text);
            } else {
                menuArray = TextMenuParser.buildMenu(text);
            }
            checkAndReply(userId, response, menuArray);
        }
    }

    /**
     * Get the state of a given user.
     * @param userId String of user Id.
     * @return Current state in integer, -1 if no such user.
     */
    public int getUserState(String userId) {
        if (!states.containsKey(userId)) return -1;
        else return states.get(userId).intValue();
    }

    /**
     * Clear all user states.
     */
    public void clearUserStates() {
        states.clear();
    }
}