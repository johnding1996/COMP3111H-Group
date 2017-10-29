package database.keeper;

import database.connection.RedisPool;

import redis.clients.jedis.Jedis;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link Keeper}
 * Abstract super class of all Keepers.
 * @author mcding
 * @version 1.1
 */
@Slf4j
public abstract class Keeper {
    protected Jedis jedis;

    /**
     * constructor
     * Connect to redis server and create the instance's jedis instance.
     */
    Keeper() {
        jedis = RedisPool.getConnection();
    }

    /**
     * close
     * Close the connection once it is not used anymore.
     */
    protected void close() {
        RedisPool.closeConnection(jedis);
    }

    /**
     * finalize
     * Override the original finalizer to check whether the connection is closed.
     */
    @Override
    public void finalize() throws java.lang.Throwable {
        if (jedis.isConnected()) {
            log.error("Redis connection is not closed when destroying the Keeper class.");
        }
        super.finalize();
    }
}
