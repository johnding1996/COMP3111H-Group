package agent;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.stream.JsonParser;

import database.querier.PartialFoodQuerier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author wguoaa
 * @version 1.2
 */
@Slf4j
public class getFoodCandidatePartial extends getFoodCandidate  {


    public JSONObject getFoodCandidate(ArrayList<String> foodNameList ){
        JSONObject foodCandidate = new JSONObject();
        PartialFoodQuerier PartialQuerier = new PartialFoodQuerier();
        for (int i = 0; i < foodNameList.size(); i++ ){
             JSONArray candidate = PartialQuerier.search(foodNameList.get(i));
             foodCandidate.append(foodNameList.get(i), candidate);

        }
        return foodCandidate;
    }
}
