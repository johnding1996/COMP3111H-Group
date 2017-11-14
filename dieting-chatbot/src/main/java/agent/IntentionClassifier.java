package agent;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import static reactor.bus.selector.Selectors.$;

import java.util.Arrays;
import java.util.HashSet;

import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;

/**
 * IntentionClassifier: classifier intention of the user and change global state.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class IntentionClassifier 
    implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired(required = false)
    private ChatbotController controller;

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("IntentionClassifier register on event bus");
        }
    }

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.IDLE ||
            psr.getType().equals("transition")) return;

        log.info("Entering user intention classifier");

        // if the input is not text
        if(!psr.getType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage("Oops, please tell me your intention in another way");
            publisher.publish(response);
            return;
        }

        String msg = psr.get("textContent");
        State state = getUserIntention(msg);
        if (state != State.IDLE && controller != null) {
            controller.setUserState(userId, state);
        }
    }

    /**
     * Get the intention of the user given a sentence.
     * @param msg User input sentence.
     * @return The state should change to given user's intention.
     */
    private State getUserIntention(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (recommendKeywords.contains(word)) return State.PARSE_MENU;
            if (initialInputKeywords.contains(word)) return State.INITIAL_INPUT;
            if (feedbackKeywords.contains(word)) return State.FEEDBACK;
        }
        return State.IDLE;
    }
    private static HashSet<String> recommendKeywords;
    private static HashSet<String> initialInputKeywords;
    private static HashSet<String> feedbackKeywords;
    static {
        recommendKeywords = new HashSet<>(
            Arrays.asList(
                "recommendation", "recommendations", "recommend",
                "menu", "suggestion", "suggest"
            )
        );
        initialInputKeywords = new HashSet<>(
            Arrays.asList(
                "setting", "settings", "personal"
            )
        );
        feedbackKeywords = new HashSet<>(
            Arrays.asList(
                "feedback", "report", "digest"
            )
        );
    }
}