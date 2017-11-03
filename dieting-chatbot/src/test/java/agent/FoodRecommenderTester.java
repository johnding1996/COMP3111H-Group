package agent;

import controller.ChatbotController;
import controller.FormatterMessageJSON;
import controller.ParserMessageJSON;
import controller.Publisher;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FoodRecommender.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class FoodRecommenderTester {
    @Autowired
    private FoodRecommender recommender;

    @Test
    public void testConstruct() {
        assert recommender != null;
    }

    @Test
    public void testGetUserJSON() {
        JSONObject userJSON = recommender.getUserJSON("813f61a35fbb9cc3adc28da525abf1fe");
        log.info(userJSON.toString(4));
    }
}