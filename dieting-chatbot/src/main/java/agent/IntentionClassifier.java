package agent;

import org.springframework.stereotype.Component;

import controller.State;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;

import utility.ParserMessageJSON;
import utility.TextProcessor;

/**
 * IntentionClassifier: classifier intention of the user and change global state.
 * @author szhouan
 * @version v1.1.0
 */
@Slf4j
@Component
public class IntentionClassifier extends Agent {

    /**
     * THis is a private HashSet store the recommend keywords.
     */
    private HashSet<String> recommendKeywords;

    /**
     * THis is a private HashSet store the initial input keywords.
     */
    private HashSet<String> initialInputKeywords;

    /**
     * THis is a private HashSet store the feedbck keywords.
     */
    private HashSet<String> feedbackKeywords;

    /**
     * Initialize intention classifier agent.
     */
    @Override
    public void init() {
        agentName = "IntentionClassifier";
        agentStates = new HashSet<>(
            Arrays.asList(State.IDLE)
        );
        handleImage = false;
        useSpellChecker = false;
        acknowledgeController = false;
        this.addHandler(0, (psr) -> parseIntention(psr));

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

    /**
     * Handler for intention input.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int parseIntention(ParserMessageJSON psr) {
        if (psr.getType().equals("transition")) return END_STATE;
        String text = psr.get("textContent");
        State globalState = State.IDLE;
        for (String word : TextProcessor.getTokens(text)) {
            if (recommendKeywords.contains(word)) globalState = State.PARSE_MENU;
            if (initialInputKeywords.contains(word)) globalState = State.INITIAL_INPUT;
            if (feedbackKeywords.contains(word)) globalState = State.FEEDBACK;
        }
        if (text.toLowerCase().startsWith("friend")) globalState = State.INVITE_FRIEND;
        if (text.toLowerCase().startsWith("code")) globalState = State.CLAIM_COUPON;
        if (text.toLowerCase().startsWith("admin mode")) globalState = State.MANAGE_CAMPAIGN;
        if (globalState != State.IDLE) {
            controller.setUserState(psr.getUserId(), globalState);
        }
        return END_STATE;
    }
}