package misc;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
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
public class UserInitialInputRecord
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired(required=false)
    private EventBus eventBus;

    @Autowired(required=false)
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null)
            eventBus.on($("ParserMessageJSON"), this);
    }
    
    // User state tracking for interaction
    private static HashMap<String, UserInitialState> userStates =
        new HashMap<String, UserInitialState>();
    
    /**
     * Validate user input
     * @param type The type of information.
     *             Should be one of "age", "gender", "weight", "desiredWeight",
     *             "height", "due"
     * @param textContent String of user input
     * @return validation result
     */
    public boolean validateInput(String type, String textContent) {
        switch(type) {
            case "age":
                if(!Validator.isInteger(textContent)) return false;
                int age = Integer.parseInt(textContent);
                if(!Validator.validateAge(age)) return false;
                break;

            case "gender":
                if (!Validator.isGender(textContent)) return false;
                break;

            case "weight": case "desiredWeight":
                if(!Validator.isInteger(textContent)) return false;
                int weight = Integer.parseInt(textContent);
                if (!Validator.validateWeight(weight)) return false;
                break;

            case "height":
                if(!Validator.isInteger(textContent)) return false;
                int height = Integer.parseInt(textContent);
                if (!Validator.validateWeight(height)) return false;
                break;

            case "due":
                return Validator.isFutureDate(textContent, "yyyy-MM-dd");
                
            default:
                return false;
        }       
        return true;
    }
    
    /**
     * add userInfo to database if everything is correct
     */
    public void addDatabase(UserInitialState u) {
        JSONObject userJSON = new JSONObject();
        userJSON.put("id", u.id);
        userJSON.put("age", u.age);
        userJSON.put("gender", u.gender);
        userJSON.put("weight", u.weight);
        userJSON.put("height", u.height);

        JSONObject goal = new JSONObject();
        goal.put("weight", u.desiredWeight);
        goal.put("due", u.goalDate);
        userJSON.put("goal", goal);
        
        // add user info to database and remove
        log.info("User Info of {} is ready for database", u.id);
        // setUserInfo(u.id, userJSON);
        userStates.remove(u.id);
    }
    
    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     * @return None
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `InitialInput`
        String currentState = psr.get("state");
        if (!currentState.equals("InitialInput")) return;

        log.info("Entering user initial input handler");
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
            userStates.put(userId, new UserInitialState(userId));
        }
        UserInitialState user = userStates.get(userId);
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "reply")
                .set("replyToken", replyToken);
        if (!validateInput(user.getState(), psr.getTextContent())) {
            response.appendTextMessage(
                "Please input a valid value according to instruction");
        } else {
            switch(user.getState()) {
                case "id":
                    response.appendTextMessage(
                        "Hello ~ Would you mind tell me your age? " +
                        "Give me an integer please ~");
                    break;
                case "age":
                    user.age = Integer.parseInt(psr.getTextContent());
                    response.appendTextMessage(
                        "Tell me your gender please, type in 'male' or 'female'");
                    break;
                case "gender":
                    user.gender = psr.getTextContent();
                    response.appendTextMessage("Hey, what is your weight? " +
                        "Just simply give me an integer (in terms of kg)");
                    break;
                case "weight":
                    user.weight = Integer.parseInt(psr.getTextContent());
                    response.appendTextMessage("How about the height in cm?");
                    break;
                case "height":
                    user.height = Integer.parseInt(psr.getTextContent());
                    response.appendTextMessage(
                        "Emmm... What is your desired weight?" +
                        "(give an integer in terms of kg)");
                    break;
                case "desiredWeight":
                    user.desiredWeight = Integer.parseInt(psr.getTextContent());
                    response.appendTextMessage("Alright, now tell when you want to finish this goal? (type in yyyy-mm-dd format)");
                    break;
                case "goalDate":
                    user.goalDate = psr.getTextContent();
                    response.appendTextMessage(
                        "Great! I now understand what you need!");
                    addDatabase(user);
                    break;
                default:
            }
            user.moveState();
        }
        publisher.publish(response);
    }

    /**
     * Inner class for tracking user interaction
     */
    class UserInitialState {
        private int stateIndex;
        private String[] stateList = {"id", "age", "gender", 
            "weight", "height", "desiredWeight", "goalDate"};
        
        public String id;
        public int age;
        public String gender;
        public int weight;
        public int height;
        public int desiredWeight;
        public String goalDate;
        
        public UserInitialState(String userId) {
            this.stateIndex = 0;
            this.id = userId;
        }

        public String getState() {
            return this.stateList[stateIndex];
        }

        public void moveState() {
            if (this.stateIndex+1 < stateList.length)
                this.stateIndex++;
        }
    }
}
