package controller;

import com.linecorp.bot.model.event.message.TextMessageContent;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * Parser.java - use to comine information into a JSONObject and pass to the observer
 * every module can at the last step construct an instance of this class to organize 
 * their message sent back to formatter
 * you can simply call constructJSONPakage() method
 * sample calling
 * 
 * 
 * 
 * the aim is to keep all the format of the JSON files transitted between those modules
 * and controller uniform
 * @version 1.0
 */

public class Parser {

    private String userID;
    private String state;
    private String replyType;
    private String replyToken;
    private String messageType;

    private String textContent;

    private String originalContentUrl;
    private String previewContentStringUrl;

    // the package to be thrown
    @Autowired
    private JSONObject msgObject;

    @Autowired
	EventBus eventBus;

	@Autowired
	CountDownLatch latch;

    // Constructors overloading

    /**
     * this is handled in handleTextContent
     */
    public Parser(String userID, String state, String replyToken, String messageType, String textContent) {
        this.userID = userID;
        this.state = state;
        this.replyToken = replyToken;
        this.messageType = messageType;
        this.textContent = textContent;
        parseBasic();
        msgObject.put("textContent", textContent); 
    }

    
	
    /**
     * this is handled in handleImageMessageEvent, I need you to privide with me the userID by calling event.getSource().getUserID()
     * for more detail please refer to how to reply to "profile" 
     */
    public Parser(String userID, String state, String replyToken, String messageType, String originalContectUrl, String previewContentStringUrl) {
        this.userID = userID;
        this.state = state;
        this.replyToken = replyToken;
        this.messageType = messageType;
        this.originalContentUrl = originalContectUrl;
        this.previewContentStringUrl = previewContentStringUrl;
        parseBasic();
        msgObject.put("originalContentUrl", originalContectUrl).put("previewContentUrl", previewContentStringUrl);
    }

    
    /** 
     * methods for judging message types
     */
    public boolean isURL() {
        String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
        
        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(textContent);//replace with string to compare
        if(m.find()) { return true; }
        else return false;
        
    }

    /**
     * call this after you create an instance of Parser
     */
    public JSONObject getJSON() { 
        return this.msgObject; 
    }

    private void parseBasic() {
        msgObject.put("userID", userID).put("state", state).put("replyToken", replyToken).put("messageType", messageType);
	}

    public void publishParserMessageJSON(ParserMessageJSON parserMessageJSON) throws InterruptedException {
        eventBus.notify("ParserMessageJSON", Event.wrap(parserMessageJSON));
        latch.await();
    }

}
