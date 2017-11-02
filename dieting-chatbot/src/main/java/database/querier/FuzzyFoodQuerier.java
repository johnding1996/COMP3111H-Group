package database.querier;

import org.json.JSONArray;

import java.sql.Connection;
import java.sql.ResultSet;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link FuzzyFoodQuerier}
 * Food querier which implement Levenshtein fuzzy serarch.
 */
@Slf4j
public class FuzzyFoodQuerier extends FoodQuerier {
    /**
     * levenshteinCosts
     * levenshtein algorithm parameters, {insert cost, delete cost, replace cost}
     */
    private int[] levenshteinCosts = {1, 10, 10};

    /**
     * constructor
     * Default constructor.
     */
    public FuzzyFoodQuerier() {
        super();
    }

    /**
     * constructor
     * Constructor which uses external sql connection.
     * @param sql external sql connection
     */
    FuzzyFoodQuerier(Connection sql) {
        super(sql);
    }

    /**
     * constructor
     * Set the query limit and levenshtein algorithm parameters.
     * @param queryLimit number of rows to return when searching
     * @param levenshteinCosts levenshtein algorithm parameters, {insert cost, delete cost, replace cost}
     */
    FuzzyFoodQuerier(int queryLimit, int[] levenshteinCosts) {
        super(queryLimit);
        System.arraycopy(levenshteinCosts, 0, this.levenshteinCosts, 0, 3);
    }

    /**
     * search
     * Fuzzy search for a food.
     * @param key string to search
     * @return JSONArray an array of FoodJSON
     */
    @Override
    public JSONArray search(String key) {
        String query = String.format("SELECT * FROM %s ORDER BY LEAST(LEVENSHTEIN('%s', %s, %d, %d, %d), LEVENSHTEIN('%s', %s, %d, %d, %d)) LIMIT %d;",
                table,
                key.toLowerCase(), desc_field, levenshteinCosts[0], levenshteinCosts[1], levenshteinCosts[2],
                key.toUpperCase(), desc_field, levenshteinCosts[0], levenshteinCosts[1], levenshteinCosts[2],
                queryLimit);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields, critical_fields);
    }
}
