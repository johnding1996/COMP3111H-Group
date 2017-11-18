package misc;

import database.keeper.HistKeeper;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;

import org.springframework.stereotype.Component;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;


import lombok.extern.slf4j.Slf4j;

/**
 * ChartGenerator: Generate line chart according to user daily or weekly weight history.
 * Generate pie chart according to user daily or weekly menu history.
 * @author wguoaa
 * @version unfinished
 */
@Slf4j
public class ChartGenerator extends Application{
    private List<Map<String,String>> weightData = new ArrayList<>();
    private List<Map<String,String>> nutritionData = new ArrayList<>();
    private String user;
    private int duration;

    /**
     * Constructor
     * @param userId user id
     * @param durationInput weekly or daily, int
     */
    public ChartGenerator(String userId, int durationInput){
        this.user = userId;
        this.duration = durationInput;
    }

    /**
     * getWeightHist: Get the weight history list according to userid and required duration.
     * @param key userid String
     * @param dur daily or weekly int
     * @param type weight or nutrition String
     * @return weightHist list of mapped timestamp and weight
     * @throws ParseException
     */
    private List<Map<String,String>> getWeightHist(String key, int dur, String type) throws ParseException {
        List<Map<String,String>> weightHist = new ArrayList<>();
        HistKeeper histKeeper = new HistKeeper();
        DateFormat stp = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject endObj = histKeeper.get(key, 1).getJSONObject(0);
        String endDate = endObj.getString("date");
        Map<String, String> weightTimePair = new HashMap<>();
        weightTimePair.put(endDate,endObj.getString(type));
        weightHist.add(weightTimePair);
        int dates = 0;
        while (dates < dur){
            JSONObject currentObj = histKeeper.get(key, 1).getJSONObject(0);
            String currentDate = currentObj.getString("date");
            Map<String, String> currentPair = new HashMap<>();
            currentPair.put(currentObj.getString("date"),currentObj.getString(type));
            weightHist.add(0,currentPair);
            if (stp.parse(currentDate).before(stp.parse(endDate))){
                dates ++;
            }
        }
        return weightHist;

    }
    private List<Map<String,String>> getNutritionHist(String key, int dur, String type){
        List<Map<String,String>> nutritionHist = new ArrayList<>();
        HistKeeper histkeeper = new HistKeeper();
        DateFormat stp = new SimpleDateFormat("yyyy-MM-dd");
        JSONObject endObj = histkeeper.get(key,1).getJSONObject(0);
        String endDate = endObj.getString("date");
        JSONArray currentMenu = new JSONArray(endObj.getString("food"));

        int dates = 0;
        while (dates < dur){

        }


        return nutritionHist;
    }

    @Override
    public void start(Stage stage) throws ParseException {
        weightData = getWeightHist(user, duration,"weight");
        stage.setTitle("Line Chart");
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Timestamp");
        yAxis.setLabel("Body Weight");

        LineChart<String,Number> lineChart = new LineChart<String,Number>(xAxis,yAxis);

        lineChart.setTitle("Weight Line Chart");

        XYChart.Series series = new XYChart.Series();
        series.setName("My Weight");
        for (Map<String, String> data: weightData) {
            series.getData().add(new XYChart.Data(data.keySet().toString(), data.values().toArray()[0]));
        }

        Scene scene  = new Scene(lineChart,800,600);
        lineChart.getData().add(series);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}
