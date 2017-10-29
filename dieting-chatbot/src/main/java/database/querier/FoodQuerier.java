package database.querier;

import java.sql.Connection;
import java.sql.ResultSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;

/**
 * {@link FoodQuerier}
 * Abstract class which manipulate FoodNutrition table.
 * Super class for {@link PartialFoodQuerier} and {@link FuzzyFoodQuerier}.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public abstract class FoodQuerier extends Querier {
    /**
     * table
     * Table name for all FoodQueriers.
     */
    protected static String table = "foodnutrition";

    /**
     * queryLimit
     * The number of JSONObjects (rows) in query result.
     */
    protected int queryLimit = 10;

    /**
     * fields
     * List of all fields in the FoodNutrition table.
     */
    protected static final List<String> fields = Arrays.asList(
            "ndb_no", "shrt_desc", "water", "energ_kcal", "protein",
            "lipid_tot", "ash", "carbohydrt", "fiber_td", "sugar_tot", "calcium",
            "iron", "magnesium", "phosphorus", "potassium", "sodium", "zinc", "copper",
            "manganese", "selenium", "vit_c", "thiamin", "riboflavin", "niacin", "panto_acid",
            "vit_b6", "folate_tot", "folic_acid", "food_folate", "folate_dfe", "choline_tot",
            "vit_b12", "vit_a_iu", "vit_a_rae", "retinol", "alpha_carot", "beta_carot",
            "beta_crypt", "lycopene", "lut_zea", "vit_e", "vit_d_mcg", "vit_d_iu", "vit_k",
            "fa_sat", "fa_mono", "fa_poly", "cholestrl", "gmwt_1", "gmwt_desc1", "gmwt_2",
            "gmwt_desc2", "refuse_pct"
    );

    /**
     * critical_fields
     * Set of all critical fields that cannot be missing in the input FoodJSON JSONObject.
     */
    protected static final Set<String> critical_fields = new HashSet<>(Arrays.asList(
            "ndb_no", "shrt_desc", "water", "energ_kcal", "protein",
            "lipid_tot", "gmwt_1", "gmwt_desc1", "gmwt_2", "gmwt_desc2"
    ));

    /**
     * constructor
     * Default constructor.
     */
    FoodQuerier() {
        super();
    }

    /**
     * constructor
     * Set the query limit.
     * @param queryLimit number of rows to return when searching
     */
    FoodQuerier(int queryLimit) {
        super();
        this.queryLimit = queryLimit;
    }

    /**
     * add
     * Add a new food to database.
     * @param foodJsons JSONArray containing JSONObjects following the FoodJSON format
     * @return boolean whether add food successfully or not
     */
    @Override
    public boolean add(JSONArray foodJsons) {
        return insertData(table, fields, critical_fields, foodJsons);
    }

    /**
     * get
     * Exact search for a food (intended for internal use only).
     * @param key string to search
     * @return JSONArray an array of FoodJSON
     */
    @Override
    public JSONArray get(String key) {
        String query = String.format("SELECT * FROM %s WHERE shrt_desc = '%s' LIMIT %d;", table, key, queryLimit);
        ResultSet rs = executeQuery(query);
        return parseResult(rs, fields);
    }

    public boolean set(JSONArray foodJsons) {

    }

    /**
     * setQueryLimit
     * Change the query limit.
     * @param queryLimit number of rows to return when searching
     */
    public void setQueryLimit(int queryLimit) {
        this.queryLimit = queryLimit;
    }
}
