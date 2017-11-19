package misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.InputStream;
import java.lang.Integer;

import agent.FoodRecommender;
import controller.ImageControl;
import database.keeper.HistKeeper;
import net.arnx.jsonic.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import controller.Publisher;
import controller.State;
import controller.ChatbotController;

import org.knowm.xchart.*;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.Styler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static reactor.bus.selector.Selectors.$;

import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

import org.knowm.xchart.internal.ChartBuilder;
import org.knowm.xchart.internal.chartpart.Chart;


/**
 * Generate user weight line chart and nutrient pie chart.
 * @author mcding, wguoaa
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
    private List<String> allNutrients = Arrays.asList("lipid_tot", "carbohydrate","sugar_tot",
            "protein","fiber_td","vit_c","sodium","potassium","calcium");
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
        // register user if it is new
        if (!states.containsKey(userId)) {
            log.info("register new user {}", userId);
            states.put(userId, 0);
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
            int result = parseFeedbackDuration(userId, msg);
            if (result != -1) {
                JSONArray histJSON = getHist(userId, result);
                if (histJSON.length() == 0) {
                    log.info(String.format("Empty hist json for user %s", userId));
                    FormatterMessageJSON response = new FormatterMessageJSON(userId);
                    response.appendTextMessage("Sorry you don't have any recorded history, session cancelled.");
                    publisher.publish(response);
                    states.remove(userId);
                    if (controller != null) {
                        controller.setUserState(userId, State.IDLE);
                    }
                }
                parseWeightHist(histJSON);
                log.info("FEEDBACK: user timestamps" + timestamps.toString());
                log.info("FEEDBACK: user weight histories" + weights.toString());
                drawLineChart(userId);
                parseNutrientHist(userId, histJSON);
                log.info("FEEDBACK: user nutrition hist" + nutrients.toString());
                drawPieChart(userId);
                states.remove(userId);
                if (controller != null) {
                    controller.setUserState(userId, State.IDLE);
                }
            }
        } else {
            log.error("Invalid internal state in feedback handler.");
        }
    }

    /**
     * Draw pie chart according to userId and user nutrition hist.
     * @param userId user's unique id
     */
    public void drawPieChart(String userId) {
        PieChart chart = new PieChartBuilder().width(800).height(600).title(getClass().getSimpleName()).build();
        for (Map<String,Double> onePair: nutrients){
            chart.addSeries(onePair.keySet().iterator().next(),onePair.values().iterator().next());
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.BMP);
            byte[] bitmapData = outputStream.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(bitmapData);
            String tempFileUri = ImageControl.inputToTempFile("bmp", inputStream);
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendImageMessage(tempFileUri, tempFileUri);
        } catch (IOException e) {
            log.error("Error encountered when saving charts in feedback handler.", e);
        }
    }

    /**
     * Draw line chart of user's weight.
     * @param userId user's unique id
     */
    public void drawLineChart(String userId) {
        XYChart chart = new XYChartBuilder().width(800).height(400)
                .title("Weight Line Chart")
                .xAxisTitle("Time")
                .yAxisTitle("Weight").build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        chart.addSeries("weight", timestamps, weights);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.BMP);
            byte[] bitmapData = outputStream.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(bitmapData);
            String tempFileUri = ImageControl.inputToTempFile("bmp", inputStream);
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendImageMessage(tempFileUri, tempFileUri);
            publisher.publish(fmt);
        } catch (IOException e) {
            log.error("Error encountered when saving charts in feedback handler.", e);
        }
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
            log.error("Error encountered when fetching user weight hist from hist keeper.", e);
        }
    }

    /**
     * Get the nutrient consumption statistics from hist JSONArray.
     */
    private void parseNutrientHist(String userId, JSONArray histJSON) {
        FoodRecommender foodRecommender = new FoodRecommender();
        int dishNum = 0;
        List<Double> allScore = new ArrayList<>(9);
        try {
            int i;
            for (i=0; i<histJSON.length(); i++) {
                JSONObject hist = histJSON.getJSONObject(i);
                JSONArray menu = hist.getJSONArray("menu");
                for(int j = 0; j < menu.length(); j++) {
                    JSONArray food = menu.getJSONObject(j).getJSONArray("foodContent");
                    JSONObject result = foodRecommender.calculateNutrientIntakes(userId,food);
                    dishNum++;
                    for (int m = 0; m < allNutrients.size(); ++m) {
                        JSONObject nutrientScore = result.getJSONObject(allNutrients.get(m));
                        double score = nutrientScore.getDouble("actual")/nutrientScore.getDouble("expect") + allScore.get(i);
                        allScore.set(m, score);
                    }
                }
            }
            log.info(String.format("Successfully fetched user nutrient hist from hist keeper, %d records were found.", i));
        } catch (JSONException e) {
            log.error("Error encountered when fetching user hist from hist keeper.", e);
        }
        for (int i = 0; i< allNutrients.size(); ++i){
            double finalScore = allScore.get(i)/dishNum;
            Map<String, Double> pair = new HashMap<>();
            pair.put(allNutrients.get(i),finalScore);
            nutrients.add(pair);
        }
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
        JSONArray histJSON = histKeeper.get(userId, date);
        histKeeper.close();
        return histJSON;
    }

    /**
     * Parse the duration from input message.
     * @param msg message
     * @return int of duration
     */
    private int parseFeedbackDuration(String userId, String msg) {
        try {
            int duration = Integer.parseInt(msg);
            if (duration<=0) throw new NumberFormatException();
            return duration;
        } catch (NumberFormatException e) {
            log.info("Invalid input when parsing duration in feedback handler.", e);
            FormatterMessageJSON response = new FormatterMessageJSON(userId);
            response.appendTextMessage("Invalid input, please give me a positive integer.");
            publisher.publish(response);
            return -1;
        }
    }



}