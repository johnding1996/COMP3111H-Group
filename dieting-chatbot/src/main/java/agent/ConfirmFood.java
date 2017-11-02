package agent;

import database.keeper.MenuKeeper;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

    // User state tracking for interaction; false stands for user did not confirm food list yet
    private static HashMap<String, Boolean> userStates =
            new HashMap<>();

    void changeUserState(String userId, boolean state) {
        userStates.put(userId, state);
    }

    private MenuKeeper menuKeeper = new MenuKeeper();

    /**
     * add userInfo to history if everything is correct
     */
    public void addDatabase(String foodInfo, String userId) {
        String[] food = foodInfo.split(";");

        // add user info to database and remove
        log.info("User Info of {} is ready for database", userId);
        // setUserInfo(u.id, userJSON);
        userStates.remove(userId);
    }

    public void printList(String userId, FormatterMessageJSON response){
        JSONObject jsonObject = new JSONObject();
        //Get QueryJSON from database
        try{
            jsonObject = menuKeeper.get(userId, 1).getJSONObject(0);
        } catch (JSONException e){
            log.warn("MenuKeeper returns a empty JSONArray", e);
        }

        JSONArray print = jsonObject.getJSONArray("menu");
        for(int j = 0; j < print.length(); j++){
            JSONObject food = print.getJSONObject(j);
            response.appendTextMessage((j+1)+ ". " + food.getString("name"));
        }
        response.appendTextMessage("Please enter in the index in this format: e.g. 1;3;4, seperated by ';'");
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
        String replyToken = psr.get("replyToken");

        // if the input is not text
        if(!psr.getMessageType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON();
            response.set("userId", userId)
                    .set("type", "reply")
                    .set("replyToken", replyToken)
                    .appendTextMessage(
                            "Please input some text at this moment ~");
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
                .set("type", "reply")
                .set("replyToken", replyToken);
        log.info(psr.toString());

        boolean selection = userStates.get(userId);
        if (!selection) {
            response.appendTextMessage("Welcome back! Please choose among what you just ate in this list:");
            printList(userId, response);
            userStates.put(userId, true);
        } else {
            String foodInfo = psr.getTextContent();
            response.set("stateTransition", "confirmMeal")
                    .appendTextMessage("Great! I have recorded what you have just eaten!");
            addDatabase(foodInfo, userId);
        }
        publisher.publish(response);
    }
}
