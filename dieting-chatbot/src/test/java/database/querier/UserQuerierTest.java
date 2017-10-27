package database.querier;

import database.querier.Querier;
import database.querier.UserQuerier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URISyntaxException;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Querier.class, String.class })
public class UserQuerierTest {
    private Querier querier;

    UserQuerierTest() throws URISyntaxException {
        querier = new UserQuerier();
    }

    @Test
    public void testRedisConnection() throws Exception {

    }

}
