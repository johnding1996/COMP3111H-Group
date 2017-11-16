package agent;

import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import database.keeper.HistKeeper;
import database.keeper.StateKeeper;
import database.querier.UserQuerier;

/**
 * UserManager: handling user follow and unfollow event.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class UserManager implements Consumer<Event<ParserMessageJSON>> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    /**
     * Register on event bus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserManager register on eventBus");
        }
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `Following` or `Unfollowing`
        String userId = psr.getUserId();
        State state = psr.getState();
        if (state != State.FOLLOWING && state != State.UNFOLLOWING) {
            return;
        }

        log.info("Entering UserManager");
        publisher.publish(new FormatterMessageJSON(userId));

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

            controller.setUserState(userId, State.IDLE);
        }
    }
}