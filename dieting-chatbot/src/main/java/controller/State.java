package controller;

import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class State {
    private String stateName;

    private int timeout; // in sec
    private String timeoutState;
    public static final int DEFAULT_TIMEOUT = 600; // 1 hour
    private static HashSet<String> stateNameSet;
    static {
        stateNameSet = new HashSet<String>();
        stateNameSet.add("Idle");
        stateNameSet.add("ParseMenu");
        stateNameSet.add("AskMeal");
        stateNameSet.add("Recommend");
        stateNameSet.add("RecordMeal");
        stateNameSet.add("InitialInput");
        stateNameSet.add("Feedback");
        stateNameSet.add("AskWeight");
    }

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
        return stateNameSet.contains(name);
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