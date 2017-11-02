package src.main.java.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PraseMenuInput implements Consumer<Event<ParserMessageJSON>> {
	
	private static ParseMenuFromText ParseText = new ParseMenuFromText();
	private static ParseMenuFromWeb ParseWeb = new ParseMenuFromWeb();
	
	// User state tracking for whether user has select which method to input
    private static HashMap<String, Boolean> userStates =
        new HashMap<String, Boolean>();
	
    @Autowired
    private EventBus eventBus;

    @Autowired
    private Publisher publisher;

    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("ParserMessageJSON"), this);
            log.info("UserInitialInputRecord register on event bus");
        }
    }
    
    /**
     * Used for pass QueryJSON
     * @param QueryJSON, user id, and FormatterMessageJSON response for appending message
     */
    public void passJSON(JSONObject QJSON, String id, FormatterMessageJSON response) {
    	if(QJSON == null) {
    		userStates.remove(id);
    		response.appendTextMessage(
                    "Ops! Something went wrong, you need to do this again");
    	}
    	else {
    		//fabricated method, waiting for feature 5 to complete
    		setFoodInfo(QJSON, id);
    		userStates.remove(id);
    		response.set("stateTransition", "menuMessage")
            		.appendTextMessage("Great! Your food list has been successfully recorded");
    	}
    }
    
    /**
     * Event handler for ParserMessageJSON
     * @param ev Event object
     */
    public void accept(Event<ParserMessageJSON> ev) {
        ParserMessageJSON psr = ev.getData();

        // only handle message if state is `ParseMenu`
        String currentState = psr.get("state");
        if (!currentState.equals("ParseMenu")) return;

        log.info("Entering user meau input handler");
        String userId = psr.get("userId");
        String replyToken = psr.get("replyToken");
        
        // register user if it is new
        if (!userStates.containsKey(userId)) {
            log.info("register new user {}", userId);
            userStates.put(userId, false);
        }
        
        boolean selection = userStates.get(userId);
        FormatterMessageJSON response = new FormatterMessageJSON();
        response.set("userId", userId)
                .set("type", "reply")
                .set("replyToken", replyToken);
        log.info(psr.toString());
        
        if (!selection) {
            response.appendTextMessage(
                "Long time no see! What food did you just enjoy? Enter 'text' for text input OR directly give me a link");
            userStates.put(userId, true);
        } else {
        	String option = psr.getTextMessage();
        	String msg = "";
        	JSONObject qJSON;
        	if(option.equals("text")) {
        		qJSON = ParseText.textGet(psr);
        	}
        	else {
        		qJSON = ParseWeb.webGet(psr);      		
        	}
        	passJSON(qJSON, userId, response);
        }
        publisher.publish(response);
    }
}
