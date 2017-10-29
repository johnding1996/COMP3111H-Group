package database.keeper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.connection.RedisPool;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.clients.jedis.Jedis;

import java.util.List;

import com.fasterxml.jackson.core.json.*;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class LogKeeperTester {
    private static Jedis jedis;
    private static LogKeeper logKeeper;
    private static JSONObject goodLogJson;
    private static JSONObject badLogJson;

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        logKeeper = new LogKeeper(jedis);
        goodLogJson = new JSONObject();
        goodLogJson.put("timestamp", "2017-10-29T13:30:52.123Z");
        goodLogJson.put("event", "recommendRequest");
        goodLogJson.put("old_state","Idle");
        goodLogJson.put("new_state", "ParseMenu");
        badLogJson = new JSONObject();
        badLogJson.put("timestamp", "2017-10-29T13:30:52.123Z");
        badLogJson.put("event", "recommendRequest");
        badLogJson.put("whatever","Idle");

    }

    @AfterClass
    public static void tearDownClass() {
        logKeeper.close();
    }

    @Test
    public void testSetSuccess() {
        jedis.del("log:0");
        logKeeper.set("0", goodLogJson);
        List<String> actual = jedis.lrange("log:0", 0, -1);
        jedis.del("log:0");
        assertTrue(actual.get(0).equals(goodLogJson.toString()));
    }

    @Test
    public void testSetFailure() {
        jedis.del("log:0");
        boolean result = logKeeper.set("0", badLogJson);
        jedis.del("log:0");
        assertTrue(!result);
    }

    @Test
    public void testGetSuccess() {
        jedis.del("log:0");
        logKeeper.set("0", goodLogJson);
        logKeeper.set("0", goodLogJson);
        logKeeper.set("0", goodLogJson);
        JSONArray actual = logKeeper.get("0", 5);
        jedis.del("log:0");
        JSONArray expected  = new JSONArray();
        expected.put(goodLogJson);
        expected.put(goodLogJson);
        expected.put(goodLogJson);
        assertTrue(expected.toString().equals(actual.toString()));
    }

    @Test
    public void testGetFailure() {
        jedis.del("log:0");
        JSONArray result = logKeeper.get("0", 5);
        jedis.del("log:0");
        assertNull(result);
    }
}
