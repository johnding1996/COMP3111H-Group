package agent;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;

import database.keeper.MenuKeeper;
import org.json.JSONArray;
import org.json.JSONObject;

import controller.State;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import org.springframework.util.ResourceUtils;

import com.linecorp.bot.client.MessageContentResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * MenuParser: handle menu in text, url and image.
 * @author cliubf, szhouan
 * @version v2.1.0
 */
@Slf4j
@Component
public class MenuParser extends Agent {

    /**
     * Initialize menu parser agent.
     */
    @Override
    public void init() {
        agentName = "MenuParser";
        agentStates = new HashSet<>(
            Arrays.asList(State.PARSE_MENU)
        );
        handleImage = true;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> askMenu(psr))
            .addHandler(1, (psr) -> parseMenu(psr));
    }

    /**
     * Handler for asking menu.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askMenu(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Long time no see! What is your menu today? " +
            "You could use text or URL, or an image.");
        publisher.publish(fmt);
        return 1;
    }

    /**
     * Handler for parsing menu.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int parseMenu(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String type = psr.getType();

        // parsing menu
        JSONArray menuArray = null;
        if (type.equals("text")) {
            String text = psr.get("textContent");
            if (ResourceUtils.isUrl(text)) {
                menuArray = UrlMenuParser.buildMenu(text);
            } else {
                menuArray = TextMenuParser.buildMenu(text);
            }
        } else {
            String uri = psr.get("imageContent");
            log.info("{}: get image with URI: {}", agentName, uri);
            menuArray = ImageMenuParser.buildMenu("uri", uri); 
        }

        // check parsed menu and reply
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if(menuArray == null) {
            log.info("{}: empty menu", agentName);
            fmt.appendTextMessage("Looks like the menu is empty, " +
                "please try again");
            publisher.publish(fmt);
            return 1;
        }
        else {
            JSONObject menuJSON = new JSONObject();
            menuJSON.put("userId", userId)
                    .put("menu", menuArray);

            // keep menu in redis
            MenuKeeper keeper = new MenuKeeper();
            keeper.set(userId, menuJSON);
            keeper.close();

            publisher.publish(fmt);
            controller.setUserState(userId, State.ASK_MEAL);
            return END_STATE;
        }
    }
}