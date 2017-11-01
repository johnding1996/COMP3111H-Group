package agent.InteractFoodConfirm;
import java.util.ArrayList;

import agent.InteractFoodConfirm.GetFoodCandidate;
import database.querier.FuzzyFoodQuerier;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author wguoaa
 * @version 1.2
 */

@Slf4j
public class GetFoodCandidateFuzzy extends GetFoodCandidate {

    /**
     * constructor
     * Default constructor.
     */
    public GetFoodCandidateFuzzy() {
        super();
    }

    /**
     *getFoodCandidate
     * Query DB by fuzzy search and get food candidate
     * @param foodNameList
     * @return JSONObject an json file stores candidates for each dish
     * key: dish name input by user
     * values: JSONArray stores candidates for this dish
     */
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
