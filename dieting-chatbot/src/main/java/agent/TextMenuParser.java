package agent;

import java.util.HashMap;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.lang.Integer;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TextMenuParser {

    /**
     * Build menu information from text
     * @param text String of user input text
     * @return A JSON array; null if no dish is successfully parsed
     */
    public static JSONArray buildMenu(String text) {
        JSONArray arr = new JSONArray();
        String[] lines = text.split("\n");
        for (String line : lines) {
            JSONObject dish = new JSONObject();
            dish.put("name", line);
            arr.put(dish);
        }
        return arr;
    }

    // @Autowired(required = false)
    // private Publisher publisher;

    // // User state tracking for interaction
    // private static ArrayList<PraseMeauInputUser> userList =
    //     new ArrayList<PraseMeauInputUser>();

    // /**
    //  * Compose food details in to JSON format required by QueryJSON
    //  * @param detail, String of food detail
    //  *             Should be in format: 1-cup, 100-g, 2-spoon etc.
    //  * @param name, String of food name
    //  * @return JSONObject containing food details
    //  */
    // public JSONObject addFoodDetails(String name, String detail) {
    //     JSONObject newFood = new JSONObject();
    //     String[] portion = detail.split("-");
    //     newFood.put("name", name);
    //     newFood.put("portionSize", portion[0]);
    //     newFood.put("portionUnit", portion[1]);
    //     newFood.put("ingredients", "");
    //     newFood.put("price", "");
    //     return newFood;
    // }

    // /**
    //  * Compose food JSONArray along with user id in to QueryJSON
    //  */
    // public JSONObject addQueryJSON(JSONArray meau, String id) {
    //     JSONObject thisMeal = new JSONObject();
    //     thisMeal.put("userId", id);
    //     thisMeal.put("meau", thisMeal);
    //     return thisMeal;
    // }

    // /**
    //  * find if user exists in userList
    //  * @param id, user id
    //  * @return reference of user, null if not exist
    //  */
    // public PraseMeauInputUser findUser(String id) {
    //     PraseMeauInputUser user = null;
    //     boolean searchResult = false;
    //     for(PraseMeauInputUser u : userList) {
    //         if(id.equals(u.id)) {
    //             user = u;
    //             searchResult = true;
    //         }
    //     }
    //     if(searchResult)
    //         return user;
    //     else return null;
    // }

    // /**
    //  * compose QueryJSON from user's sequential input
    //  * @param ParserMessageJSON input
    //  * @return a QueryJSON if process completed, else null
    //  */
    // public JSONObject textGet(ParserMessageJSON PMJ) {

    //     String userID = PMJ.get("userId");
    //     String replyToken = PMJ.get("replyToken");
    //     JSONArray meau = new JSONArray();       
    //     PraseMeauInputUser p = findUser(userID);

    //     if(p == null) {
    //         p = new PraseMeauInputUser(userID);
    //         FormatterMessageJSON response = new FormatterMessageJSON();
    //         response.set("userId", userID)
    //                 .set("type", "reply")
    //                 .set("replyToken", replyToken)
    //                 .appendTextMessage("Enter food names in format: food1;food2;food3");
    //         publisher.publish(response);
    //         log.info("User start to input text");
    //         this.userList.add(p);
    //     }

    //     else {
    //         FormatterMessageJSON response = new FormatterMessageJSON();
    //         response.set("userId", userID)
    //                 .set("type", "reply")
    //                 .set("replyToken", replyToken);
    //         log.info(PMJ.toString());

    //         if(p.getState() == 0) {
    //             p.foodList = PMJ.getTextContent().split(";");
    //             response.appendTextMessage("Nice! For " + p.foodList[0] + " How much do you have? Enter food names in format: 100-g OR 1-cup (split by '-')");
    //             p.moveState();
    //         }
    //         else if(p.getState() < p.foodList.length) {
    //             meau.add(addFoodDetails(p.foodList[p.getState() - 1], PMJ.getTextContent()));
    //             response.appendTextMessage("okay, For " + p.foodList[p.getState() - 1] + " How much do you have? Enter food names in format: 100-g OR 1-cup (split by '-')");
    //             p.moveState();
    //         }
    //         else {
    //             meau.add(addFoodDetails(p.foodList[p.getState() - 1], PMJ.getTextContent()));
    //             response.appendTextMessage("Great! I have record your menu!");

    //             this.userList.remove(p);
    //             publisher.publish(response);
    //             return addQueryJSON(meau, userID);              
    //         }
    //         publisher.publish(response);
    //     }               
    //     return null;
    // }

    // /**
    //  * Inner class for tracking user interaction
    //  * Note that state = 0 stands for initialization of food list; 
    //  *      state = 1 stands for the first food entering the JSONArray
    //  */
    // class PraseMeauInputUser {
    //     private int state = 0;

    //     String id;
    //     String[] foodList;

    //     public PraseMeauInputUser(String id) {
    //         this.id = id;
    //     }

    //     public int getState() {
    //         return this.state;
    //     }

    //     public void moveState() {
    //         this.state++;
    //     }
    // }
}