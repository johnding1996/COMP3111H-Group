package agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import controller.FormatterMessageJSON;
import controller.ParserMessageJSON;
import controller.Publisher;
import database.connection.SQLPool;
import database.querier.PartialFoodQuerier;

import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MealAsker
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserInitialInputRecord register on event bus");
        }
    }

    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `AskMeal`
        String currentState = psr.get("state");
        if (!currentState.equals("AskMeal")) return;

        log.info("Entering user ask meal handler");
        String userId = psr.get("userId");
        String replyToken = psr.get("replyToken");

        // if the input is not text
        if(!psr.getMessageType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON();
            response.set("userId", userId)
                    .set("type", "reply")
                    .set("replyToken", replyToken)
                    .appendTextMessage(
                        "Sorry but I don't understand this image");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }
    }
}