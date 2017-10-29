package database.keeper;

import redis.clients.jedis.Jedis;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class StateKeeperTester {
    @Test
    public void testGetNull() {
        StateKeeper stateKeeper = new StateKeeper();
        String state = stateKeeper.get(0);
        assertTrue(state.equals("Idle"));
    }
}