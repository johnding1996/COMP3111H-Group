package controller;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.linecorp.bot.model.event.message.MessageContent;

import lombok.extern.slf4j.Slf4j;
import lombok.Value;

@Slf4j
public class StateMachine {
    private String userId;
    static private Map<String, State> states = new HashMap<String, State>();
    static private Map<String, Map<String, String>> transitionTable
        = new HashMap<String, Map<String, String>>();
    private String currentState;

    /**
     * Initialize states and transitionTable
     */
    static {
        try {
            states.put("Idle", new State("Idle"));
            states.put("ParseMenu", new State("ParseMenu"));
            states.put("AskMeal", new State("AskMeal"));
            states.put("Recommend", new State("Recommend",
                State.DEFAULT_TIMEOUT, "RecordMeal"));
            states.put("RecordMeal", new State("RecordMeal"));
            states.put("InitialInput", new State("InitialInput"));
            states.put("Feedback", new State("Feedback"));

            // check
            for (Map.Entry<String, State> entry : states.entrySet())
                assert entry.getKey().equals(entry.getValue().getName());
        } catch (Exception e) {
            log.info("Error in init states: " + e.toString());
        }
        try {
            Map<String, String> tempTable;

            // Idle
            tempTable = new HashMap<String, String>();
            tempTable.put("recommendationRequest", "ParseMenu");
            tempTable.put("initialInputRequest", "InitialInput");
            tempTable.put("feedbackRequest", "Feedback");
            transitionTable.put("Idle", tempTable);

            // ParseMenu
            tempTable = new HashMap<String, String>();
            tempTable.put("menuMessage", "AskMeal");
            tempTable.put("timeout", "Idle");
            transitionTable.put("ParseMenu", tempTable);

            // AskMeal
            tempTable = new HashMap<String, String>();
            tempTable.put("confirmMeal", "Recommend");
            tempTable.put("timeout", "Idle");
            transitionTable.put("AskMeal", tempTable);

            // Recommend
            tempTable = new HashMap<String, String>();
            /* only timeout transition */
            tempTable.put("timeout", "RecordMeal");
            transitionTable.put("Recommend", tempTable);

            // RecordMeal
            tempTable = new HashMap<String, String>();
            tempTable.put("confirmMeal", "Idle");
            tempTable.put("timeout", "Idle");
            transitionTable.put("RecordMeal", tempTable);

            // InitialInput
            tempTable = new HashMap<String, String>();
            tempTable.put("userInitialInput", "Idle");
            tempTable.put("timeout", "Idle");
            transitionTable.put("InitialInput", tempTable);

            // Feedback
            tempTable = new HashMap<String, String>();
            tempTable.put("sendFeedback", "Idle");
            tempTable.put("timeout", "Idle");
            transitionTable.put("Feedback", tempTable);

            // check
            for (Map.Entry<String, Map<String, String>> entry : transitionTable.entrySet()) {
                assert states.containsKey(entry.getKey());
                for (Map.Entry<String, String> trans : entry.getValue().entrySet()) {
                    assert states.containsKey(trans.getValue());
                }
            }
        } catch (Exception e) {
            log.info("Error in init transitionTable: " + e.toString());
        }
    }

    /**
     * Construct state machine for a user
     * @param userId String of user id
     */
    public StateMachine(String userId) {
        this.userId = userId;
        initialize();
    }

    /**
     * Initialize the state machine
     * @return None
     */
    public void initialize() {
        setState("Idle");
    }
    
    /**
     * Get current state
     * @return current state in String
     */
    public String getState() {
        return currentState;
    }

    /**
     * Get current State object
     * @return The current State object
     */
    public State getStateObject() {
        return states.get(currentState);
    }

    /**
     * Overwrite state of the state machine, for testing only
     * @param newState new state
     */
    public void setState(String newState) {
        if (!isValidState(newState)) {
            log.info("Set to invalid newState: " + newState);
            return;
        }
        currentState = newState;        
        log.info("State overwritten to " + newState);
    }

    /**
     * Check whether a String is a valid state
     * @param stateName Name of state in String
     */
    public static boolean isValidState(String stateName) {
        return states.containsKey(stateName);
    }

    /**
     * Get user id
     * @return userId for the StateMachine in String
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set user id
     * @param userId new userId
     */
    public void setUserId(String userId) {
        // todo: check validity of userId
        this.userId = userId;
    }

    /**
     * Go to next state according to transition String
     * @param transition a String encoding the transition
     * @return whether state transition is successful
     */
    public boolean toNextState(String transition) {
        if (!transitionTable.get(currentState).containsKey(transition)) {
            log.info(String.format("Invalid transition %s from state %s",
                transition, currentState));
            return false;
        }
        currentState = transitionTable.get(currentState).get(transition);
        log.info(String.format("To new state %s by %s", currentState,
            transition));
        return true;
    }
}
