package controller;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class State {
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

    static public boolean validateStateName(String name) {
        switch (name) {
            case "Idle": case "ParseMenu": case "AskMeal":
            case "Recommend": case "RecordMeal":
            case "InitialInput": case "Feedback":
                return true;
            default:
                return false;
        }
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