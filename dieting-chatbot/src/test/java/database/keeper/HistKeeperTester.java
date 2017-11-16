package database.keeper;

import database.connection.RedisPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;
import redis.clients.jedis.Jedis;

import java.util.List;

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
    //private static String user_id = "813f61a35fbb9cc3adc28da525abf1fe";
    private static String user_id = "111111a35fbb9cc3adc28da525abf1fe";

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

    @Before
    public void setUp() {
        jedis.del("hist:" + user_id);
    }

    @After
    public void tearDown() {
        jedis.del("hist:" + user_id);
    }

    @Test
    public void testSetSuccess() {
        histKeeper.set(user_id, goodHistJson);
        List<String> actual = jedis.lrange("hist:" + user_id, 0, -1);
        JSONAssert.assertEquals(goodHistJson, new JSONObject(actual.get(0)), false);
    }

    @Test
    public void testSetFailure() {
        boolean result = histKeeper.set(user_id, badHistJson);
        assertTrue(!result);
    }

    @Test
    public void testGetSuccess() {
        histKeeper.set(user_id, goodHistJson);
        histKeeper.set(user_id, goodHistJson);
        JSONArray actual = histKeeper.get(user_id, 5);
        JSONArray expected  = new JSONArray();
        expected.put(goodHistJson);
        expected.put(goodHistJson);
        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testGetFailure() {
        JSONArray result = histKeeper.get(user_id, 5);
        assertNull(result);
    }
}
