package agent;

import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.swabunga.spell.event.SpellChecker;

import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IntentionClassifier.class, JazzySpellChecker.class})
@ContextConfiguration(classes = {TestConfiguration.class, DatabaseMocker.class})
public class IntentionClassifierTester extends AgentTest {

    @Bean
    public SpellChecker sp() {
        return new SpellChecker();
    }

    @Autowired
    private IntentionClassifier intentionClassifier;

    @PostConstruct
    public void init() {
        agent = intentionClassifier;
        agentState = State.ASK_MEAL;
        userId = "813f61a35fbb9cc3adc28da525abf1fe";
    }

    @Test
    public void initTest() {
        intentionClassifier.init();
    }

    @Test
    public void parseIntentionTest() {
        ParserMessageJSON psr1 = new ParserMessageJSON(userId, "transition");
        psr1.set("textContent", "hahahaa")
           .setState(agentState.getName());
        intentionClassifier.parseIntention(psr1);

        ParserMessageJSON psr2 = new ParserMessageJSON(userId, "idle");
        psr2.set("textContent", "recommend")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr2);

        ParserMessageJSON psr3 = new ParserMessageJSON(userId, "idle");
        psr3.set("textContent", "personal")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr3);

        ParserMessageJSON psr4 = new ParserMessageJSON(userId, "idle");
        psr4.set("textContent", "digest")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr4);

        ParserMessageJSON psr5 = new ParserMessageJSON(userId, "idle");
        psr5.set("textContent", "frient")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr5);

        ParserMessageJSON psr6 = new ParserMessageJSON(userId, "idle");
        psr6.set("textContent", "code")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr6);

        ParserMessageJSON psr7 = new ParserMessageJSON(userId, "idle");
        psr7.set("textContent", "admin mode")
            .setState(agentState.getName());
        intentionClassifier.parseIntention(psr7);


    }




}
