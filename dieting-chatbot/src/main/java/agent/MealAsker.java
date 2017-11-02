package agent;

import org.json.JSONObject;
import org.json.JSONArray;
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
import java.util.HashMap;

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

    static private HashMap<String, JSONObject> menus = new HashMap<>();

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserInitialInputRecord register on event bus");
        }
    }

    /**
     * Clear all QueryJSON
     */
    public void clearQueryJSON() {
        log.info("Removing all QueryJSON object");
        menus.clear();
    }

    /**
     * set QueryJSON for a user
     * @param json QueryJSON to add
     */
    public void setQueryJSON(JSONObject json) {
        if (validateQueryJSON(json))
            menus.put((String)json.get("userId"), json);
        else log.info("Invalid Query JSON:\n" + json.toString(4));
    }

    /**
     * get QueryJSON for a user
     * @param userId String of user Id
     * @return JSONObject
     */
    public JSONObject getQueryJSON(String userId) {
        if (menus.containsKey(userId)) return menus.get(userId);
        else return null;
    }

    /**
     * Validate QueryJSON
     * @param json QueryJSON to check
     * @return A boolean, whether the format is valid
     */
    public static boolean validateQueryJSON(JSONObject json) {
        try {
            String userId = (String)json.get("userId");
            JSONArray menu = (JSONArray)json.get("menu");
            for (int i=0; i<menu.length(); ++i) {
                JSONObject dish = menu.getJSONObject(i);
                String dishName = (String)dish.get("name");
                assert dishName != null;
            }
            return true;
        } catch (Exception e) {
            log.info("Invalid QueryJSON: {}", e.toString());
            return false;
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