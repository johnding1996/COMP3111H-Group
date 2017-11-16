package misc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
import org.json.JSONObject;

import controller.Publisher;
import controller.State;
import controller.ChatbotController;
import database.querier.UserQuerier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import utility.Validator;

/**
 * UserInitialInputRecorder: add user initial input.
 * @author cliubf, szhouan
 * @version v2.0.0
 */
@Slf4j
@Component
public class InitialInputRecorder
    implements Consumer<Event<ParserMessageJSON>> {

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
            log.info("InitialInputRecorder register on event bus");
        }
    }
    
    // User state tracking for interaction
    private static HashMap<String, UserInitialState> states =
        new HashMap<String, UserInitialState>();
    
    /**
     * Validate user input.
     * @param type The type of information.
     *             Should be one of "age", "gender", "weight", "desiredWeight",
     *             "height", "due"
     * @param textContent String of user input
     * @return validation result
     */
    static public boolean validateInput(String type, String textContent) {
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

            case "goalDate":
                return Validator.isFutureDate(textContent, "yyyy-MM-dd");
            
            case "id":
                return true;
                
            default:
                return false;
        }       
        return true;
    }
    
    /**
     * Add userInfo to database if everything is correct.
     * @param u state variable for the user.
     */
    private void addDatabase(UserInitialState u) {
        JSONObject userJSON = new JSONObject();
        userJSON.put("id", u.id);
        userJSON.put("age", u.age);
        userJSON.put("gender", u.gender);
        userJSON.put("weight", u.weight);
        userJSON.put("height", u.height);
        userJSON.put("goal_weight", u.desiredWeight);
        userJSON.put("due_date", u.goalDate);

        log.info("User Info of {} is ready for database", u.id);
        log.info(userJSON.toString(4));
        UserQuerier querier = new UserQuerier();
        querier.add(userJSON);
        querier.close();
    }
    
    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // Is it my duty?
        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.INITIAL_INPUT) {
            // not my duty, clean up if needed
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("Clear user {}", userId);

            }
            return;
        }

        log.info("INITIAL_INPUT:\n{}", psr.toString());

        // Acknowledge that the psr is handled
        log.info("Entering user initial input handler");
        publisher.publish(new FormatterMessageJSON(userId));


        // if the input is image
        if(psr.getType().equals("image")) {

            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage(
                        "Please input some text at this moment ~");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!states.containsKey(userId)) {
            log.info("register new user {}", userId);
            states.put(userId, new UserInitialState(userId));
        }
        UserInitialState user = states.get(userId);
        FormatterMessageJSON response = new FormatterMessageJSON(userId);
        if (!validateInput(user.getState(), psr.get("textContent"))) {
            response.appendTextMessage(
                "Please input a valid value according to instruction");
        } else {
            switch(user.getState()) {
                case "id":
                    response.appendTextMessage(
                        "Would you please tell me your age? " +
                        "Give me an integer please ~");
                    break;
                case "age":
                    user.age = Integer.parseInt(psr.get("textContent"));
                    response.appendTextMessage(
                        "Tell me your gender please, type in 'male' or 'female'");
                    break;
                case "gender":
                    List<String> words = TextProcessor.sentenceToWords(
                        psr.get("textContent"));
                    boolean isMale = true;
                    for (String word : words) {
                        if (word.equals("female") || word.equals("woman")) {
                            isMale = false;
                            break;
                        }
                    }
                    user.gender = isMale?"male":"female";
                    response.appendTextMessage("Hey, what is your weight? " +
                        "Just simply give me an integer (in terms of kg)");
                    break;
                case "weight":
                    user.weight = Integer.parseInt(psr.get("textContent"));
                    response.appendTextMessage("How about the height in cm?");
                    break;
                case "height":
                    user.height = Integer.parseInt(psr.get("textContent"));
                    response.appendTextMessage(
                        "Emmm... What is your desired weight?" +
                        "(give an integer in terms of kg)");
                    break;
                case "desiredWeight":
                    user.desiredWeight = Integer.parseInt(psr.get("textContent"));
                    response.appendTextMessage(
                        "Alright, now tell when you want to finish this goal? " +
                        "(type in yyyy-mm-dd format)");
                    break;
                case "goalDate":
                    user.goalDate = psr.get("textContent");
                    response.appendTextMessage(
                        "Great! I now understand what you need!");
                    addDatabase(user);

                    // remove user, and notify state transition
                    states.remove(userId);
                    log.info("Internal state for user {} removed", userId);

                    if (controller != null)
                        controller.setUserState(userId, State.IDLE);
                    break;
                default:
            }
            user.moveState();
        }
        publisher.publish(response);
    }

    /**
     * Set the state of a given user.
     * @param userId String of user Id.
     * @param stateIndex index of the new state.
     */
    public void setUserState(String userId, int stateIndex) {
        if (!states.containsKey(userId)) {
            log.info("Set state for nonexisting user {}", userId);
            return;
        }
        UserInitialState u = states.get(userId);
        u.setState(stateIndex);
        log.info("Overriding state of user {} to {}", userId,
            u.getState());
    }

    /**
     * Get the state of a given user.
     * @param userId String of user Id.
     * @return A String of the current state, null if no such user.
     */
    public String getUserState(String userId) {
        if (!states.containsKey(userId)) return null;
        else return states.get(userId).getState();
    }

    /**
     * Clear all user states.
     */
    public void clearUserStates() {
        states.clear();
    }

    /**
     * Inner class for tracking user interaction.
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

        public void setState(int stateIndex) {
            if (stateIndex < 0) stateIndex = 0;
            if (stateIndex >= stateList.length)
                stateIndex = stateList.length-1;
            this.stateIndex = stateIndex;
        }

        public void moveState() {
            if (this.stateIndex+1 < stateList.length)
                this.stateIndex++;
        }
    }
}