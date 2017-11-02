package database.querier;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * Base Querier manipulate FoodNutrition table.
 * Base class for {@link PartialFoodQuerier} and {@link FuzzyFoodQuerier}.
 * @author mcding
 * @version 1.2
 */
@Slf4j
public class FoodQuerier extends Querier {

    /**
     * Default constructor.
     */
    FoodQuerier() {
        super();
        table = "foodnutrition";
        idx_field = "ndb_no";
        desc_field = "shrt_desc";
        fields = Arrays.asList(
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
        critical_fields = new HashSet<>(Arrays.asList(
                "ndb_no", "shrt_desc", "energ_kcal", "protein", "lipid_tot"
        ));
        queryLimit = 10;
    }

    /**
     * Constructor which uses external sql connection.
     * @param sql external sql connection
     */
    FoodQuerier(Connection sql) {
        this();
        this.sql = sql;
    }

    /**
     * Set the query limit.
     * @param queryLimit number of rows to return when searching
     */
    public FoodQuerier(int queryLimit) {
        this();
        this.queryLimit = queryLimit;
    }
}
