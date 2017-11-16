package agent;

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

import java.util.HashMap;

import reactor.fn.Consumer;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

/**
 * PortionAsker: ask portion size of each dish.
 * @author cliubf, szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class PortionAsker implements Consumer<Event<ParserMessageJSON>> {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired
    private FoodRecommender recommender;

    @Autowired(required = false)
    private ChatbotController controller;

    /**
     * User menus internal memory for food recommendation.
     */
    private HashMap<String, JSONObject> menus = new HashMap<>();

    private HashMap<String, Integer> states = new HashMap<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("PortionAsker register on eventBus");
        }
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object.
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.ASK_PORTION) {
            if (states.containsKey(userId)) {
                states.remove(userId);
                menus.remove(userId);
                log.info("Clear user {}", userId);
            }
            return;
        }

        log.info("Entering PortionAsker");
        publisher.publish(new FormatterMessageJSON(userId));

        if (menus.containsKey(userId)) {
            recommender.setMenuJSON(menus.remove(userId));
        }
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Skipping state: AskPortion");
        publisher.publish(fmt);
        if (controller != null) {
            controller.setUserState(userId, State.RECOMMEND);
        }
    }

    /**
     * Set MenuJSON for a user.
     * @param json menuJSON to add.
     */
    public void setMenuJSON(JSONObject json) {
        menus.put(json.getString("userId"), json);
    }
}