package database.querier;

import database.connection.SQLPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class PartialFoodQuerierTester {
    private static Connection sql;
    private static PartialFoodQuerier partialFoodQuerier;
    private static JSONObject goodFoodJson;
    private static JSONObject badFoodJson;

    @BeforeClass
    public static void setUpClass() {
        sql = SQLPool.getConnection();
        partialFoodQuerier = new PartialFoodQuerier(sql);
        goodFoodJson = new JSONObject();
        goodFoodJson.put("ndb_no", 0);
        goodFoodJson.put("shrt_desc", "APPLE, BANANA");
        goodFoodJson.put("water", 12.34);
        goodFoodJson.put("energ_kcal", 1234);
        goodFoodJson.put("protein", 12.34);
        goodFoodJson.put("lipid_tot", 12.34);
        goodFoodJson.put("ash", 12.34);
        goodFoodJson.put("carbohydrt", 12.34);
        goodFoodJson.put("fiber_td", 123.4);
        goodFoodJson.put("sugar_tot", 12.34);
        goodFoodJson.put("calcium", 1234);
        goodFoodJson.put("iron", 12.34);
        goodFoodJson.put("magnesium", 1234);
        goodFoodJson.put("phosphorus", 1234);
        goodFoodJson.put("potassium", 1234);
        goodFoodJson.put("sodium", 1234);
        goodFoodJson.put("zinc", 12.34);
        goodFoodJson.put("copper", 1.234);
        goodFoodJson.put("manganese", 1.234);
        goodFoodJson.put("selenium", 123.4);
        goodFoodJson.put("vit_c", 123.4);
        goodFoodJson.put("thiamin", 1.234);
        goodFoodJson.put("riboflavin", 1.234);
        goodFoodJson.put("niacin", 1.234);
        goodFoodJson.put("panto_acid", 1.234);
        goodFoodJson.put("vit_b6", 1.234);
        goodFoodJson.put("floate_tot", 1234);
        goodFoodJson.put("folic_acid", 1234);
        goodFoodJson.put("food_floate", 1234);
        goodFoodJson.put("folate_dfe", 12.34);
        goodFoodJson.put("choline_tot", 12.34);
        goodFoodJson.put("vit_b12", 12.34);
        goodFoodJson.put("vit_a_iu", 1234);
        goodFoodJson.put("vit_a_rae", 1234);
        goodFoodJson.put("retinol", 1234);
        goodFoodJson.put("alpha_carot", 1234);
        goodFoodJson.put("beta_carot", 1234);
        goodFoodJson.put("beta_crtpy", 1234);
        goodFoodJson.put("lycopene", 1234);
        goodFoodJson.put("lut_zea", 1234);
        goodFoodJson.put("vit_e", 12.34);
        goodFoodJson.put("vit_d_mcg", 123.4);
        goodFoodJson.put("vit_d_iu", 1234);
        goodFoodJson.put("vit_k", 123.4);
        goodFoodJson.put("fa_sat", 1.234);
        goodFoodJson.put("fa_mono", 1.234);
        goodFoodJson.put("fa_poly", 1.234);
        goodFoodJson.put("cholestrl", 1.234);
        goodFoodJson.put("gmwt_1", 12.34);
        goodFoodJson.put("gmwt_desc1", "1 ust, (1/4\" gram)");
        goodFoodJson.put("gmwt_2", 12.34);
        goodFoodJson.put("gmwt_desc2", "1 hkust");
        goodFoodJson.put("rerfuse_pct", 12);
    }

    @AfterClass
    public static void tearDownClass() {
        partialFoodQuerier.close();
    }

    private void deleteRow() {
        try{
            PreparedStatement stmt = sql.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error(String.format("Failed to execute query: %s.", query), e);
        }
    }

    @Test
    public void testSetSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodFoodJson);
        boolean result = partialFoodQuerier.add(jsonArray);
        assertTrue(result);
    }


}
