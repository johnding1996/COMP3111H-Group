package agent.FoodConfirm;
import java.util.List;

import database.querier.PartialFoodQuerier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * @author wguoaa
 * @version 1.2
 */
@Slf4j
public class GetFoodCandidatePartial extends GetFoodCandidate {

    /**
     * constructor
     * Default constructor.
     */
    public GetFoodCandidatePartial(JSONObject foodQuery) {
        super(foodQuery);
    }

    /**
     *getFoodCandidate
     * Query DB by partial match search and get food candidate
     * @param foodNameList
     * @return JSONObject an json file stores candidates for each dish
     * key: dish name input by user
     * values: JSONArray stores candidates for this dish
     */
    public JSONObject getFoodCandidate(List<String> foodNameList ){
        JSONObject foodCandidate = new JSONObject();
        PartialFoodQuerier PartialQuerier = new PartialFoodQuerier();
        for (int i = 0; i < foodNameList.size(); i++ ){
             JSONArray candidate = PartialQuerier.search(foodNameList.get(i));
             foodCandidate.append(foodNameList.get(i), candidate);

        }
        return foodCandidate;
    }
}
