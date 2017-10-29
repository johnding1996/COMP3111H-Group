package database.querier;

import org.json.JSONArray;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link UserQuerier}
 * Querier which handles queries for User_Info table.
 */
@Slf4j
public class UserQuerier extends Querier {
    /**
     * table
     * Table name for UserQuerier.
     */
    private static final String table = "user_info";

    /**
     * fields
     * List of all fields in the User_Info table.
     */
    private static final List<String> fields = Arrays.asList(
            "user_id", "age", "gender", "weight", "height", "goal_weight", "due_date"
    );

    /**
     * critical_fields
     * Set of all critical fields that cannot be missing in the input UserJSON JSONObject.
     */
    private static final Set<String> critical_fields = new HashSet<String>(Arrays.asList(
            "user_id", "age", "gender", "weight", "height"
    ));

    /**
     * constructor
     */
    UserQuerier(){
        super();
    }

    /**
     * get
     * Get a user info according to the user_id string.
     * @param key user_id to search for
     * @return JSONArray
     */
    @Override
    public JSONArray get(String key) {
        String query;
        // If null user_id string, return all students
        if (key == null) {
            query = String.format("SELECT * FROM %s", table, key);
        } else {
            query = String.format("SELECT * FROM %s WHERE user_id = '%s' LIMIT %d;", table, key, 1);
        }
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields);
    }

    /**
     * add
     * Add a user info into the User_Info table.
     * @param userJsons JSONArray containing JSONObjects following the UserJSON format
     * @return whether add successfully or not
     */
    @Override
    public boolean add(JSONArray userJsons) {
        return insertData(table, fields, critical_fields, userJsons);
    }
}
