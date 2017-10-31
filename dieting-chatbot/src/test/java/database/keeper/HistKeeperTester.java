package database.keeper;

import database.connection.RedisPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.skyscreamer.jsonassert.JSONAssert;
import redis.clients.jedis.Jedis;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class HistKeeperTester {
    private static Jedis jedis;
    private static HistKeeper histKeeper;
    private static JSONObject goodHistJson;
    private static JSONObject badHistJson;

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        histKeeper = new HistKeeper(jedis);
        goodHistJson = new JSONObject();
        goodHistJson.put("date", "2017-10-29");
        goodHistJson.put("number_of_meal", 2);
        JSONArray food = new JSONArray();
        food.put(0);
        food.put(1);
        goodHistJson.put("food", food);
        goodHistJson.put("time_created", "2017-10-29T13:30:52.123Z");
        badHistJson = new JSONObject();
        badHistJson.put("date", "2017-10-29");
        badHistJson.put("number_of_meal", 2);
        badHistJson.put("time_created", "2017-10-29T13:30:52.123Z");

    }

    @AfterClass
    public static void tearDownClass() {
        histKeeper.close();
    }

    @Test
    public void testSetSuccess() {
        jedis.del("hist:0");
        histKeeper.set("0", goodHistJson);
        List<String> actual = jedis.lrange("hist:0", 0, -1);
        jedis.del("hist:0");
        JSONAssert.assertEquals(goodHistJson, new JSONObject(actual.get(0)), false);
    }

    @Test
    public void testSetFailure() {
        jedis.del("hist:0");
        boolean result = histKeeper.set("0", badHistJson);
        jedis.del("hist:0");
        assertTrue(!result);
    }

    @Test
    public void testGetSuccess() {
        jedis.del("hist:0");
        histKeeper.set("0", goodHistJson);
        histKeeper.set("0", goodHistJson);
        histKeeper.set("0", goodHistJson);
        JSONArray actual = histKeeper.get("0", 5);
        jedis.del("hist:0");
        JSONArray expected  = new JSONArray();
        expected.put(goodHistJson);
        expected.put(goodHistJson);
        expected.put(goodHistJson);
        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testGetFailure() {
        jedis.del("hist:0");
        JSONArray result = histKeeper.get("0", 5);
        jedis.del("hist:0");
        assertNull(result);
    }
}
