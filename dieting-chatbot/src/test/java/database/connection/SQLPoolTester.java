package database.connection;

import java.sql.Connection;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class SQLPoolTester {
    @Test
    public void testGetConnection() {
        Connection sql = SQLPool.getConnection();
        assertTrue(true);
    }
}