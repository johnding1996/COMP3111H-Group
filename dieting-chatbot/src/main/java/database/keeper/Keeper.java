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

    /**
     * Check if the input user id is in the valid format
     * @param userId Input user id String
     * @return
     */
    public Boolean checkValidityUserId(String userId){
        if(userId.length() == 32){
            return true;
        }
        else{
            log.error("Invalid UserId Input");
            return false;
        }
    }

    /**
     * Check if the input code is in the valid format
     * @param code Input code String
     * @return
     */
    public Boolean checkValidityCode(String code){
        if(code.length() == 6){
            return true;
        }
        else{
            log.error("Invalid Code Input");
            return false;
        }
    }

}
