package agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import static reactor.bus.selector.Selectors.$;

import java.util.HashMap;

import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

/**
 * CampaignManager.
 * @author szhouan
 * @version unfinished
 */
@Slf4j
@Component
public class CampaignManager
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    private static HashMap<String, String> states = new HashMap<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("CampaignManager register on eventBus");
        }
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is relevant
        String userId = psr.getUserId();
        State state = psr.getState();
        if (state != State.INVITE_FRIEND &&
            state != State.CLAIM_COUPON &&
            state != State.UPLOAD_COUPON) {
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("remove internal state of user {}", userId);
            }
            return;
        }

        log.info("Entering CampaignManager");
        publisher.publish(new FormatterMessageJSON(userId));

        if (state == State.INVITE_FRIEND) {
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Great, you want to invite friend!");
            publisher.publish(fmt);

            controller.setUserState(userId, State.IDLE);
        } else if (state == State.CLAIM_COUPON) {
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Great, you want to claim coupon!");
            publisher.publish(fmt);

            controller.setUserState(userId, State.IDLE);
        } else { // state == State.UPLOAD_COUPON
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Great, you want to upload coupon!");
            publisher.publish(fmt);

            controller.setUserState(userId, State.IDLE);
        }
    }
}