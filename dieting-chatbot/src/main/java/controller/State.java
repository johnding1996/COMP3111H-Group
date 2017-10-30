package controller;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class State {
    private String stateName;

    private int timeout; // in sec
    private String timeoutState;
    static final int DEFAULT_TIMEOUT = 30; // 1 hour

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

    /**
     * Get name of the state
     * @return String of state name
     */
    public String getName() {
        return stateName;
    }

    /**
     * Get timeout of the state
     * @return An integer, representing timeout in sec
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Get timeout state name
     * @return String of timeout state
     */
    public String getTimeoutState() {
        return timeoutState;
    }

    /**
     * Set timeout of the state
     * @param newTimeout new timeout in sec, must be nonnegative
     * @return None
     */
    public void setTimeout(int newTimeout) {
        if (newTimeout >= 0) {
            timeout = newTimeout;
        } else {
            log.info("STATE: attempting to set invalid timeout {}", newTimeout);
        }
    }

    @Override
    public String toString() {
        return "State " + stateName;
    }
}