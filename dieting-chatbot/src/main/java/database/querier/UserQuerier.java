package database.querier;

import org.json.JSONArray;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;

/**
 * Querier which handles queries for User_Info table.
 */
@Slf4j
public class UserQuerier extends Querier {

    /**
     * Default constructor.
     */
    public UserQuerier(){
        super();
        table = "userinfo";
        idx_field = "id";
        desc_field = "id";
        fields = Arrays.asList(
                "id", "name", "age", "gender", "weight", "height", "goal_weight", "due_date"
        );
        critical_fields = new HashSet<>(Arrays.asList(
                "id", "age", "gender", "weight", "height"
        ));
        queryLimit = 1;
    }

    /**
     * Constructor which uses external sql connection.
     * @param sql external sql connection
     */
    UserQuerier(Connection sql) {
        this();
        this.sql = sql;
    }

}
