package agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

/**
 * Agent: abstract class for chatbot {@link Agent}.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public abstract class Agent implements Consumer<Event<ParserMessageJSON>> {
    /**
     * MessageHandler interface: implemented by concrete agent.
     */
    interface MessageHandler {
        /**
         * Handle a message given its ParserMessageJSON.
         * @param psr Input ParserMessageJSON
         * @return next state
         */
        public int handleMessage(ParserMessageJSON psr);
    }

    /**
     * This is a private attribute EventBus.
     */
    @Autowired
    private EventBus eventBus;

    /**
     * This is a private attribute Publisher.
     */
    @Autowired
    Publisher publisher;

    /**
     * This is a private attribute Controller.
     */
    @Autowired
    ChatbotController controller;

    /**
     * This is a private attribute JazzySpellChecker.
     */
    @Autowired
    JazzySpellChecker spellChecker;

    /**
     * This is a hasMap storing the handler index.
     */
    HashMap<Integer, MessageHandler> handlers = new HashMap<>();

    /**
     * Internal state of the agent module.
     */
    HashMap<String, JSONObject> states = new HashMap<>();

    /**
     * Name of the agent module, to be overriden.
     */
    String agentName = "DefaultAgent";

    /**
     * Whether handle image message.
     */
    boolean handleImage = false;

    /**
     * Whether use spell checker.
     */
    boolean useSpellChecker = false;

    /**
     * Whether acknowledge controller when message handled.
     */
    boolean acknowledgeController = true;

    /**
     * Responsible states of the agent, to be overriden.
     */
    HashSet<State> agentStates = new HashSet<>(
        Arrays.asList(State.INVALID)
    );

    /**
     * Special integer for start  of responsibility of the agent.
     */
    static final int START_STATE = 0;

    /**
     * Special integer for end of send of responsibility of the agent.
     */
    static final int END_STATE = -1;

    /**
     * Number of milliseconds to sleep between adjacent push.
     */
    private static final int SLEEP_TIME = 300;

    /**
     * Add handler for an internal state.
     * @param state State represented in integer
     * @param handler A MessageHandler
     * @return this object
     */
    public Agent addHandler(int state, MessageHandler handler) {
        handlers.put(state, handler);
        return this;
    }

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void registerEventBus() {
        init();
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("{} register on eventBus", agentName);
        }
    }

    /**
     * Agent initialization, to be overriden.
     */
    public abstract void init();

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (!agentStates.contains(globalState)) {
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("{}: Remove user {}", agentName, userId);
            }
            return;
        }

        log.info("Entering agent {}", agentName);
        if (acknowledgeController) {
            publisher.publish(new FormatterMessageJSON(userId));
        }

        // check image message
        if (psr.getType().equals("image") && !handleImage) {
            log.info("Agent {} does not handle image", agentName);
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Sorry but you cannot use image at this step :(");
            publisher.publish(fmt);
            return;
        }

        // correct user input
        if (psr.getType().equals("text") && useSpellChecker) {
            String textContent = psr.get("textContent");
            String correctedContent = spellChecker.getCorrectedText(textContent);
            if (!textContent.equals(correctedContent)) {
                FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                fmt.appendTextMessage("Corrected input: " + correctedContent);
                publisher.publish(fmt);
                psr.set("textContent", correctedContent);
                sleep();
            }
        }

        // register state for first entry
        if (!states.containsKey(userId)) {
            registerUser(userId);
        }
        JSONObject stateJSON = states.get(userId);
        int state = stateJSON.getInt("state");
        log.info(String.format("%s: Internal state is %d", agentName, state));
        MessageHandler handler = handlers.get(state);
        int nextState = handler.handleMessage(psr);
        if (nextState == END_STATE) {
            states.remove(userId);
        } else {
            stateJSON.put("state", nextState);
        }

        log.info("Leaving agent {}", agentName);
    }

    /**
     * Reject user input.
     * @param psr Input ParserMessageJSON
     * @param hint Customized hint for user, leave null if none
     */
    public void rejectUserInput(ParserMessageJSON psr, String hint) {
        String userId = psr.getUserId();
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage(String.format(
            "OOPS, your input \"%s\" is invalid :(", psr.get("textContent")));
        if (hint != null) {
            fmt.appendTextMessage(hint);
        }
        fmt.appendTextMessage("Please do it again >_<");
        publisher.publish(fmt);
    }

    /**
     * Get internal state of a user.
     * @param userId String of user Id
     * @return internal state in integer
     */
    public int getUserState(String userId) {
        if (states.containsKey(userId)) {
            return states.get(userId).getInt("state");
        } else {
            return END_STATE;
        }
    }

    /**
     * Register internal state of a user.
     * @param userId String of user Id
     */
    public void registerUser(String userId) {
        JSONObject state = new JSONObject();
        state.put("state", START_STATE);
        states.put(userId, state);
        log.info("{}: Register new user {}", agentName, userId);
    }

    /**
     * Set internal state of a user.
     * This function is mainly for testing.
     * @param userId String of user Id
     * @param newState new state in integer
     */
    public void setUserState(String userId, int newState) {
        if (!states.containsKey(userId)) {
            log.info("{}: no such user {}", agentName, userId);
        } else {
            states.get(userId).put("state", newState);
        }
    }

    /**
     * Sleep for a few milli seconds.
     */
    public void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (Exception e ) {
            log.warn("{}: Sleep interrupted", agentName);
        }
    }
}