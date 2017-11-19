package agent;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.State;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashSet;

import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

/**
 * PortionAsker: ask portion size of each dish.
 * @author cliubf, szhouan
 * @version v1.1.0
 */
@Slf4j
@Component
public class PortionAsker extends Agent {

    @Autowired
    private MenuManager menuManager;

    /**
     * Initialize portion asker agent.
     */
    @Override
    public void init() {
        agentName = "PortionAsker";
        agentStates = new HashSet<>(
            Arrays.asList(State.ASK_PORTION)
        );
        handleImage = false;
        useSpellChecker = true;
        this.addHandler(0, (psr) -> askPortion(psr));
    }

    /**
     * Handler for asking portion size.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askPortion(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        JSONObject menuJSON = menuManager.getMenuJSON(userId);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Skipping state: AskPortion");

        menuManager.storeMenuJSON(userId, menuJSON);

        publisher.publish(fmt);
        controller.setUserState(userId, State.RECOMMEND);
        return END_STATE;
    }
}