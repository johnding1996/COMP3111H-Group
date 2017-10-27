package database.querier;

import org.json.JSONArray;

import java.net.URISyntaxException;

public class PartialFoodQuerier extends Querier {

    private PartialFoodQuerier() throws URISyntaxException {
        super();
    }

    @Override
    public JSONArray get(String key) {
        return new JSONArray();
    }

    @Override
    public void set(String key, JSONArray a) {

    }

}
