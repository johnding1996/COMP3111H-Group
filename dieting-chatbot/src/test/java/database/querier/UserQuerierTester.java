package database.querier;

import database.connection.SQLPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Locale;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class UserQuerierTester {
    private static Connection sql;
    private static String user_id1 = "4b5f62d023bcfc773968a2d1e80d0a14";
    private static String user_id2 = "9cc5f0eb38acb25dc4a5e924c7ca3d74";
    private static Querier userQuerier;
    private static JSONObject goodUserJson;
    private static JSONObject anotherGoodUserJson;
    private static JSONObject badUserJson;

    @BeforeClass
    public static void setUpClass() {
        Locale.setDefault(Locale.US);
        sql = SQLPool.getConnection();
        userQuerier = new UserQuerier(sql);
        goodUserJson = new JSONObject();
        goodUserJson.put("id", user_id1);
        goodUserJson.put("name", "john");
        goodUserJson.put("age", 20);
        goodUserJson.put("gender", "male");
        goodUserJson.put("weight", 67.5);
        goodUserJson.put("height", 178.5);
        goodUserJson.put("goal_weight", 65.0);
        goodUserJson.put("due_date", "2017-10-29");
    }

    @AfterClass
    public static void tearDownClass() {
        userQuerier.close();
    }

    private void deleteRow(String index) {
        try {
            String query = String.format("DELETE FROM userinfo WHERE id = '%s';", index);
            PreparedStatement stmt = sql.prepareStatement(query);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error(String.format("Failed to delete row with index %s of userinfo table when testing.", index), e);
        }
    }

    private boolean checkExistRow(String index) {
        try {
            String query = String.format("SELECT * FROM userinfo WHERE id = '%s';", index);
            PreparedStatement stmt = sql.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            boolean hasRow = rs.next();
            rs.close();
            stmt.close();
            return hasRow;
        } catch (SQLException e) {
            log.error(String.format("Failed to check whether row with index %s of userinfo table exists or not when testing.", index), e);
            return false;
        }
    }

    @Before
    public void setUp() {
        deleteRow(user_id1);
        deleteRow(user_id2);
    }

    @After
    public void tearDown() {
        deleteRow(user_id1);
        deleteRow(user_id2);
    }

    @Test
    public void testAddSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        anotherGoodUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        anotherGoodUserJson.remove("id");
        anotherGoodUserJson.put("id", user_id2);
        jsonArray.put(anotherGoodUserJson);
        boolean result = userQuerier.add(jsonArray);
        boolean resultAnother0 = checkExistRow(user_id1);
        boolean resultAnother1 = checkExistRow(user_id2);
        assertTrue(result && resultAnother0 && resultAnother1);
    }

    @Test
    public void testAddEmptyJSONArrayFailure() {
        JSONArray jsonArray = new JSONArray();
        boolean result = userQuerier.add(jsonArray);
        assertTrue(!result);
    }

    @Test
    public void testAddInvalidJSONArrayFailure() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put("whatever");
        boolean result = userQuerier.add(jsonArray);
        boolean resultAnother = !checkExistRow(user_id1);
        assertTrue(!result && resultAnother);
    }

    @Test
    public void testAddMissingCriticalFiledFailure() {
        JSONArray jsonArray = new JSONArray();
        badUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        badUserJson.remove("age");
        jsonArray.put(badUserJson);
        boolean result = userQuerier.add(jsonArray);
        boolean resultAnother = !checkExistRow(user_id1);
        assertTrue(!result && resultAnother);
    }

    @Test
    public void testAddNotSupportedJSONTypeFailure() {
        JSONArray jsonArray = new JSONArray();
        badUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        badUserJson.remove("due_date");
        badUserJson.put("due_date", new Date());
        jsonArray.put(badUserJson);
        boolean result = userQuerier.add(jsonArray);
        boolean resultAnother = !checkExistRow(user_id1);
        assertTrue(!result && resultAnother);
    }

    @Test
    public void testAddNotSupportedSQLTypeFailure() {
        JSONArray jsonArray = new JSONArray();
        badUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        badUserJson.remove("age");
        badUserJson.put("age", true);
        jsonArray.put(badUserJson);
        boolean result = userQuerier.add(jsonArray);
        boolean resultAnother = !checkExistRow(user_id1);
        assertTrue(!result && resultAnother);
    }

    @Test
    public void testAddDuplicatedIndexFailure() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        jsonArray.put(goodUserJson);
        boolean result = userQuerier.add(jsonArray);
        // Non of the rows with duplicated index will be added to the table
        boolean resultAnother1 = !checkExistRow(user_id1);
        boolean resultAnother2 = !checkExistRow(user_id2);
        assertTrue(!result && resultAnother1 && resultAnother2);
    }

    @Test
    public void testDeleteSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        userQuerier.add(jsonArray);
        boolean result = userQuerier.delete(user_id1);
        boolean resultAnother = !checkExistRow(user_id2);
        assertTrue(result && resultAnother);
    }

    @Test
    public void testDeleteNotFoundSuccess() {
        boolean result = userQuerier.delete(user_id1);
        assertTrue(result);
    }

    @Test
    public void testGetSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        userQuerier.add(jsonArray);
        JSONObject jsonObjectActual = userQuerier.get(user_id1);
        JSONAssert.assertEquals(goodUserJson, jsonObjectActual, false);
    }

    @Test
    public void testGetAllSuccess() { ;
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        anotherGoodUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        anotherGoodUserJson.remove("id");
        anotherGoodUserJson.put("id", user_id2);
        jsonArray.put(anotherGoodUserJson);
        userQuerier.add(jsonArray);
        JSONArray jsonArrayActual = userQuerier.get();
        JSONAssert.assertEquals(jsonArray, jsonArrayActual, false);
    }

    @Test
    public void testUpdateSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        userQuerier.add(jsonArray);
        anotherGoodUserJson = new JSONObject(goodUserJson, JSONObject.getNames(goodUserJson));
        anotherGoodUserJson.remove("name");
        anotherGoodUserJson.put("name", "thomas");
        JSONArray anotherJsonArray = new JSONArray();
        anotherJsonArray.put(anotherGoodUserJson);
        boolean result = userQuerier.update(anotherJsonArray);
        JSONObject jsonObjectActual = userQuerier.get(user_id1);
        JSONAssert.assertEquals(anotherGoodUserJson, jsonObjectActual, false);
    }

    @Test
    public void testUpdateNotFoundSuccess() {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(goodUserJson);
        boolean result = userQuerier.update(jsonArray);
        boolean resultAnother = !checkExistRow(user_id1);
        assertTrue(result && resultAnother);
    }
}
