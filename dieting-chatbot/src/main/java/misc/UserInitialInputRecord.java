package src.main.java.misc;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.Integer;
import org.json.JSONObject;
import jdk.nashorn.internal.runtime.ParserException;

import controller.ParserMessageJSON;
import controller.FormatterMessageJSON;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import static reactor.bus.selector.Selectors.$;
import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;

public class UserInitialInputRecord implements Consumer<Event<ParserMessageJSON>> {
	
    @Autowired(required=false)
    private EventBus eventBus;

    @PostConstruct
    public void init() {
        if (eventBus != null)
            eventBus.on($("ParserMessageJSON"), this);
    }
	
    //This list stores users' state initially
	private static ArrayList<UserInitialState> userList = new ArrayList<UserInitialState>();
	
	//check whether string is convertable to int
	public static boolean isInt(String str) {  
	  try {  
	    int i = Integer.parseInt(str);  
	  } catch(NumberFormatException nfe) {  
	    return false;  
	  }  
	  return true;  
	}
	
	//This method checks whether the user input is valid; if so, return 0
	//invalid age returns 1
	//invalid gender returns 2
	//invalid weight returns 3
	//invalid height returns 4
	//invalid due date returns 5
	int checkValid(String type, String textContent) {
		switch(type) {
			case "age":
				if(!isInt(textContent))
					return 1;
				int age = Integer.parseInt(textContent);
				if(age < 5 || age > 95)
					return 1;
				break;
			case "gender":
				if(!textContent.equals("male") || !textContent.equals("female"))
					return 2;
				break;
			case "weight":
			case "desiredWeight":
				if(!isInt(textContent))
					return 3;
				int weight = Integer.parseInt(textContent);
				if(weight < 25 || weight > 225)
					return 3;
				break;
			case "height":
				if(!isInt(textContent))
					return 4;
				int height = Integer.parseInt(textContent);
				if(height < 25 || height > 225)
					return 4;
				break;
			case "due":
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					Date date1 = sdf.parse(textContent);
					Calendar today = Calendar.getInstance();
					if(today.after(date1))
						return 5;
				} catch(ParseException pe) {
					return 5;
				}
				
			default:
				return -1;
		}		
		return 0;
	}
	
	//add userInfo to database if everything is correct
	public void addDatabase(UserInitialState u) {
		JSONObject goal = new JSONObject();
		goal.put("weight", u.desiredWeight);
		goal.put("due", u.goalDate);
		
		JSONObject UserJSON = new JSONObject();
		UserJSON.put("id", u.id);
		UserJSON.put("age", u.age);
		UserJSON.put("gender", u.gender);
		UserJSON.put("weight", u.weight);
		UserJSON.put("height", u.height);
		UserJSON.put("goal", goal);
		
		//add this to database and remove
		setUserInfo(u.id, UserJSON);
		userList.remove(u);
	}
	
	//This method deals with the situation when user add this chatbot as friend
	public void accept(Event<ParserMessageJSON> ev) {
		ParserMessageJSON PMJ = ev.getData();
		//if the input is not text
		if(PMJ.getTextContent() == null) {
			FormatterMessageJSON response = new FormatterMessageJSON();
			response.appendTextMessage("plz input some text in this moment ~");
			return;
		}
		
		//check whether the user in progress of inputting basic info
		String userID = PMJ.get("userID");
		UserInitialState user;
		boolean searchResult = false;
		for(UserInitialState u : userList) {
			if(userID.equals(u.getName())) {
				user = u;
				searchResult = true;
			}
		}
		
		//deal with different progress
		//user is already registered if searchResult returns true
		if(searchResult) {
			if(checkValid(user.getState(), PMJ.getTextContent()) != 0) {
				FormatterMessageJSON response = new FormatterMessageJSON();
				response.appendTextMessage("plz input a valid value according to instruction");
			}
			else {
				switch(user.getState()) {
					case "age":
						user.age = Integer.parseInt(PMJ.getTextContent());
						FormatterMessageJSON askGender = new FormatterMessageJSON();
						askGender.appendTextMessage("Tell me your gender please, type in 'male' or ' female' ");
						break;
					case "gender":
						user.gender = PMJ.getTextContent();
						FormatterMessageJSON askWeight = new FormatterMessageJSON();
						askWeight.appendTextMessage("Hey, what your weight, jsut simply give me an integer (in terms of kg)");
						break;
					case "weight":
						user.weight = Integer.parseInt(PMJ.getTextContent());
						FormatterMessageJSON askHeight = new FormatterMessageJSON();
						askHeight.appendTextMessage("How about the height, give me an integer (in terms of CM)");
						break;
					case "height":
						user.height = Integer.parseInt(PMJ.getTextContent());
						FormatterMessageJSON desiredWeight = new FormatterMessageJSON();
						desiredWeight.appendTextMessage("Emmm...What is your desired weight? (give an integer in terms of kg)");
						break;
					case "desiredWeight":
						user.desiredWeight = Integer.parseInt(PMJ.getTextContent());
						FormatterMessageJSON dueDate = new FormatterMessageJSON();
						dueDate.appendTextMessage("Alright, now tell when you want to finish this goal? (type in yyyy-mm-dd format)");
						break;
					//Note that after the below case, the user info should be complete and need to be added to database
					case "goalDate":
						user.goalDate = PMJ.getTextContent();
						FormatterMessageJSON finish = new FormatterMessageJSON();
						finish.appendTextMessage("Great! I now understand you need!");
						//if goalDate can be successful input, user info is complete
						addDatabase(user); return;
						break;
					default:
						FormatterMessageJSON done = new FormatterMessageJSON();
						done.appendTextMessage("OMG, something bad happens, you may need to re-add me");
						assert false;
				}
				
				user.moveState();
			}
		}
		
		//create a new user
		else {
			user = new UserInitialState(PMJ.get("userID"));
			FormatterMessageJSON askAge = new FormatterMessageJSON();
			askAge.appendTextMessage("Hello ~ would you mind tell me your age? Give me an integer please ~");
			userList.add(user);
			user.moveState();
		}
	}
}