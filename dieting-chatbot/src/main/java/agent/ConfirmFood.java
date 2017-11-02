package src.main.java.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
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

    // User state tracking for interaction; false stands for user did not enter food list yet
    private static HashMap<String, Boolean> userStates =
            new HashMap<String, Boolean>();

    /**
     * add userInfo to history if everything is correct
     */
    public void addDatabase(String foodInfo) {
        String[] food = foodInfo.split(";");
        JSONObject userJSON = new JSONObject();
        userJSON.put("id", u.id);
        userJSON.put("age", u.age);
        userJSON.put("gender", u.gender);
        userJSON.put("weight", u.weight);
        userJSON.put("height", u.height);

        JSONObject goal = new JSONObject();
        goal.put("weight", u.desiredWeight);
        goal.put("due", u.goalDate);
        userJSON.put("goal", goal);

        // add user info to database and remove
        log.info("User Info of {} is ready for database", u.id);
        // setUserInfo(u.id, userJSON);
        userStates.remove(u.id);
    }
}
