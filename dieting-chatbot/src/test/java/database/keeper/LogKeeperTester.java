package database.keeper;

import database.connection.RedisPool;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;

import redis.clients.jedis.Jedis;

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
    private static String user_id = "813f61a35fbb9cc3adc28da525abf1fe";

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

    @Before
    public void setUp() {
        jedis.del("log:" + user_id);
    }

    @After
    public void tearDown() {
        jedis.del("log:" + user_id);
    }

    @Test
    public void testSetSuccess() {
        logKeeper.set(user_id, goodLogJson);
        List<String> actual = jedis.lrange("log:" + user_id, 0, -1);
        JSONAssert.assertEquals(goodLogJson, new JSONObject(actual.get(0)), false);
    }

    @Test
    public void testSetFailure() {
        boolean result = logKeeper.set(user_id, badLogJson);
        assertTrue(!result);
    }

    @Test
    public void testGetSuccess() {
        logKeeper.set(user_id, goodLogJson);
        logKeeper.set(user_id, goodLogJson);
        logKeeper.set(user_id, goodLogJson);
        JSONArray actual = logKeeper.get(user_id, 5);
        JSONArray expected  = new JSONArray();
        expected.put(goodLogJson);
        expected.put(goodLogJson);
        expected.put(goodLogJson);
        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testGetFailure() {
        JSONArray result = logKeeper.get(user_id, 5);
        assertNull(result);
    }
}
