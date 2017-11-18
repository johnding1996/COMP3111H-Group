package misc;

import java.text.DateFormat;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;

import database.keeper.HistKeeper;
import org.apache.commons.lang3.time.DateUtils;
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
        log.info("Entering user initial input handler");
        publisher.publish(new FormatterMessageJSON(userId));

        // if the input is image
        if(psr.getType().equals("image")) {
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage("Please input some text at this moment");
            publisher.publish(response);
            log.info("Cannot handle image message");
            return;
        }

        // register user if it is new
        if (!states.containsKey(userId)) {
            log.info("register new user {}", userId);
            states.put(userId, 0);
        }
    }



    public void drawWeightLineChart() {
        //XYChart chart = QuickChart.getChart("Sample Chart", "X", "Y", "y(x)", xData, yData);
    }

    /**
     * getWeightHist: Get the weight history list according to userid and required duration.
     * @param userId userid String
     * @param duration daily or weekly int
     * @return weightHist list of mapped timestamp and weight
     */
    private List<Map<Date,Double>> getWeightHist(String userId, int duration) {
        List<Map<Date,String>> weightHist = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -duration);
        Date date = cal.getTime();
        date = DateUtils.truncate(date, Calendar.DATE);
        HistKeeper histKeeper = new HistKeeper();
        histKeeper.get(userId, date);
        return new ArrayList<Map<Date, Double>>();
    }

}