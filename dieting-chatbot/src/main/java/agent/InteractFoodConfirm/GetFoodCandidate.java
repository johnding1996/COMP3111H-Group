package agent.InteractFoodConfirm;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Super class for {@link getFoodCandidatePartial} and {@link getFoodCandidateFuzzy}
 * @author wguoaa
 * @version 1.2
 */
@Slf4j
public class GetFoodCandidate {

    private JSONObject foodQueryData;

    /**
     * constructor
     * Default constructor.
     */
    GetFoodCandidate () {
        super();
        foodQueryData = new JSONObject();
    }

    /**
     * getFoodName
     * Return a list of input food name for query
     * @return Arraylist an array of food names
     */

    public  ArrayList<String> getFoodName() {
        ArrayList<String> foodNameList = new ArrayList<>();

        JSONArray foodList = foodQueryData.getJSONArray("menu");
        for (int i = 0; i < foodList.length(); i++){
            JSONObject dishInfo = (JSONObject)foodList.get(i);
            String dishName = dishInfo.getString("name");
            foodNameList.add(dishName);

        }
        return foodNameList;

    }

}
