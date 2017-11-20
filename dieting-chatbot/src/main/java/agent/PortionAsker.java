package agent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.State;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashSet;

import utility.FormatterMessageJSON;
import utility.JsonUtility;
import utility.ParserMessageJSON;
import utility.Validator;

/**
 * PortionAsker: ask portion size of each dish.
 * 
 * State map:
 *      0 - Ask whether to skip
 *      1 - Starting ask portion
 *      2 - Update portion
 * 
 * @author cliubf, szhouan
 * @version v1.2.0
 */
@Slf4j
@Component
public class PortionAsker extends Agent {

    /**
     * THis is a private attibute MenuManager object.
     */
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
        useSpellChecker = false;
        this.addHandler(0, (psr) -> askSkip(psr))
            .addHandler(1, (psr) -> startAskPortion(psr))
            .addHandler(2, (psr) -> updatePortion(psr));
    }

    /**
     * Handler for asking whether skip PortionAsker.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askSkip(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        JSONObject menuJSON = menuManager.getMenuJSON(userId);
        states.get(userId).put("menuJSON", menuJSON);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Okay, here is your menu:")
           .appendTextMessage(JsonUtility.formatMenuJSON(menuJSON, false))
           .appendTextMessage("Would you like to tell me what is the portion " +
                "size of each dish? Key in 'Yes' or 'No'");
        publisher.publish(fmt);
        return 1;
    }

    /**
     * Handler for asking whether skip PortionAsker.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int startAskPortion(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").trim().toLowerCase();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (text.equals("yes")) {
            fmt.appendTextMessage("Okay, so give me your update in this format: " +
                    "<dish index>:<portion in gram>, such as '1:100'")
               .appendTextMessage("Typically, an apple is around 100g")
               .appendTextMessage("Note that if you finish all updates you desired, " +
                    "you just need to type 'leave' to end the session");
            publisher.publish(fmt);
            return 2;
        } else if (text.equals("no")) {
            fmt.appendTextMessage("Alright, let's move on.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.RECOMMEND);
            return END_STATE;
        } else {
            rejectUserInput(psr, "You should simply tell me yes or no.");
            return 1;
        }
    }

    /**
     * Handler for updating portion size.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int updatePortion(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").trim().toLowerCase();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (text.equals("leave")) {
            fmt.appendTextMessage("Alright, update portion finished.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.RECOMMEND);
            return END_STATE;
        } else {
            JSONObject menuJSON = states.get(userId).getJSONObject("menuJSON");
            JSONArray menu = menuJSON.getJSONArray("menu");
            int menuNum = menu.length();
            String[] tokens = text.split(":");

            boolean valid = true;
            int index = 0;
            int portion = 0;
            if (!Validator.isInteger(tokens[0]) || !Validator.isInteger(tokens[1]) ||
                tokens.length != 2) {
                valid = false;
            } else {
                index = Integer.parseInt(tokens[0]);
                portion = Integer.parseInt(tokens[1]);
                if (index < 1 || index > menuNum) valid = false;
                if (portion < 1 || portion > 7000) valid = false;
            }

            if (!valid) {
                rejectUserInput(psr, "Please enter in this format: " +
                        "<dish index>:<portion in gram>, " +
                        "both of the number shall be integer. " +
                        "Or type 'leave' if no more update desired.");
                return 2;
            } else {
                updateDatabase(index, portion, userId);
                fmt.appendTextMessage(String.format("Roger, %d gram of %s",
                    portion, menu.getJSONObject(index-1).getString("name")));
                publisher.publish(fmt);
                return 2;
            }
        }
    }

    /**
     * update portion size to MenuKeeper.
     * @param dishIndex the index of dish in menu, started by 1.
     * @param portion portion of the dish, default portion unit as gram.
     * @param userId String of user Id.
     */
    public void updateDatabase(int dishIndex, int portion, String userId) {
        JSONObject menuJSON = menuManager.getMenuJSON(userId);
        JSONObject dish = menuJSON.getJSONArray("menu").getJSONObject(dishIndex-1);
        dish.put("portionSize", portion);
        menuJSON.getJSONArray("menu").put(dishIndex-1, dish);
        menuManager.storeMenuJSON(userId, menuJSON);
    }
}