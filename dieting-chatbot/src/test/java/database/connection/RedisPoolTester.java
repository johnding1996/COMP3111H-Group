package database.connection;

import redis.clients.jedis.Jedis;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class RedisPoolTester {
    @Test
    public void testGetConnection() {
        Jedis jedis = RedisPool.getConnection();
        assertNotNull(jedis);
    }
}