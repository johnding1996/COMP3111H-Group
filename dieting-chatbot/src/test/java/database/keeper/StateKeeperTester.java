package database.keeper;

import database.connection.RedisPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.clients.jedis.Jedis;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class StateKeeperTester {
    private static Jedis jedis;
    private static StateKeeper stateKeeper;

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        stateKeeper = new StateKeeper(jedis, 1);
    }

    @AfterClass
    public static void tearDownClass() {
        stateKeeper.close();
    }

    @Test
    public void testGetNotFound() {
        jedis.del("state:0");
        String state = stateKeeper.get(0);
        jedis.del("state:0");
        assertTrue(state.equals("Idle"));
    }

    @Test
    public void testSetSuccess() {
        jedis.del("state:0");
        boolean result = stateKeeper.set(0, "ParseMenu");
        String state = jedis.get("state:0");
        jedis.del("state:0");
        assertTrue(result && state.equals("ParseMenu"));
    }

    @Test
    public void testGetFound() {
        jedis.del("state:0");
        jedis.set("state:0", "Recommend");
        String state = stateKeeper.get(0);
        jedis.del("state:0");
        assertTrue(state.equals("Recommend"));
    }

    @Test
    public void testSetFailure() {
        boolean result = stateKeeper.set(0, "whatever");
        assertTrue(!result);
    }

    @Test
    public void testExpire() {
        jedis.del("state:0");
        stateKeeper.set(0, "AskMeal");
        try{
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            assertTrue(false);
        }
        String state = stateKeeper.get(0);
        jedis.del("state:0");
        assertTrue(state.equals("Idle"));
    }

}