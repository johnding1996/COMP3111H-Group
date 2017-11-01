package agent;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.stream.JsonParser;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author wguoaa
 * @version 1.2
 */
@Slf4j
public class getFoodCandidate {


    private JSONObject foodQueryData  = new JSONObject();

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
