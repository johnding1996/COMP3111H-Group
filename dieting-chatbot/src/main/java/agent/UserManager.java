package agent;

import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import database.keeper.HistKeeper;
import database.keeper.StateKeeper;
import database.querier.UserQuerier;
import java.util.Arrays;
import java.util.HashSet;

/**
 * UserManager: handling user follow and unfollow event.
 * @author szhouan
 * @version v1.1.0
 */
@Slf4j
@Component
public class UserManager extends Agent {

    /**
     * Initialize user manager agent.
     */
    @Override
    public void init() {
        agentName = "UserManager";
        agentStates = new HashSet<>(
            Arrays.asList(State.FOLLOWING, State.UNFOLLOWING)
        );
        handleImage = false;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> handleUser(psr));
    }

    /**
     * Handler for follow and unfollow event.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int handleUser(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        State state = psr.getState();

        if (state == State.FOLLOWING) {
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Nice to meet you, and I am your dieting assistant! " +
                "Please input your personal information so that I could serve you better. " +
                "You could do this later by saying CANCEL.");
            publisher.publish(fmt);

            controller.setUserState(userId, State.INITIAL_INPUT);
        } else { // state == State.UNFOLLOWING
            UserQuerier userQuerier = new UserQuerier();
            userQuerier.delete(userId);
            userQuerier.close();
            log.info("Remove user {} from UserInfo table", userId);

            controller.setUserState(userId, State.IDLE);
        }
        return END_STATE;
    }

    /**
     * Get UserJSON from UserQuerier.
     * @param userId String of user Id
     * @return A UserJSON
     */
    public JSONObject getUserJSON(String userId) {
        UserQuerier querier = getUserQuerier();
        JSONObject userJSON = querier.get(userId);
        querier.close();
        return userJSON;
    }

    /**
     * Store UserJSON to UserQuerier.
     * @param userId String of user Id
     * @param userJSON userJSON to store
     */
    public void storeUserJSON(String userId, JSONObject userJSON) {
        UserQuerier querier = getUserQuerier();
        if (getUserJSON(userId) == null) {
            querier.add(userJSON);
        } else {
            querier.update(userJSON);
        }
        querier.close();
    }

    /**
     * Get UserQuerier.
     * @return A UserQuerier
     */
    public UserQuerier getUserQuerier() {
        return new UserQuerier();
    }
}