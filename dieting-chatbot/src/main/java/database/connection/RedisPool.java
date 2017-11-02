package database.connection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

/**
 * Statically keep the Redis connection pool.
 * Provide wrapper method to generate new connection and close connection.
 * @author mcding
 * @version 1.2
 */
@Slf4j
public class RedisPool {
    private static JedisPool pool;

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

    /**
     * Return a new redis connection.
     * @return redis connection
     */
    public static Jedis getConnection() {
        try {
            return pool.getResource();
        } catch (NullPointerException e) {
            log.error("Redis pool has not been properly initialized.", e);
            return null;
        } catch (JedisConnectionException e) {
            log.error("Failed to establish redis connection.", e);
            return null;
        }
    }

    /**
     * Close a redis connection.
     * @param connection a redis connection
     */
    public static void closeConnection(Jedis connection) {
        connection.close();
    }
}
