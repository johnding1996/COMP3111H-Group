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
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;

import utility.Validator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                if(age < 5 || age > 95) return false;
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
        
        //add this to database and remove
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
                        "Please input some text at this moment~");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }
        
        //deal with different progress
        //user is already registered if searchResult returns true
        if(userStates.containsKey(userId)) {
            UserInitialState user = userStates.get(userId);
            if(!validateInput(user.getState(), psr.getTextContent())) {
                FormatterMessageJSON response = new FormatterMessageJSON();
                response.appendTextMessage("plz input a valid value according to instruction");
            }
            else {
                switch(user.getState()) {
                    case "age":
                        user.age = Integer.parseInt(psr.getTextContent());
                        FormatterMessageJSON askGender = new FormatterMessageJSON();
                        askGender.appendTextMessage("Tell me your gender please, type in 'male' or ' female' ");
                        break;
                    case "gender":
                        user.gender = psr.getTextContent();
                        FormatterMessageJSON askWeight = new FormatterMessageJSON();
                        askWeight.appendTextMessage("Hey, what your weight, jsut simply give me an integer (in terms of kg)");
                        break;
                    case "weight":
                        user.weight = Integer.parseInt(psr.getTextContent());
                        FormatterMessageJSON askHeight = new FormatterMessageJSON();
                        askHeight.appendTextMessage("How about the height, give me an integer (in terms of CM)");
                        break;
                    case "height":
                        user.height = Integer.parseInt(psr.getTextContent());
                        FormatterMessageJSON desiredWeight = new FormatterMessageJSON();
                        desiredWeight.appendTextMessage("Emmm...What is your desired weight? (give an integer in terms of kg)");
                        break;
                    case "desiredWeight":
                        user.desiredWeight = Integer.parseInt(psr.getTextContent());
                        FormatterMessageJSON dueDate = new FormatterMessageJSON();
                        dueDate.appendTextMessage("Alright, now tell when you want to finish this goal? (type in yyyy-mm-dd format)");
                        break;
                    //Note that after the below case, the user info should be complete and need to be added to database
                    case "goalDate":
                        user.goalDate = psr.getTextContent();
                        FormatterMessageJSON finish = new FormatterMessageJSON();
                        finish.appendTextMessage("Great! I now understand you need!");
                        //if goalDate can be successful input, user info is complete
                        addDatabase(user); return;
                        break;
                    default:
                        FormatterMessageJSON done = new FormatterMessageJSON();
                        done.appendTextMessage("OMG, something bad happens, you may need to re-add me");
                        assert false;
                }
                
                user.moveState();
            }
        }
        
        //create a new user
        else {
            UserInitialState user = new UserInitialState(userId);
            FormatterMessageJSON askAge = new FormatterMessageJSON();
            askAge.appendTextMessage("Hello ~ would you mind tell me your age? Give me an integer please ~");
            userStates.put(userId, user);
            user.moveState();
        }
    }
}
