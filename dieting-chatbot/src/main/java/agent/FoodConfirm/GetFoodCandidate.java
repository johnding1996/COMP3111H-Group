package agent.FoodConfirm;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Super class for {@link GetFoodCandidatePartial} and {@link GetFoodCandidateFuzzy}
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
    GetFoodCandidate (JSONObject foodQuery) {
        foodQueryData = foodQuery;
    }

    /**
     * getFoodName
     * Return a list of input food name for query
     * @return Arraylist an array of food names
     */

    public  List<String> getFoodName() {
        List<String> foodNameList = new ArrayList<>();

        JSONArray foodList = foodQueryData.getJSONArray("menu");
        for (int i = 0; i < foodList.length(); i++){
            JSONObject dishInfo = (JSONObject)foodList.get(i);
            String dishName = dishInfo.getString("name");
            foodNameList.add(dishName);
        }
        return foodNameList;

    }

}
