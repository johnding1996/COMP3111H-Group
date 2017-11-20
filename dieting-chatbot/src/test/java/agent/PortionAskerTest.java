package agent;

import controller.State;
import controller.TestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

import java.util.Arrays;

import javax.annotation.PostConstruct;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {PortionAsker.class, JazzySpellChecker.class})
@ContextConfiguration(classes = {TestConfiguration.class, DatabaseMocker.class})
public class PortionAskerTest extends AgentTest {

    @Autowired
    private PortionAsker asker;

    @Autowired
    private MenuManager menuManager;

    // //Used to save mocked user menu json record
    // private static HashMap<String, JSONObject> userMenuJSON = new HashMap<>();

    // //test userId throughout this test suite
    // private static final String userId = "cliubf";

    @PostConstruct
    public void init() {
        agent  = asker;
        agentState = State.ASK_PORTION;
        userId = "cliubf";

        JSONObject menuJSON = new JSONObject();
        JSONArray menu = new JSONArray();

        for(int i = 0; i < 3; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "xiaoxigua");
            menu.put(dish);
        }

        for(int i = 0; i < 3; i++){
            JSONObject dish = new JSONObject();
            dish.put("name", "laoxigua");
            menu.put(dish);
        }

        menuJSON.put("userId", userId);
        menuJSON.put("menu", menu);
        menuManager.storeMenuJSON(userId, menuJSON);
    }

    @Test
    public void testConstruct() {
        assert asker != null;
    }

    @Test
    public void testTransition() {
        asker.registerUser(userId);
        checkHandler("", Arrays.asList("Okay, here is",
            "", "Would you like"), 0, 1);
    }

    // @Bean
    // public PortionAsker createPortionAsker(){
    //     PortionAsker asker = Mockito.spy(PortionAsker.class);
    //     Mockito.doAnswer(new Answer<JSONObject>() {
    //         @Override
    //         public JSONObject answer(InvocationOnMock invocation) {
    //             String userId = invocation.getArgumentAt(0, String.class);
    //             return userMenuJSON.get(userId);
    //         }
    //     }).when(asker).getMenuKeeperJSON(Matchers.anyString());
    //     Mockito.doAnswer(new Answer<Boolean>() {
    //         @Override
    //         public Boolean answer(InvocationOnMock invocation) {
    //             String userId = invocation.getArgumentAt(0, String.class);
    //             JSONObject newMenuJSON = invocation.getArgumentAt(1, JSONObject.class);
    //             userMenuJSON.put(userId, newMenuJSON);
    //             return true;
    //         }
    //     }).when(asker).setMenuKeeperJSON(Matchers.anyString(),
    //             Matchers.any(JSONObject.class));

    //     return asker;
    // }

    // /**
    //  * Assert replies for a psr have expected prefix.
    //  * @param prefixList List of prefix
    //  * @param psr ParserMessageJSON
    //  */
    // private void assertReplyContent(List<String> prefixList, ParserMessageJSON psr) {
    //     Event<ParserMessageJSON> ev = new Event<ParserMessageJSON>(null, psr);
    //     Mockito.doAnswer(new Answer<Void>() {
    //         @Override
    //         public Void answer(InvocationOnMock invocation)
    //                 throws Throwable {
    //             FormatterMessageJSON fmt = invocation.getArgumentAt(0,
    //                     FormatterMessageJSON.class);
    //             assert fmt.getUserId().equals(userId);
    //             JSONArray messages = fmt.getMessageArray();
    //             if (messages.length() == 0) return null;
    //             for (int i=0; i<prefixList.size(); ++i) {
    //                 String text = messages.getJSONObject(i).getString("textContent");
    //                 assert text.startsWith(prefixList.get(i));
    //             }
    //             return null;
    //         }
    //     }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
    //     asker.accept(ev);
    //     Mockito.reset(publisher);
    // }

    // @Test
    // public void testAcceptImageMessage() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "image");
    //     psr.setState("AskPortion");
    //     asker.changeUserState(psr.get("userId"), 0);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("I am sorry that");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testAnotherState() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("RecordMeal")
    //             .set("textContent", "hello");
    //     Event<ParserMessageJSON> ev =
    //             new Event<ParserMessageJSON>(null, psr);
    //     Mockito.doAnswer(new Answer<Void>() {
    //         @Override
    //         public Void answer(InvocationOnMock invocation)
    //                 throws Throwable {
    //             assert false;
    //             return null;
    //         }
    //     }).when(publisher).publish(Matchers.any(FormatterMessageJSON.class));
    //     asker.accept(ev);
    //     Mockito.reset(publisher);
    // }

    // @Test
    // public void testState0() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "hello");
    //     asker.changeUserState(psr.get("userId"), 0);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Okay, this is");
    //     prefixList.add("1 - xiaoxigua");
    //     prefixList.add("Would you like to update");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState1a() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "Yes");
    //     asker.changeUserState(psr.get("userId"), 1);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Okay, so give me");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState1b() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "No");
    //     asker.changeUserState(psr.get("userId"), 1);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Alright, let's move on");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState1c() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "skdhfkjdsfh");
    //     asker.changeUserState(psr.get("userId"), 1);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Sorry, I'm not sure");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2a() {
    //     this.setMockDatabase(1);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "leave");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Alright, we are going to process your update");
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b1a() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "hskdfjh:100");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Plz enter in this format");
    //     log.info(psr.toString());
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b1b() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "6:sdfhks");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Plz enter in this format");
    //     log.info(psr.toString());
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b1c() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "100:100");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Plz enter in this format");
    //     log.info(psr.toString());
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b1d() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "100:1000000");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Plz enter in this format");
    //     log.info(psr.toString());
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b1e() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "1:1:1");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Plz enter in this format");
    //     log.info(psr.toString());
    //     assertReplyContent(prefixList, psr);
    // }

    // @Test
    // public void testState2b2() {
    //     this.setMockDatabase(10);

    //     ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
    //     psr.setState("AskPortion")
    //             .set("textContent", "15:100");
    //     asker.changeUserState(psr.get("userId"), 2);

    //     List<String> prefixList = new ArrayList<>();
    //     prefixList.add("Roger, 100 gram laoxigua");
    //     assertReplyContent(prefixList, psr);
    // }
}