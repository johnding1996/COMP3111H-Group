package agent;

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
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

import java.util.HashMap;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FoodRecommender.class, PortionAsker.class})
@ContextConfiguration(classes = TestConfiguration.class)
public class PortionAskerTester {

    @Autowired
    private PortionAsker asker;

    @Autowired
    private Publisher publisher;

    //Used to save mocked user menu json record
    private static HashMap<String, JSONObject> userMenuJSON = new HashMap<>();

    /**
     * This method would initialize userMenuJSON with a record QueryJSON
     * with the user as "cluibf", and all dishes named with "xiaoxigua"
     * @param numOfDish, number of "xiaoxigua" & "laoxigua" in menu
     */
    private void setMockDatabase(int numOfDish){
        this.userMenuJSON.clear();

        JSONObject queryJSON = new JSONObject();
        JSONArray menu = new JSONArray();

        for(int i = 0; i < numOfDish; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "xiaoxigua");
            menu.put(dish);
        }

        for(int i = 0; i < numOfDish; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "laoxigua");
            menu.put(dish);
        }

        queryJSON.put("userId", "cliubf");
        queryJSON.put("menu", menu);
        this.userMenuJSON.put("cliubf", queryJSON);
    }

    /**
     * Constructor for test class, initialized asker with a mocked one
     */
    public PortionAskerTester(){
        PortionAsker asker = Mockito.spy(PortionAsker.class);
        Mockito.doAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                return userMenuJSON.get(userId);
            }
        }).when(asker).getMenuKeeperJSON(Matchers.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                JSONObject newMenuJSON = invocation.getArgumentAt(1, JSONObject.class);
                userMenuJSON.put(userId, newMenuJSON);
                return null;
            }
        }).when(asker).setMenuKeeperJSON(Matchers.anyString(),
                Matchers.any(JSONObject.class));

        this.asker = asker;
    }

    @Test
    public void testAcceptImageMessage() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
        psr.setState("AskPortion");
        asker.changeUserState(psr.get("userId"), 0);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                if (messages.length() == 0) return null;
                String text = (String) messages.getJSONObject(0)
                        .get("textContent");
                assert text.startsWith("I am sorry that");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testAnotherState() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("RecordMeal")
                .set("textContent", "hello");
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                assert false;
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
        Mockito.reset(publisher);
    }

    @Test
    public void testState0() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "hello");
        asker.changeUserState(psr.get("userId"), 0);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Okay, this is");
                String text2 = (String) messages.getJSONObject(1).get("textContent");
                assert text2.startsWith("1 - xiaoxigua");
                String text3 = (String) messages.getJSONObject(2).get("textContent");
                assert text3.startsWith("Would you like to update");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState1a() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "Yes");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Okay, so give me");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState1b() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "No");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Alright, let's move on");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState1c() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "skdhfkjdsfh");
        asker.changeUserState(psr.get("userId"), 1);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Sorry, I'm not sure");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState2a() {
        String userId = "cliubf";
        this.setMockDatabase(1);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "leave");
        asker.changeUserState(psr.get("userId"), 2);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Alright, we are going to process your update");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState2b1() {
        String userId = "cliubf";
        this.setMockDatabase(10);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "100:100");
        asker.changeUserState(psr.get("userId"), 2);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Plz enter in this format");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }

    @Test
    public void testState2b2() {
        String userId = "cliubf";
        this.setMockDatabase(10);
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.setState("AskPortion")
                .set("textContent", "15:100");
        asker.changeUserState(psr.get("userId"), 2);
        Event<ParserMessageJSON> ev =
                new Event<ParserMessageJSON>(null, psr);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation)
                    throws Throwable {
                FormatterMessageJSON fmt = invocation.getArgumentAt(0,
                        FormatterMessageJSON.class);
                assert fmt.getUserId().equals(userId);
                JSONArray messages = (JSONArray)fmt.getMessageArray();
                String text1 = (String) messages.getJSONObject(0).get("textContent");
                assert text1.startsWith("Roger, 100 gram laoxigua");
                return null;
            }
        }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
        asker.accept(ev);
    }
}