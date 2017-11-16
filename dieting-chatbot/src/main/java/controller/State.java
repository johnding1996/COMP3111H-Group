package controller;


import java.util.Date;

/**
 * Enum class for global state representation.
 * @author szhouan
 * @version v2.0.0
 */
public enum State {

    /**
     * Invalid state.
     * This state is an indicator of errors.
     */
    INVALID("Invalid"),
    /**
     * Idle state.
     * This is the initial and final state for any operation.
     */
    IDLE("Idle"),
    /**
     * Initial input state.
     * State for user initial input.
     */
    INITIAL_INPUT("InitialInput"),
    /**
     * Parser menu state.
     * State for parsing user menu input.
     */
    PARSE_MENU("ParseMenu"),
    /**
     * Ask meal state.
     * State for interacting with user on the menu given.
     */
    ASK_MEAL("AskMeal"),
    /**
     * Recommend state.
     * State for giving recommendation.
     */
    RECOMMEND("Recommend"),
    /**
     * Record meal state.
     * State for recording what the user has eaten.
     */
    RECORD_MEAL("RecordMeal"),
    /**
     * Feedback state.
     * State for providing user feedback information.
     */
    FEEDBACK("Feedback"),
    /**
     * Ask weight state.
     * Not used in the current system.
     */
    ASK_WEIGHT("AskWeight"),
    /**
     * Invite friend state.
     * State for handling `friend` command from user.
     */
    INVITE_FRIEND("InviteFriend"),
    /**
     * Claim coupon state.
     * State for handling `code` command from new user.
     */
    CLAIM_COUPON("ClaimCoupon"),
    /**
     * Upload coupon state.
     * State for handling coupon image upload for admin user.
     */
    UPLOAD_COUPON("UploadCoupon"),
    /**
     * Following state.
     * State when user follows the chatbot.
     */
    FOLLOWING("Following"),
    /**
     * Unfollowing state.
     * State when user unfollows the chatbot.
     */
    UNFOLLOWING("Unfollowing");

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
            if (state.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return corresponding state for a string.
     * @param name String of state name.
     * @return Corresponding state.
     */
    public static State getStateByName(String name) {
        for (State state : State.values()) {
            if (state.name.equals(name)) {
                return state;
            }
        }
        return State.INVALID;
    }

    /**
     * Return Date object for time that is TIMEOUT_PERIOD sec later.
     * @return A Date object as described.
     */
    public static Date getTimeoutDate() {
        return new Date((new Date()).getTime() + 1000 * TIMEOUT_PERIOD);
    }
}