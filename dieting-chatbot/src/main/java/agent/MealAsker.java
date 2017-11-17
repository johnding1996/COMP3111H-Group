package agent;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.JsonUtility;
import controller.State;
import java.util.Arrays;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

/**
 * MealAsker: interact with user to get the appropriate menu.
 * @author szhouan
 * @version v2.1.0
 */
@Slf4j
@Component
public class MealAsker extends Agent {

    @Autowired
    private MenuManager menuManager;

    /**
     * Initialize initial input recorder agent.
     */
    @Override
    public void init() {
        agentName = "MealAsker";
        agentStates = new HashSet<>(
            Arrays.asList(State.ASK_MEAL)
        );
        handleImage = false;
        useSpellChecker = true;
        this.addHandler(0, (psr) -> showMenu(psr));
    }

    /**
     * Handler for showing menu.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int showMenu(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        JSONObject menuJSON = menuManager.getMenuJSON(userId);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (menuJSON == null) {
            fmt.appendTextMessage("Looks like your menu is empty. Session cancelled.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        } else {
            // query database for food content
            JsonUtility.getFoodContent(menuJSON);

            fmt.appendTextMessage("Well, I got your menu.")
               .appendTextMessage("The Menu I got is\n" +
                        JsonUtility.formatMenuJSON(menuJSON, false))
               .appendTextMessage("And this is the food " +
                        "content of each dish I found:")
               .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, true));

            menuManager.storeMenuJSON(userId, menuJSON);

            publisher.publish(fmt);
            controller.setUserState(userId, State.ASK_PORTION);
            return END_STATE;
        }
    }
}