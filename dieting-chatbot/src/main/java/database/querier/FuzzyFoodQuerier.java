package database.querier;

import org.json.JSONArray;

import java.sql.ResultSet;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link FuzzyFoodQuerier}
 * Food querier which implement Levenshtein fuzzy serarch.
 */
@Slf4j
public class FuzzyFoodQuerier extends FoodQuerier {
    /**
     * levenshteinParams
     * levenshtein algorithm parameters, {insert cost, delete cost, replace cost}
     */
    private int[] levenshteinParams = {1, 10, 10};

    /**
     * constructor
     * Set the query limit and levenshtein algorithm parameters.
     * @param queryLimit number of rows to return when searching
     * @param levenshteinParams levenshtein algorithm parameters, {insert cost, delete cost, replace cost}
     */
    FuzzyFoodQuerier(int queryLimit, int[] levenshteinParams) {
        super(queryLimit);
        System.arraycopy(levenshteinParams, 0, this.levenshteinParams, 0, 3);
    }

    /**
     * get
     * Fuzzy search for a food.
     * @param key string to search
     * @return JSONArray an array of FoodJSON
     */
    @Override
    public JSONArray get(String key) {
        String query = String.format("SELECT *, levenshtein(shrt_desc, '%s', %d, %d, %d) as dist FROM %s ORDER BY dist LIMIT %d;",
                key, levenshteinParams[0], levenshteinParams[1], levenshteinParams[2], table, queryLimit);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields);
    }
}
