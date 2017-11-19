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
import controller.*;
import database.keeper.HistKeeper;
import net.arnx.jsonic.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchart.*;
import org.knowm.xchart.internal.series.Series;
import org.knowm.xchart.style.Styler;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;

import static reactor.bus.selector.Selectors.$;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Feedback.class, JazzySpellChecker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class FeedbackTester {
    private static String user_id = "913f61a35fbb9cc3adc28da525abf1fe";
    @Autowired
    private Feedback feedback;

    @Autowired
    private Publisher publisher;

    @Autowired
    private ChatbotController controller;

}
