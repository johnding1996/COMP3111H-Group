package database.querier;

import database.connection.SQLPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class FoodQuerierTester {
    private static Connection sql;
    private static Querier foodQuerier;
    private static JSONObject goodFoodJson;
    private static JSONObject badFoodJson;

    @BeforeClass
    public static void setUpClass() {
        Locale.setDefault(Locale.US);
        sql = SQLPool.getConnection();
        foodQuerier = new FoodQuerier();
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
        goodFoodJson.put("folate_tot", 1234);
        goodFoodJson.put("folic_acid", 1234);
        goodFoodJson.put("food_folate", 1234);
        goodFoodJson.put("folate_dfe", 1234);
        goodFoodJson.put("choline_tot", 1234);
        goodFoodJson.put("vit_b12", 12.34);
        goodFoodJson.put("vit_a_iu", 1234);
        goodFoodJson.put("vit_a_rae", 1234);
        goodFoodJson.put("retinol", 1234);
        goodFoodJson.put("alpha_carot", 1234);
        goodFoodJson.put("beta_carot", 1234);
        goodFoodJson.put("beta_crypt", 1234);
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
        goodFoodJson.put("refuse_pct", 12);
    }

    @AfterClass
    public static void tearDownClass() {
        foodQuerier.close();
    }

    private void deleteRow(int index) {
        try {
            String query = String.format("DELETE FROM foodnutrition WHERE ndb_no = '%d';", index);
            PreparedStatement stmt = sql.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error(String.format("Failed to delete row with index %d of ndb_no table when testing.", index), e);
        }
    }

    private boolean checkExistRow(int index) {
        try {
            String query = String.format("SELECT * FROM foodnutrition WHERE ndb_no = '%d';", index);
            PreparedStatement stmt = sql.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            boolean hasRow = rs.next();
            rs.close();
            stmt.close();
            return hasRow;
        } catch (SQLException e) {
            log.error(String.format("Failed to check whether row with index %d of foodnutrition table exists or not when testing.", index), e);
            return false;
        }
    }

    @Before
    public void setUp() {
        deleteRow(0);
    }

    @After
    public void tearDown() {
        deleteRow(0);
    }

    @Test
    public void testAddMissingCriticalFiledFailure() {
        JSONArray jsonArray = new JSONArray();
        badFoodJson = new JSONObject(goodFoodJson, JSONObject.getNames(goodFoodJson));
        badFoodJson.remove("energ_kcal");
        jsonArray.put(badFoodJson);
        boolean result = foodQuerier.add(jsonArray);
        boolean resultAnother = !checkExistRow(0);
        assertTrue(!result && resultAnother);
    }

    @Test
    public void testGetNumericalRoundingFailure() {
        JSONArray jsonArray = new JSONArray();
        badFoodJson = new JSONObject(goodFoodJson, JSONObject.getNames(goodFoodJson));
        badFoodJson.remove("energ_kcal");
        badFoodJson.put("energ_kcal", 1.234);
        jsonArray.put(badFoodJson);
        foodQuerier.add(jsonArray);
        JSONObject jsonObjectActual = foodQuerier.get(0);
        badFoodJson.remove("energ_kcal");
        badFoodJson.put("energ_kcal", 1);
        JSONAssert.assertEquals(badFoodJson, jsonObjectActual, false);

    }

    @Test
    public void testSearchSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodFoodJson);
        foodQuerier.add(jsonArray);
        JSONArray jsonArrayActual = foodQuerier.search("APPLE, BANANA");
        JSONAssert.assertEquals(jsonArray, jsonArrayActual, false);
    }

    @Test
    public void testSearchNotFoundFailure() {
        JSONArray jsonArrayActual = foodQuerier.search("whatever");
        JSONAssert.assertEquals(new JSONArray(), jsonArrayActual, false);
    }
}
