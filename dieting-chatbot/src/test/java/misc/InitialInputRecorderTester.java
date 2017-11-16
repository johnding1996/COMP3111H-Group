package misc;

import controller.ChatbotController;
import controller.Publisher;
import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.bus.Event;
import utility.FormatterMessageJSON;
import utility.JazzySpellChecker;
import utility.ParserMessageJSON;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InitialInputRecorder.class, JazzySpellChecker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class InitialInputRecorderTester {
    @Autowired
    private InitialInputRecorder recorder;

    @Autowired
    private Publisher publisher;

    @Autowired
    private ChatbotController controller;

    @Test
    public void testConstruction() {
        assert recorder != null;
        assert publisher != null;
    }

    @Test
    public void testValidateInput1() {
        assert InitialInputRecorder.validateInput("age", "20");
        assert InitialInputRecorder.validateInput("gender", "male");
        assert InitialInputRecorder.validateInput("weight", "55");
        assert InitialInputRecorder.validateInput("desiredWeight", "60");
        assert InitialInputRecorder.validateInput("height", "180");
        assert InitialInputRecorder.validateInput("goalDate", "3000-4-1");
    }

    @Test
    public void testValidateInput2() {
        assert !InitialInputRecorder.validateInput("age", "-20");
        assert !InitialInputRecorder.validateInput("gender", "lema");
        assert !InitialInputRecorder.validateInput("weight", "100000");
        assert !InitialInputRecorder.validateInput("desiredWeight", "0.60");
        assert !InitialInputRecorder.validateInput("height", "3.14");
        assert !InitialInputRecorder.validateInput("goalDate", "3-4-1");
        assert !InitialInputRecorder.validateInput("invalidField", "foobar");

    }

    @Test
    public void testAccept1() {
        recorder.clearUserStates();
        Event<ParserMessageJSON> ev =
            getParserMessageJSONEvent("szhouan", "Hello");
        Mockito.doAnswer(getFormatterMessageJSONAnswerObject("Would"))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recorder.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testAccept2() {
        String userId = "szhouan";
        recorder.clearUserStates();
        addUser(userId);
        assert recorder.getUserState(userId).equals("age");

        checkStateTransition(userId, "Hey", "Please input", "20",
            "Tell me your", "age", "gender");
        checkStateTransition(userId, "What?", "Please input", "maale",
            "Hey, what is", "gender", "weight");
        checkStateTransition(userId, "Foo", "Please input", "60",
            "How about the", "weight", "height");
        checkStateTransition(userId, "Bar", "Please input", "180",
            "Emmm...", "height", "desiredWeight");
        checkStateTransition(userId, "Baz", "Please input", "65",
            "Alright, now tell", "desiredWeight", "goalDate");
        checkStateTransition(userId, "Rah", "Please input", "2020-12-31",
            "Great! I now", "goalDate", null);
    }

    /**
     * Wrapper for tracking internal state transition
     */
    private void checkStateTransition(String userId, String invalidInput,
        String invalidPrefix, String validInput, String validPrefix,
        String currentState, String nextState) {
        
        Event<ParserMessageJSON> ev;
        ev = getParserMessageJSONEvent(userId, invalidInput); // invalid
        Mockito.doAnswer(getFormatterMessageJSONAnswerObject(invalidPrefix))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recorder.accept(ev);
        assert recorder.getUserState(userId).equals(currentState);

        ev = getParserMessageJSONEvent(userId, validInput); // valid
        Mockito.doAnswer(getFormatterMessageJSONAnswerObject(validPrefix))
            .when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        recorder.accept(ev);
        if (nextState != null)
            assert recorder.getUserState(userId).equals(nextState);
        Mockito.reset(publisher);
    }

    /**
     * Add user to internal state of InitialInputRecorder
     * @param userId String of user Id
     */
    private void addUser(String userId) {
        controller.setUserState(userId, State.INITIAL_INPUT);
        Event<ParserMessageJSON> ev =
            getParserMessageJSONEvent(userId, "hello");
        recorder.accept(ev);
    }

    /**
     * Publish ParserMessageJSON with given userId and text
     * @param userId String of user Id
     * @param text Text string to publish
     * @return An Event object
     */
    private Event<ParserMessageJSON> getParserMessageJSONEvent(
        String userId, String text) {
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.set("textContent", text)
           .setState("InitialInput");
        return new Event<>(null, psr);
    }

    /**
     * Return an Answer<void> object for assertion
     * 
     * Also check transition if presents
     * @param prefix Expected prefix of the message
     * @return An Answer<void> object that check the prefix is correct
     */
    private Answer<Void> getFormatterMessageJSONAnswerObject(String prefix) {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                    FormatterMessageJSON.class);
                JSONArray messages = fmt.getMessageArray();
                if (messages.length()==0) return null;
                String text = messages.getJSONObject(0)
                    .getString("textContent");
                if (text.startsWith("CORRECTED")) {
                    text = messages.getJSONObject(1)
                        .getString("textContent");
                }
                log.info("Reply Message: {}", text);
                assert text.startsWith(prefix);
                return null;
            }
        };
    }
}