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
public class MenuKeeperTester {
    private static Jedis jedis;
    private static MenuKeeper menuKeeper;
    private static JSONObject goodQueryJson;
    private static JSONObject badQueryJson;
    private static String user_id = "813f61a35fbb9cc3adc28da525abf1fe";

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        menuKeeper = new MenuKeeper(jedis);

        goodQueryJson = new JSONObject();
        goodQueryJson.put("userId", user_id);
        JSONArray menu = new JSONArray();
        JSONObject food1 = new JSONObject();
        food1.put("name", "Spicy Bean curd with Minced Pork served with Rice");
        food1.put("portionSize", 12.34);
        food1.put("protionUnit", "1 cup");
        JSONArray ingredients1 = new JSONArray();
        ingredients1.put("Butter");
        ingredients1.put("Bread");
        food1.put("ingredients", ingredients1);
        food1.put("price", 35);
        menu.put(food1);
        JSONObject food2 = new JSONObject();
        food2.put("name", "Sweet and Sour Pork served with Rice");
        food2.put("portionSize", 0.1234);
        food2.put("protionUnit", "1 unit");
        JSONArray ingredients2 = new JSONArray();
        ingredients2.put("John");
        ingredients2.put("Tomas");
        food2.put("ingredients", ingredients2);
        food2.put("price", 36);
        menu.put(food2);
        goodQueryJson.put("menu", menu);
        badQueryJson = new JSONObject(goodQueryJson.toString());
        badQueryJson.remove("userId");

    }

    @AfterClass
    public static void tearDownClass() {
        menuKeeper.close();
    }

    @Before
    public void setUp() {
        jedis.del("menu:" + user_id);
    }

    @After
    public void tearDown() {
        jedis.del("menu:" + user_id);
    }

    @Test
    public void testSetSuccess() {
        menuKeeper.set(user_id, goodQueryJson);
        List<String> actual = jedis.lrange("menu:" + user_id, 0, -1);
        JSONAssert.assertEquals(goodQueryJson, new JSONObject(actual.get(0)), false);
    }

    @Test
    public void testSetFailure() {
        boolean result = menuKeeper.set(user_id, badQueryJson);
        assertTrue(!result);
    }

    @Test
    public void testGetSuccess() {
        menuKeeper.set(user_id, goodQueryJson);
        menuKeeper.set(user_id, goodQueryJson);
        JSONArray actual = menuKeeper.get(user_id, 3);
        JSONArray expected  = new JSONArray();
        expected.put(goodQueryJson);
        expected.put(goodQueryJson);
        JSONAssert.assertEquals(expected, actual, false);
    }

    @Test
    public void testGetFailure() {
        JSONArray result = menuKeeper.get(user_id, 3);
        assertNull(result);
    }
}
