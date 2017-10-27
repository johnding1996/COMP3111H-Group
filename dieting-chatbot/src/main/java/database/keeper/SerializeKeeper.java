package database.keeper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.lang.NullPointerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * SerializeKeeper
 * Helper class which flatten a JSONObject to redis Hash, and handle the read/write operations.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public class SerializeKeeper {
    private static JedisPool pool;
    private Jedis jedis;
    static {
        try {
            URI uri = new URI(System.getenv("REDIS_URL"));
            pool = new JedisPool(new JedisPoolConfig(), uri);
            log.info("Redis pool initialized.");
        } catch (URISyntaxException e) {
            // If the URI is invalid
            pool = null;
            log.error("Invalid redis URI", e);
        }
    }

    SerializeKeeper() {

    }
}
