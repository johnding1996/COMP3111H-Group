package database.keeper;

import database.connection.RedisPool;

import redis.clients.jedis.Jedis;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class of all Keepers.
 * @author mcding
 * @version 1.2.1
 */
@Slf4j
public abstract class Keeper {
    /**
     * Jedis connection for this Keeper.
     */
    protected Jedis jedis;

    /**
     * Connect to redis server and create the instance's jedis instance.
     */
    Keeper() {
        jedis = RedisPool.getConnection();
    }

    /**
     * Close the connection once it is not used anymore.
     */
    public void close() {
        RedisPool.closeConnection(jedis);
    }

}
