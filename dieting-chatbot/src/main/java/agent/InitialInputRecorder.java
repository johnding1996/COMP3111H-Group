package agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.json.JSONObject;

import controller.State;
import database.querier.UserQuerier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import utility.Validator;

/**
 * UserInitialInputRecorder: handle user initial input.
 * 
 * State mapping:
 *      0 - ask age
 *      1 - ask gender
 *      2 - ask weight
 *      3 - ask height
 *      4 - ask desiredWeight
 *      5 - ask goalDate
 *      6 - store user info
 * 
 * @author cliubf, szhouan
 * @version v2.1.0
 */
@Slf4j
@Component
public class InitialInputRecorder extends Agent {

    @Autowired
    private UserManager userManager;

    /**
     * Initialize initial input recorder agent.
     */
    @Override
    public void init() {
        agentName = "InitialInputRecorder";
        agentStates = new HashSet<>(
            Arrays.asList(State.INITIAL_INPUT)
        );
        handleImage = false;
        useSpellChecker = true;
        this.addHandler(0, (psr) -> askAge(psr))
            .addHandler(1, (psr) -> askGender(psr))
            .addHandler(2, (psr) -> askWeight(psr))
            .addHandler(3, (psr) -> askHeight(psr))
            .addHandler(4, (psr) -> askDesiredWeight(psr))
            .addHandler(5, (psr) -> askGoalDate(psr))
            .addHandler(6, (psr) -> storeUserInfo(psr));
    }

    /**
     * Handler for asking age.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askAge(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Would you please tell me your age? " +
            "Give me an integer please ~");
        publisher.publish(fmt);
        return 1;
    }

    /**
     * Handler for asking gender.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askGender(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        if (!validateInput(1, psr.get("textContent"))) {
            rejectUserInput(psr, null);
            return 1;
        }
        states.get(userId).put("age", Integer.parseInt(psr.get("textContent")));
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Tell me your gender please, type in 'male' or 'female'");
        publisher.publish(fmt);
        return 2;
    }

    /**
     * Handler for asking weight.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askWeight(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");
        if (!validateInput(2, text)) {
            rejectUserInput(psr, null);
            return 2;
        }
        boolean isMale = true;
        for (String word : TextProcessor.getTokens(text)) {
            if (word.equals("female") || word.equals("woman")) {
                isMale = false;
                break;
            }
        }
        states.get(userId).put("gender", isMale ? "male" : "female");
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Hey, what is your weight? " +
            "Just simply give me an integer (in terms of kg)");
        publisher.publish(fmt);
        return 3;
    }

    /**
     * Handler for asking height.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askHeight(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        if (!validateInput(3, psr.get("textContent"))) {
            rejectUserInput(psr, null);
            return 3;
        }
        states.get(userId).put("weight", Integer.parseInt(psr.get("textContent")));
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("How about the height in cm?");
        publisher.publish(fmt);
        return 4;
    }

    /**
     * Handler for asking desired weight.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askDesiredWeight(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        if (!validateInput(4, psr.get("textContent"))) {
            rejectUserInput(psr, null);
            return 4;
        }
        states.get(userId).put("height", Integer.parseInt(psr.get("textContent")));
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Emmm... What is your desired weight?" +
            "(give an integer in terms of kg)");
        publisher.publish(fmt);
        return 5;
    }

    /**
     * Handler for asking goal date.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askGoalDate(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        if (!validateInput(5, psr.get("textContent"))) {
            rejectUserInput(psr, null);
            return 5;
        }
        states.get(userId).put("desiredWeight", Integer.parseInt(psr.get("textContent")));
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Alright, now tell when you want to finish this goal? " +
            "(type in yyyy-mm-dd format)");
        publisher.publish(fmt);
        return 6;
    }

    /**
     * Handler for storing user info.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int storeUserInfo(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        if (!validateInput(6, psr.get("textContent"))) {
            rejectUserInput(psr, null);
            return 6;
        }
        JSONObject state = states.get(userId);
        state.put("goalDate", psr.get("textContent"));

        JSONObject userJSON = new JSONObject();
        userJSON.put("id", userId);
        userJSON.put("age", state.getInt("age"));
        userJSON.put("gender", state.getString("gender"));
        userJSON.put("weight", state.getInt("weight"));
        userJSON.put("height", state.getInt("height"));
        userJSON.put("goal_weight", state.getInt("desiredWeight"));
        userJSON.put("due_date", state.getString("goalDate"));
        log.info("Storing UserJSON: {}", userJSON.toString(4));

        userManager.storeUserJSON(userId, userJSON);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Great! I now understand what you need!");
        publisher.publish(fmt);
        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * Validate user input.
     * @param state Current state
     * @param textContent String of user input
     * @return validation result
     */
    public boolean validateInput(int state, String textContent) {
        switch (state) {
            case 1:
                if(!Validator.isInteger(textContent)) return false;
                int age = Integer.parseInt(textContent);
                if(!Validator.validateAge(age)) return false;
                break;

            case 2:
                if (!Validator.isGender(textContent)) return false;
                break;

            case 3: case 5:
                if(!Validator.isInteger(textContent)) return false;
                int weight = Integer.parseInt(textContent);
                if (!Validator.validateWeight(weight)) return false;
                break;

            case 4:
                if(!Validator.isInteger(textContent)) return false;
                int height = Integer.parseInt(textContent);
                if (!Validator.validateWeight(height)) return false;
                break;

            case 6:
                return Validator.isFutureDate(textContent, "yyyy-MM-dd");

            default:
                return false;
        }
        return true;
    }
}