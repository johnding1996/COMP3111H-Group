package database.keeper;

import database.connection.RedisPool;
import org.junit.*;
import redis.clients.jedis.Jedis;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class StateKeeperTester {
    private static Jedis jedis;
    private static StateKeeper stateKeeper;
    private static String user_id = "813f61a35fbb9cc3adc28da525abf1fe";

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        stateKeeper = new StateKeeper(jedis, 1);
    }

    @AfterClass
    public static void tearDownClass() {
        stateKeeper.close();
    }

    @Before
    public void setUp() {
        jedis.del("state:" + user_id);
    }

    @After
    public void tearDown() {
        jedis.del("state:" + user_id);
    }


    @Test
    public void testGetNotFound() {
        String state = stateKeeper.get(user_id);
        assertTrue(state.equals("Idle"));
    }

    @Test
    public void testSetSuccess() {
        boolean result = stateKeeper.set(user_id, "ParseMenu");
        String state = jedis.get("state:" + user_id);
        assertTrue(result && state.equals("ParseMenu"));
    }

    @Test
    public void testGetFound() {
        jedis.set("state:" + user_id, "Recommend");
        String state = stateKeeper.get(user_id);
        assertTrue(state.equals("Recommend"));
    }

    @Test
    public void testSetFailure() {
        boolean result = stateKeeper.set(user_id, "whatever");
        assertTrue(!result);
    }

    @Test
    public void testExpire() {
        stateKeeper.set(user_id, "AskMeal");
        try{
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            assertTrue(false);
        }
        String state = stateKeeper.get(user_id);
        assertTrue(state.equals("Idle"));
    }

}