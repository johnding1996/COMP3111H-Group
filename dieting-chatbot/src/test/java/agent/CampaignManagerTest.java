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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import utility.JazzySpellChecker;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CampaignManager.class, JazzySpellChecker.class})
@ContextConfiguration(classes = {TestConfiguration.class, DatabaseMocker.class})
public class CampaignManagerTest extends AgentTest {

    @Autowired
    private CampaignManager campaignManager;

    @Autowired
    private UserManager userManager;

    private JSONObject userJSON = new JSONObject();
    private JSONObject adminJSON = new JSONObject();

    // private String parentUserId = "hfanhfanhfanhfanhfanhfanhfanhfan";
    // private String correctCode = "999999";

    @PostConstruct
    public void init() {
        agent = campaignManager;
        userId = "blahblahblahblahblahblahblahblah";
        userJSON.put("userId", userId);
        adminJSON.put("userId", "admin")
                 .put("isAdmin", true);
        userManager.storeUserJSON("admin", adminJSON);
    }

    @Test
    public void testCampaignManagement() {
        campaignManager.availableCoupon = 0;

        agentState = State.MANAGE_CAMPAIGN;
        campaignManager.registerUser(userId);
        checkHandler("", "Sorry, we cannot", 0, Agent.END_STATE);

        userManager.storeUserJSON(userId, userJSON);
        campaignManager.registerUser(userId);
        checkHandler("", "You are not an", 0, 1);
        checkHandler("wrongAccessCode", "Incorrect access code", 1, Agent.END_STATE);

        campaignManager.registerUser(userId);
        checkHandler("", "You are not an", 0, 1);
        checkHandler(CampaignManager.ADMIN_ACCESS_CODE, "", 1, 3);
        checkHandler("-1", "", 3, 3);
        checkHandler("Can you understand", "", 3, 3);
        checkHandler("100", Arrays.asList("Start campaign", "Now please"), 3, 4);
        checkHandler("Blah", Arrays.asList("Set coupon text", "Leaving admin mode"),
            4, Agent.END_STATE);
        
        campaignManager.registerUser(userId);
        checkHandler("", Arrays.asList("Hi admin", "The campaign is now open",
            "Do you want to"), 0, 2);
        checkHandler("whatever", "", 2, 2);
        checkHandler("no", "Update coupon number skipped", 2, 4);
        campaignManager.setUserState(userId, 2);
        checkHandler("10", "Set available coupon to 10", 2, 4);
        checkHandler("UPDATE: bbb", "Update coupon text", 4, Agent.END_STATE);

        campaignManager.registerUser(userId);
        checkHandler("", "Hi admin", 0, 2);
        checkHandler("0", "Set available coupon to non", 2, Agent.END_STATE);

        campaignManager.registerUser(userId);
        checkHandler("", Arrays.asList("Hi", "The campaign is not", "Input a pos"), 0, 3);
        checkHandler("nope", "OK, cancelling", 3, Agent.END_STATE);

        campaignManager.registerUser(userId);
        checkHandler("", Arrays.asList("Hi", "The campaign", "Input"), 0, 3);
        checkHandler("100", Arrays.asList("Start", "Now please"), 3, 4);
        campaignManager.setUserState(userId, 2);
        checkHandler("10", "Set available", 2, 4);
        checkHandler("blah", "Cancel to update", 4, Agent.END_STATE);
    }

    @Test
    public void testCouponClaiming() {
        campaignManager.availableCoupon = 0;

        agentState = State.CLAIM_COUPON;
        campaignManager.registerUser(userId);
        checkHandler("", "Great! What is your code", 0, 5);
        checkHandler("blah", Arrays.asList("Sorry, the format",
            "The correct format"), 5, Agent.END_STATE);

        campaignManager.registerUser(userId);
        campaignManager.setUserState(userId, 5);
        checkHandler("abcdef", Arrays.asList("Sorry, the format",
            "The correct format"), 5, Agent.END_STATE);

        campaignManager.registerUser(userId);
        campaignManager.setUserState(userId, 5);
        checkHandler("123456", "Sorry, the campaign is not open",
            5, Agent.END_STATE);

        // campaignManager.availableCoupon = 10;
        // campaignManager.registerUser(userId);
        // campaignManager.setUserState(userId, 5);
        // checkHandler("123456", Arrays.asList("Well, this is not a valid",
        //     "Please double check"), 5, Agent.END_STATE);
        
        // campaignManager.registerUser(userId);
        // campaignManager.setUserState(userId, 5);
        // checkHandler(correctCode, Arrays.asList("Well, I don't have",
        //     "Please do so by"), 5, Agent.END_STATE);
    }
}