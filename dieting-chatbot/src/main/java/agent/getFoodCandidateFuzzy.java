package agent;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.stream.JsonParser;

import database.querier.FuzzyFoodQuerier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author wguoaa
 * @version 1.2
 */

@Slf4j
public class getFoodCandidateFuzzy extends getFoodCandidate  {


    public JSONObject getFoodCandidate(ArrayList<String> foodNameList ){

        JSONObject foodCandidate = new JSONObject();

        FuzzyFoodQuerier FuzzyQuerier = new FuzzyFoodQuerier();
        for (int i = 0; i < foodNameList.size(); i++){
            JSONArray candidate = FuzzyQuerier.search(foodNameList.get(i));
            foodCandidate.append(foodNameList.get(i), candidate);

        }
        return foodCandidate;
    }
}
