package agent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;

import controller.ImageControl;
import database.keeper.HistKeeper;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import controller.Publisher;
import controller.State;
import controller.ChatbotController;

import org.knowm.xchart.*;
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

import utility.TextProcessor;


/**
 * Generate user weight line chart and nutrient pie chart.
 * 
 * State map:
 *      0 - Ask feedback duration
 *      1 - Ask chart type
 *      2 - Send chart to user
 * 
 * @author mcding, wguoaa, szhouan
 * @version v2.1.0
 */
@Slf4j
@Component
public class FeedbackManager extends Agent {

    @Autowired
    private FoodRecommender recommender;

    /**
     * Initialize initial input recorder agent.
     */
    @Override
    public void init() {
        agentName = "FeedbackManager";
        agentStates = new HashSet<>(
            Arrays.asList(State.FEEDBACK)
        );
        handleImage = false;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> askDuration(psr))
            .addHandler(1, (psr) -> askChartType(psr))
            .addHandler(2, (psr) -> sendChart(psr));
    }

    /**
     * Handler for asking feedback duration.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askDuration(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Ok, how many dates do you want for feedback? Please input a positive integer.");
        publisher.publish(fmt);
        return 1;
    }

    /**
     * Handler for asking chart type.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int askChartType(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        int resultDuration = parseFeedbackDuration(userId, text);
        if (resultDuration == -1) { // invalid input
            rejectUserInput(psr, "Your input for duration is invalid.");
            return 1;
        }
        states.get(userId).put("resultDuration", resultDuration);

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        JSONArray histJSON = getHist(userId, resultDuration);
        if (histJSON == null || histJSON.length() == 0) {
            fmt.appendTextMessage("Sorry you don't have any recorded history, session cancelled.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        parseWeightHist(userId, histJSON);
        JSONArray timestamps = states.get(userId).getJSONArray("timestamps");
        JSONArray weights = states.get(userId).getJSONArray("weights");
        log.info("FEEDBACK: user timestamps" + timestamps.toString(4));
        log.info("FEEDBACK: user weight histories" + weights.toString(4));
        parseNutrientHist(userId, histJSON);
        JSONObject nutrients = states.get(userId).getJSONObject("nutrients");
        log.info("FEEDBACK: user nutrition hist" + nutrients.toString(4));
        fmt.appendTextMessage("Great, We've analyzed the history of your weights and meals, " +
                "and generated two charts for you. Which chart would you like to have a look at? " +
                "Please reply \'weight\' or \'nutrient\'.");
        publisher.publish(fmt);
        return 2;
    }

    /**
     * Handler for sending chart to user.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int sendChart(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").trim().toLowerCase();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (text.equals("weight")) {
            fmt.appendTextMessage("Loading your chart, please wait...");
            publisher.publish(fmt);
            drawLineChart(userId);
        } else if (text.equals("nutrient")) {
            fmt.appendTextMessage("Loading your chart, please wait...");
            publisher.publish(fmt);
            drawPieChart(userId);
        } else {
            rejectUserInput(psr, "Invalid chart type. " +
                    "Please reply \'weight\' or \'nutrient\'.");
            return 2;
        }
        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * Draw pie chart according to userId and user nutrition hist.
     * @param userId user's unique id
     */
    public void drawPieChart(String userId) {
        PieChart chart = new PieChartBuilder().width(800).height(600).
                title("Nutrient Pie Chart").build();
        JSONObject nutrients = states.get(userId).getJSONObject("nutrients");
        for (Object o : nutrients.keySet()){
            String nutrient = (String) o;
            chart.addSeries(nutrient, nutrients.getDouble(nutrient));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.BMP);
            sendChart(userId, outputStream);
        } catch (IOException e) {
            log.error("Error encountered when saving charts in feedback handler.", e);
        }
    }

    /**
     * Draw line chart of user's weight.
     * @param userId user's unique id
     */
    public void drawLineChart(String userId) {
        XYChart chart = new XYChartBuilder().width(800).height(600)
                .title("Weight Line Chart")
                .xAxisTitle("Time")
                .yAxisTitle("Weight").build();
        JSONArray timestamps = states.get(userId).getJSONArray("timestamps");
        JSONArray weights = states.get(userId).getJSONArray("weights");
        List<Date> timestampList = new ArrayList<>();
        List<Integer> weightList = new ArrayList<>();
        for (int i=0; i<timestamps.length(); ++i) {
            timestampList.add(new Date(timestamps.getLong(i)));
        }
        for (int i=0; i<weights.length(); ++i) {
            weightList.add(weights.getInt(i));
        }
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);
        chart.addSeries("weight", timestampList, weightList);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BitmapEncoder.saveBitmap(chart, outputStream, BitmapEncoder.BitmapFormat.BMP);
            sendChart(userId, outputStream);
        } catch (IOException e) {
            log.error("Error encountered when saving charts in feedback handler.", e);
        }
    }

    /**
     * Send chart ot image controller.
     * @param userId user id
     * @param outputStream outputStream contains chart in bmp format.
     */
    public void sendChart(String userId, ByteArrayOutputStream outputStream) {
        byte[] bitmapData = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bitmapData);
        String tempFileUri = ImageControl.inputToTempFile("bmp", inputStream);
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendImageMessage(tempFileUri, tempFileUri);
        publisher.publish(fmt);
    }

    /**
     * Get the weight history list from hist JSONArray.
     * @param userId String of user Id
     * @param histJSON user hist JSONArray
     */
    private void parseWeightHist(String userId, JSONArray histJSON) {
        JSONArray timestamps = new JSONArray();
        JSONArray weights = new JSONArray();
        try {
            int i;
            for (i=0; i<histJSON.length(); i++) {
                JSONObject hist = histJSON.getJSONObject(i);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                Date timestamp = format.parse(hist.getString("timestamp"));
                int weight = hist.getInt("weight");
                timestamps.put(timestamp.getTime());
                weights.put(weight);
            }
            states.get(userId).put("timestamps", timestamps);
            states.get(userId).put("weights", weights);
            log.info(String.format("Successfully fetched user hist from hist keeper, %d records were found.", i));
        } catch (JSONException | ParseException e) {
            log.error("Error encountered when fetching user weight hist from hist keeper.", e);
        }
    }

    /**
     * Get the nutrient consumption statistics from hist JSONArray.
     */
    private void parseNutrientHist(String userId, JSONArray histJSON) {
        JSONObject nutrients = new JSONObject();
        try {
            for (int i=0; i<histJSON.length(); i++) {
                JSONObject hist = histJSON.getJSONObject(i);
                JSONArray foodContent = hist.getJSONArray("menu").getJSONObject(0).getJSONArray("foodContent");
                int portionSize = hist.getInt("portionSize");
                JSONArray foodList = recommender.getFoodJSON(foodContent);
                for (int j=0; j<FoodRecommender.nutrientDailyIntakes.length(); j++) {
                    String nutrient = FoodRecommender.nutrientDailyIntakes.getJSONObject(j).getString("name");
                    String desc = FoodRecommender.nutrientDailyIntakes.getJSONObject(j).getString("desc");
                    double actualIntake = portionSize * recommender.getAverageNutrient(foodList, nutrient) / 100 * 3;
                    double expectedIntake = FoodRecommender.nutrientDailyIntakes.getJSONObject(j).getInt("y");
                    if (!nutrients.keySet().contains(desc)) {
                        nutrients.put(desc, 0.0);
                    }
                    double x = nutrients.getDouble(desc);
                    nutrients.put(desc, actualIntake/expectedIntake/histJSON.length() + nutrients.getDouble(desc));
                }
            }
            states.get(userId).put("nutrients", nutrients);
            log.info("Successfully calculated the user nutrient consumptions.");
        } catch (JSONException e) {
            log.error("Error encountered when fetching user hist from hist keeper.", e);
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
            return -1;
        }
    }
}