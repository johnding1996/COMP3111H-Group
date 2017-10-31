package database.querier;

import database.connection.SQLPool;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class FuzzyFoodQuerierTester {
    private static Connection sql;
    private static Querier fuzzyFoodQuerier;
    private static JSONObject goodFoodJson;
    private static JSONObject badFoodJson;

    @BeforeClass
    public static void setUpClass() {
        sql = SQLPool.getConnection();
        fuzzyFoodQuerier = new FuzzyFoodQuerier(sql);
    }

    @AfterClass
    public static void tearDownClass() {
        fuzzyFoodQuerier.close();
    }

    public Set<Integer> retriveIndices(JSONArray jsonArray) {
        Set<Integer> indices = new HashSet<>();
        for (int i=0; i<jsonArray.length(); i++) {
            JSONObject jsonObject;
            try {
                jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                log.error("Parsing JSONArray failed when get JSONObject in FuzzyFoodQuerierTester.", e);
                return null;
            }
            indices.add(jsonObject.getInt("ndb_no"));
        }
        return indices;
    }

    // Note that this unittest require the foodnutrition table to be exactly the original SR28 release.
    // Running these tests on other modified foodnutrition table may fail.

    @Test
    public void testSearchSuccess() {
        JSONArray jsonArray = fuzzyFoodQuerier.search("BEEF STEAK GRILLED");
        Set<Integer> indicesExpect = new HashSet<>(Arrays.asList(
                13946, 23076, 23268, 23516, 13469, 23536, 23075, 13493, 23267, 13232
        ));
        Set<Integer> indicesActual = retriveIndices(jsonArray);
        log.info("length: " + jsonArray.length());
        log.info("actual: " + indicesActual);
        log.info("expect: " + indicesExpect);
        assertTrue(indicesExpect.equals(indicesActual));
    }

    @Test
    public void testSearchCaseInsensitiveSuccess() {
        JSONArray jsonArray = fuzzyFoodQuerier.search("Beef Steak Grilled");
        Set<Integer> indicesExpect = new HashSet<>(Arrays.asList(
                13946, 23076, 23268, 23516, 13469, 23536, 23075, 13493, 23267, 13232
        ));
        Set<Integer> indicesActual = retriveIndices(jsonArray);
        assertTrue(indicesExpect.equals(indicesActual));
    }
}
