package database.connection;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
public class SQLPoolTester {
    private static Connection sql;

    @BeforeClass
    public static void setUpClass() {
        sql = SQLPool.getConnection();
    }

    @AfterClass
    public static void tearDownClass() {
        SQLPool.closeConnection(sql);
    }

    @Test
    public void testGetConnection() {
        assertNotNull(sql);
    }

    @Test
    public void testCheckConnection() {
        try {
            boolean result = sql.isValid(5);
            assertTrue(result);
        } catch (SQLException e) {
            assertTrue(false);
        }
    }
}