package src.main.java.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
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

    // User state tracking for interaction; false stands for user did not enter food list yet
    private static HashMap<String, Boolean> userStates =
            new HashMap<String, Boolean>();

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
        JSONObject qJSON = new JSONObject();
        //Get QueryJSON from database
        //qJSON = getFoodJSON(userId);
        JSONArray print = qJSON.getJSONArray("menu");
        int i = 1;
        for(JSONObject food : print){
            response.appendTextMessage(i + ". " + food.getString("name"));
            i++;
        }
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
        boolean selection = userStates.get(userId);

        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "reply")
                .set("replyToken", replyToken);
        log.info(psr.toString());
        if (!selection) {
            response.appendTextMessage(
                    "Plz tell me what food you just have, " +
                            "enter in this format: food1;food2;food3 (seperate with ';')");
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
