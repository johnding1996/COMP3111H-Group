package database.connection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import redis.clients.jedis.Jedis;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class RedisPoolTester {
    private static Jedis jedis;

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
    }

    @AfterClass
    public static void tearDownClass() {
        RedisPool.closeConnection(jedis);
    }

    @Test
    public void testGetConnection() {
        assertNotNull(jedis);
    }

    @Test
    public void testCheckConnection() {
        String result = jedis.ping();
        assertTrue(result.equals("PONG"));
    }
}