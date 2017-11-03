package agent;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;

import database.keeper.MenuKeeper;
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
import org.springframework.util.ResourceUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MenuParser
    implements Consumer<Event<ParserMessageJSON>> {
    
    // User state tracking
    private static HashMap<String, Integer> userStates = new HashMap<String, Integer>();
    
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required=false)
    private MealAsker mealAsker;

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
     * Validate the parsed menu, and interact with user
     * @param userId String of user Id
     * @param response FormatterMessageJSON for replying user
     * @param menuArray Parsed JSONArray as menu
     */
    public void checkAndReply(String userId,
        FormatterMessageJSON response, JSONArray menuArray) {
        if(menuArray == null) {
            response.appendTextMessage("Looks like the menu is empty, " +
                "please try again");
        }
        else {
            userStates.remove(userId);
            JSONObject queryJSON = new JSONObject();
            queryJSON.put("userId", userId)
                     .put("menu", menuArray);
            // set queryJSON for meal asker
            if (mealAsker != null) {
                mealAsker.setQueryJSON(queryJSON);
            } else {
                for (int i=0; i<100; ++i)
                log.info("Error: mealAsker is null");
            }

            // keep menu in redis
            MenuKeeper keeper = new MenuKeeper();
            keeper.set(userId, queryJSON);
            keeper.close();

            // no need to reply, give control to MealAsker
            response.set("stateTransition", "menuMessage")
                    .set("type", "transition");
        }
    }
    
    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `ParseMenu`
        String currentState = psr.get("state");
        if (!currentState.equals("ParseMenu")) return;

        log.info("Entering user menu input handler");
        String userId = psr.get("userId");
        String replyToken = psr.get("replyToken");

        // do not handle image for now
        if(!psr.getMessageType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON();
            response.set("userId", userId)
                    .set("type", "reply")
                    .set("replyToken", replyToken)
                    .appendTextMessage(
                        "Sorry but I don't understand this image, give me some text please ~");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }
        
        String text = psr.getTextContent();

        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, 0);
        }

        Integer userState = userStates.get(userId);
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "reply")
                .set("replyToken", replyToken);

        if (userState == 0) {
            response.appendTextMessage(
                "Long time no see! What is your menu today? " +
                "You could use text or URL.");
            userStates.put(userId, 1);
        } else if (userState == 1) {
            JSONArray menuArray;
            if (ResourceUtils.isUrl(text)) {
                menuArray = UrlMenuParser.buildMenu(text);
            } else {
                menuArray = TextMenuParser.buildMenu(text);
            }
            checkAndReply(userId, response, menuArray);
        }
        publisher.publish(response);
    }

    /**
     * Get the state of a given user
     * @param userId String of user Id
     * @return A String of the current state, null of no such user
     */
    public int getUserState(String userId) {
        if (!userStates.containsKey(userId)) return -1;
        else return userStates.get(userId).intValue();
    }

    /**
     * Clear all user states
     */
    public void clearUserStates() {
        userStates.clear();
    }
}
