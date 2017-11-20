package agent;

import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InitialInputRecorder.class, JazzySpellChecker.class, UserManager.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class InitialInputRecorderTest extends AgentTest {

    @Autowired
    private InitialInputRecorder recorder;

    @PostConstruct
    public void init() {
        agent = recorder;
        agentState = State.INITIAL_INPUT;
        userId = "szhouan";
    }

    @Test
    public void testValidateInput1() {
        assert recorder.validateInput(1, "20");
        assert recorder.validateInput(2, "male");
        assert recorder.validateInput(3, "55");
        assert recorder.validateInput(4, "60");
        assert recorder.validateInput(5, "180");
        assert recorder.validateInput(6, "3000-4-1");
    }

    @Test
    public void testValidateInput2() {
        assert !recorder.validateInput(1, "-20");
        assert !recorder.validateInput(2, "lema");
        assert !recorder.validateInput(3, "100000");
        assert !recorder.validateInput(4, "0.60");
        assert !recorder.validateInput(5, "3.14");
        assert !recorder.validateInput(6, "3-4-1");
        assert !recorder.validateInput(1000, "foobar");
    }

    @Test
    public void testTransition() {
        recorder.registerUser(userId);
        checkHandler("", "Would you please", 0, 1);
        checkHandler("Hey", "", 1, 1);
        checkHandler("20", "Tell me your", 1, 2);
        checkHandler("What?", "", 2, 2);
        checkHandler("maale", "Hey, what is", 2, 3);
        checkHandler("Foo", "", 3, 3);
        checkHandler("60", "How about your", 3, 4);
        checkHandler("Bar", "", 4, 4);
        checkHandler("180", "Emmm...", 4, 5);
        checkHandler("Baz", "", 5, 5);
        checkHandler("65", "Alright, now tell", 5, 6);
        checkHandler("Rah", "", 6, 6);
        checkHandler("2020-12-31", "Great! I now", 6, Agent.END_STATE);
        assert controller.getUserState(userId) == State.IDLE;
    }
}