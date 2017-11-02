package agent;

import database.keeper.HistKeeper;
import database.keeper.MenuKeeper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.Integer;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;
import org.json.JSONObject;

import controller.ParserMessageJSON;
import controller.Publisher;
import controller.FormatterMessageJSON;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;

import utility.Validator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ConfirmFood implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("ConfirmFood register on event bus");
        }
    }

    /**
     * User states tracking for interaction; false stands for user did not confirm food list yet
     */
    private static Map<String, Boolean> userStates = new HashMap<>();

    void changeUserState(String userId, boolean state) {
        userStates.put(userId, state);
    }

    /**
     * add userInfo to history if everything is correct
     * @param idxs list of indices
     */
    public void addDatabase (List<Integer> idxs, String userId, MenuKeeper menuKeeper, HistKeeper histKeeper)
            throws NumberFormatException {
        List<String> foodNames = new ArrayList<>();
        try{
            JSONArray menu = menuKeeper.get(userId, 1).getJSONObject(0).getJSONArray("menu");
            for(int j = 0; j < menu.length(); j++){
                foodNames.add(menu.getJSONObject(j).getString("name"));
            }
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
        for (Integer idx: idxs) {
            if (idx < 1 || idx > foodNames.size()) {
                throw new NumberFormatException();
            }
        }
        for (Integer idx: idxs) {
            JSONObject foodJson = new JSONObject();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            foodJson.put("date", dateFormat.format(Calendar.getInstance()));
            foodJson.put("number_of_meal", 1);
            foodJson.put("food", foodNames.get(idx));
            DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            foodJson.put("timestamp", dateTimeFormat.format(Calendar.getInstance()));

        }

        log.info(String.format("Stored the meal history of user %s in to the caches.", userId));
    }

    /**
     * print list for user to select
     * @param userId user id string
     * @param response FormatterMessageJSON object
     */
    public void printList(String userId, FormatterMessageJSON response, MenuKeeper menuKeeper){
        try{
            JSONArray menu = menuKeeper.get(userId, 1).getJSONObject(0).getJSONArray("menu");
            for(int j = 0; j < menu.length(); j++){
                JSONObject food = menu.getJSONObject(j);
                response.appendTextMessage((j + 1) + ". " + food.getString("name"));
            }
            response.appendTextMessage("Please enter in a list of indices separated by ';' (e.g. 1;3;4).");
        } catch (JSONException e){
            log.warn("MenuKeeper returns an empty or invalid JSONArray", e);
        }
    }

    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        MenuKeeper menuKeeper = new MenuKeeper();
        HistKeeper histKeeper = new HistKeeper();


        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `RecordMeal`
        String currentState = psr.get("state");
        if (!currentState.equals("RecordMeal")) return;

        log.info("Entering user meal confirm handler");
        String userId = psr.get("userId");
        String replyToken = psr.get("replyToken");

        // if the input is not text
        if(!psr.getMessageType().equals("text")) {
            FormatterMessageJSON response = new FormatterMessageJSON();
            response.set("userId", userId)
                    .set("type", "reply")
                    .set("replyToken", replyToken)
                    .appendTextMessage(
                            "Please input some text at this moment.");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, false);
        }
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "reply")
                .set("replyToken", replyToken);
        log.info(psr.toString());

        boolean selection = userStates.get(userId);
        if (!selection) {
            response.appendTextMessage("Welcome back! Please choose among what you just ate in this list:");
            printList(userId, response, menuKeeper);
            userStates.put(userId, true);
        } else {
            String[] idxStrings = psr.getTextContent().split(";");
            try {
                List<Integer> idxs = new ArrayList<>();
                for (String idxString: idxStrings) {
                    int idx = Integer.parseInt(idxString);
                    idxs.add(idx);
                }
                response.set("stateTransition", "confirmMeal")
                        .appendTextMessage("Great! I have recorded what you have just eaten!");
                addDatabase(idxs, userId, menuKeeper, histKeeper);
                userStates.remove(userId);
            } catch (NumberFormatException e) {
                response.appendTextMessage("Sorry, please enter in a list of valid indices separated by ';'.");
            }
        }
        menuKeeper.close();
        histKeeper.close();
        publisher.publish(response);
    }
}
