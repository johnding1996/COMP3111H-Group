package com.example.bot.spring;

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
    {
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
            transitionTable.put("ParseMenu", tempTable);

            // AskMeal
            tempTable = new HashMap<String, String>();
            tempTable.put("confirmMeal", "Recommend");
            transitionTable.put("AskMeal", tempTable);

            // Recommend
            tempTable = new HashMap<String, String>();
            /* only timeout transition */
            transitionTable.put("Recommend", tempTable);

            // RecordMeal
            tempTable = new HashMap<String, String>();
            tempTable.put("confirmMeal", "Idle");
            transitionTable.put("RecordMeal", tempTable);

            // InitialInput
            tempTable = new HashMap<String, String>();
            tempTable.put("userInitialInput", "Idle");
            transitionTable.put("InitialInput", tempTable);

            // Feedback
            tempTable = new HashMap<String, String>();
            tempTable.put("sendFeedback", "Idle");
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
     */
    public StateMachine(String userId) {
        this.userId = userId;
    }

    /**
     * Initialize the state machine
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
     * Overwrite state of the state machine, for testing only
     */
    public void setState(String newState) {
        if (!states.containsKey(newState)) {
            log.info("Set to invalid newState: " + newState);
            return;
        }
        currentState = newState;        
        log.info("State overwritten to " + newState);
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
     */
    public void toNextState(String transition) {
        if (!transitionTable.get(currentState).containsKey(transition)) {
            log.info(String.format("Invalid transition %s from state %s",
                transition, currentState));
            return;
        }
        currentState = transitionTable.get(currentState).get(transition);
        log.info(String.format("To new state %s by %s", currentState,
            transition));
    }
}

@Slf4j
class State {
    private String stateName;

    private int timeout;
    private String timeoutState;
    static final int DEFAULT_TIMEOUT = 3600; // 1 hour

    /**
     * Construct a state with default timeoutState and timeout
     * @param stateName
     */
    public State(String stateName) throws Exception {
        if (validateStateName(stateName))
            this.stateName = stateName;
        else {
            log.info("Invalid state name: " + stateName);
            assert false;
        }
        this.timeout = DEFAULT_TIMEOUT;
        this.timeoutState = "Idle";
        assert validateStateName(timeoutState);
    }

    /**
     * Construct a state that will change to timeoutState after timeout
     * @param stateName
     * @param timeout the time period in sec after which timeout event triggered
     * @param timeoutState the new state after timeout occurs
     */
    public State(String stateName, int timeout, String timeoutState) throws Exception {
        if (validateStateName(stateName))
            this.stateName = stateName;
        else {
            log.info("Invalid state name: " + stateName);
            assert false;
        }
        this.timeout = timeout;
        if (validateStateName(timeoutState))
            this.timeoutState = timeoutState;
        else {
            log.info("Invalid timeout state: " + timeoutState);
            assert false;
        }
    }

    private boolean validateStateName(String name) {
        switch (name) {
            case "Idle": case "ParseMenu": case "AskMeal":
            case "Recommend": case "RecordMeal":
            case "InitialInput": case "Feedback":
            return true;
        }
        return false;
    }

    public String getName() {
        return stateName;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getTimeoutState() {
        return timeoutState;
    }

    @Override
    public String toString() {
        return "State " + stateName;
    }
}