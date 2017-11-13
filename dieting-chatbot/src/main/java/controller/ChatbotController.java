package controller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import utility.Validator;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;

/**
 * ChatbotController: interfacing with LINE API, handle global state transition.
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
@Service
@LineMessageHandler
public class ChatbotController
    implements Consumer<reactor.bus.Event<
        FormatterMessageJSON>> {

    private HashMap<String, State> states = new HashMap<>();

    @Autowired(required = false)
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private Publisher publisher;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TaskScheduler taskScheduler;

    private static final boolean debugFlag = true;
    public static final String DEBUG_COMMAND_PREFIX = "$$$";
    private static final int NO_REPLY_TIMEOUT = 3;
 
    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("FormatterMessageJSON"), this);
            log.info("Register FormatterMessageJSON on eventBus");
        }
    }

    /**
     * EventBus FormatterMessageJSON event handler.
     * Send LINE message if required.
     * @param ev FormatterMessageJSON event
     */
    public void accept(Event<FormatterMessageJSON> ev) {
        FormatterMessageJSON fmt = ev.getData();
        String userId = fmt.getUserId();

        // build message list
        List<Message> messages = new ArrayList<>();
        JSONArray arr = fmt.getMessageArray();
        if (arr.length() == 0) {
            return;
        }
        for (int i=0; i<arr.length(); ++i) {
            JSONObject obj = arr.getJSONObject(i);
            switch (obj.getString("type")) {
                case "text":
                    messages.add(new TextMessage(obj.getString("textContent")));
                    break;
                case "image":
                    messages.add(new ImageMessage(obj.getString("originalContentUrl"),
                        obj.getString("previewContentUrl")));
                    break;
                default:
                    log.info("Invalid message type {}", obj.getString("type"));
            }
        }
        log.info("CONTROLLER: Send push message");
        PushMessage pushMessage = new PushMessage("U"+userId, messages);
        if (lineMessagingClient != null)
            lineMessagingClient.pushMessage(pushMessage);
    }

    /**
     * Event handler for LINE text message.
     * @param event LINE text message event
     */
    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event)
        throws Exception {

        String userId = event.getSource().getUserId();
        String textContent = event.getMessage().getText();
        String messageId = event.getMessage().getId();

        // remove first letter 'U' from userId
        int endIndex = userId.length();
        userId = userId.substring(1, endIndex);

        // construct user state
        if (!states.containsKey(userId)) {
            states.put(userId, State.IDLE);
        }

        // test
        if (getUserState(userId) == State.IDLE) {
            setUserState(userId, State.INITIAL_INPUT);
        }

        // publish message
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.set("messageId", messageId)
           .set("textContent", textContent);
        publisher.publish(psr);
    }

    /**
     * Get state of a user.
     * @param userId String of user Id
     * @return State of the user. State.INVALID is returned if there
     *         is no record for this user
     */
    public State getUserState(String userId) {
        if (states.containsKey(userId)) {
            return states.get(userId);
        } else {
            return State.INVALID;
        }
    }

    /**
     * Set state of a user, register timeout callback, and publish the transition
     * @param userId String of user Id
     * @param newState New state for the user
     * @return A boolean indicating whether set state succeed
     */
    public boolean setUserState(String userId, State newState) {
        if (states.containsKey(userId)) {
            if (states.get(userId) != newState) {
                ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
                publisher.publish(psr);
            }
            states.put(userId, newState);
            return true;
        } else {
            return false;
        }
    }
}