package controller;

import java.util.Date;

/**
 * Enum class for global state representation.
 * @author szhouan
 * @version v2.0.0
 */
public enum State {
    INVALID("Invalid"),
    IDLE("Idle"),
    INITIAL_INPUT("InitialInput"),
    PARSE_MENU("ParseMenu"),
    ASK_MEAL("AskMeal"),
    RECOMMEND("Recommend"),
    RECORD_MEAL("RecordMeal"),
    FEEDBACK("Feedback"),
    ASK_WEIGHT("AskWeight"),
    INVITE_FRIEND("InviteFriend"),
    CLAIM_COUPON("ClaimCoupon"),
    UPLOAD_COUPON("UploadCoupon");

    private final String name;

    /**
     * Timeout period for the states.
     */
    private static final int TIMEOUT_PERIOD = 3600;

    /**
     * Construct a State enum with given name.
     * @param name String of state name.
     */
    private State(String name) {
        this.name = name;
    }

    /**
     * Get name of the state.
     * @return name of the state.
     */
    public String getName() {
        return name;
    }

    /**
     * Overriden toString method for State enum.
     * @return name of the state.
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Validate whether a string is a valid state name.
     * @param name String of state name.
     * @return Whether name is a valid state name.
     */
    public static boolean validateStateName(String name) {
        for (State state : State.values()) {
            if (state.name == name) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return Date object for time that is TIMEOUT_PERIOD sec later.
     * @return A Date object as described.
     */
    public static Date getTimeoutDate() {
        return new Date((new Date()).getTime() + 1000 * TIMEOUT_PERIOD);
    }
}