package misc;

import java.text.DateFormat;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;

import database.keeper.HistKeeper;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import controller.Publisher;
import controller.State;
import controller.ChatbotController;
import database.querier.UserQuerier;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.swabunga.spell.event.SpellChecker;

import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import utility.Validator;

/**
 * Generate user weight line chart and nutrient pie chart.
 * @author wguoaa
 * @version v2.0.0
 */
@Slf4j
@Component
public class Feedback implements Consumer<Event<ParserMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @Autowired
    private JazzySpellChecker spellChecker;

    @Autowired(required = false)
    private ChatbotController controller;

    private List<Date> timestamps = new ArrayList<>();
    private List<Integer> weights = new ArrayList<>();
    private Set<Map<String, Double>> nutrients = new HashSet<>();

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("InitialInputRecorder register on event bus");
        }
    }

    // User state tracking for interaction
    private static HashMap<String, Integer> states = new HashMap<>();

    /**
     * Event handler for ParserMessageJSON.
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();
        // Is it my duty?
        String userId = psr.getUserId();
        State globalState = psr.getState();
        if (globalState != State.FEEDBACK) {
            // not my duty, clean up if needed
            if (states.containsKey(userId)) {
                states.remove(userId);
                log.info("Clear user {}", userId);
            }
            return;
        }
        log.info("FEEDBACK:\n{}", psr.toString());
        // Acknowledge that the psr is handled
        log.info("Entering feedback handler");
        publisher.publish(new FormatterMessageJSON(userId));
        // if the input is image
        if(psr.getType().equals("image")) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage("Please input some text at this moment");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // main interaction
        int state = states.get(userId);
        if (state==0) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage("Ok, how many dates do you want for feedback? Please input an positive integer.");
            publisher.publish(response);
            states.put(userId, 1);
        } else if (state==1) {
            String msg = psr.get("textContent");
            if (parseFeedbackDuration(msg) != -1) {
                JSONArray histJSON = getHist(userId, parseFeedbackDuration(msg));
                parseWeightHist(histJSON);
                log.info("TIMESTAMPS:" + timestamps.toString());
                log.info("WEIGHTS:" + weights.toString());
                states.remove(userId);
                if (controller != null) {
                    controller.setUserState(userId, State.IDLE);
                }
            }
        } else {
            log.error("Invalid internal state in feedback handler.");
        }
    }


    public void drawWeightLineChart() {
        //XYChart chart = QuickChart.getChart("Sample Chart", "X", "Y", "y(x)", xData, yData);
    }

    /**
     * Get the weight history list from hist JSONArray.
     */
    private void parseWeightHist(JSONArray histJSON) {
        timestamps = new ArrayList<>();
        weights = new ArrayList<>();
        try {
            int i;
            for (i=0; i<histJSON.length(); i++) {
                JSONObject hist = histJSON.getJSONObject(i);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                Date timestamp = format.parse(hist.getString("timestamp"));
                int weight = hist.getInt("weight");
                timestamps.add(timestamp);
                weights.add(weight);
            }
            log.info(String.format("Successfully fetched user hist from hist keeper, %d records were found.", i));
        } catch (JSONException | ParseException e) {
            log.error("Error encountered when fetching user hist from hist keeper.", e);
        }
    }

    /**
     * Get the nutrient consumption statistics from hist JSONArray.
     */
    private void parseNutrientHist(JSONArray histJSON) {

    }

    /**
     * Get the hist JSONArray form HistKeeper.
     * @param userId userid String
     * @param duration number of days to query
     * @return user hist JSONArray
     */
    private JSONArray getHist(String userId, int duration) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -duration);
        Date date = calendar.getTime();
        date = DateUtils.truncate(date, Calendar.DATE);
        HistKeeper histKeeper = new HistKeeper();
        return histKeeper.get(userId, date);
    }

    /**
     * Parse the duration from input message.
     * @param msg message
     * @return int of duration
     */
    private int parseFeedbackDuration(String msg) {
        try {
            int duration = Integer.parseInt(msg);
            if (duration<=0) throw new NumberFormatException();
            return duration;
        } catch (NumberFormatException e) {
            log.info("Invalid input when parsing duration in feedback handler.", e);
            return -1;
        }
    }



}