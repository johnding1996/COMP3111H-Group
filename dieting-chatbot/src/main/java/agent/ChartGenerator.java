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

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.Validator;

import java.io.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import lombok.extern.slf4j.Slf4j;

/**
 * ChartGenerator: Generate line chart according to user daily or weekly weight history.
 * Generate pie chart according to user daily or weekly menu history.
 * @author wguoaa
 * @version unfinished
 */
@Slf4j
@Component
public class ChartGenerator{

    public String[] GetWeight(){

    }

    public String[] GetNutrition(){

    }

    public static void lineChartWeekly( String[ ] args ) throws Exception {
        DefaultCategoryDataset line_chart_dataset = new DefaultCategoryDataset();
        //add values
        //line_chart_dataset.addValue( 15 , "schools" , "1970" );


        JFreeChart lineChartObject = ChartFactory.createLineChart(
                "Body Weight Vs Date","Date",
                "Body Weight",
                 line_chart_dataset,PlotOrientation.VERTICAL,
                true,true,false);

        int width = 640;    /* Width of the image */
        int height = 480;   /* Height of the image */
        File lineChart = new File( "LineChartWeekly.jpeg" );
        ChartUtilities.saveChartAsJPEG(lineChart ,lineChartObject, width ,height);
    }

    public static void lineChartDaily() throws Exception {

    }

    public static void PieChartWeekly() throws Exception {

    }

    public static void PieChartDaily() throws Exception {

    }
}
