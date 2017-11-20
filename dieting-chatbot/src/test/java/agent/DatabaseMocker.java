package agent;

import java.util.HashMap;
import org.json.JSONObject;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import database.keeper.CampaignKeeper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DatabaseMocker {

    private static HashMap<String, String> campaigns = new HashMap<>();
    private static HashMap<String, JSONObject> users = new HashMap<>();
    private static HashMap<String, JSONObject> menus = new HashMap<>();

    @Bean
    public CampaignKeeper createCampaignKeeper() {
        campaigns.put("COUPON_COUNT_", "0");
        CampaignKeeper keeper = Mockito.mock(CampaignKeeper.class);
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) {
                return campaigns.get("COUPON_COUNT_");
            }
        }).when(keeper).getCouponCnt();
        Mockito.doAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocationOnMock) {
                int cnt = Integer.parseInt(campaigns.get("COUPON_COUNT_"));
                campaigns.put("COUPON_COUNT_", ""+(++cnt));
                return (long) cnt;
            }
        }).when(keeper).incrCouponCnt();
        Mockito.doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) {
                String key = invocationOnMock.getArgumentAt(0, String.class);
                String parentUserId = invocationOnMock.getArgumentAt(1, String.class);
                campaigns.put(key, parentUserId);
                return true;
            }
        }).when(keeper).setParentUserId(Matchers.anyString(), Matchers.anyString());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) {
                String key = invocationOnMock.getArgumentAt(0, String.class);
                if (campaigns.containsKey(key)) return campaigns.get(key);
                else return null;
            }
        }).when(keeper).getParentUserId(Matchers.anyString());
        return keeper;
    }

    @Bean
    public UserManager createUserManager() {
        UserManager manager = Mockito.spy(UserManager.class);
        Mockito.doAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocationOnMock) {
                String userId = invocationOnMock.getArgumentAt(0, String.class);
                return users.getOrDefault(userId, null);
            }
        }).when(manager).getUserJSON(Matchers.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                String userId = invocationOnMock.getArgumentAt(0, String.class);
                JSONObject userJSON = invocationOnMock.getArgumentAt(1, JSONObject.class);
                users.put(userId, userJSON);
                return null;
            }
        }).when(manager).storeUserJSON(Matchers.anyString(), Matchers.any(JSONObject.class));
        return manager;
    }

    @Bean
    public MenuManager createMenuManager() {
        MenuManager manager = Mockito.spy(MenuManager.class);
        Mockito.doAnswer(new Answer<JSONObject>() {
            @Override
            public JSONObject answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                return menus.getOrDefault(userId, null);
            }
        }).when(manager).getMenuJSON(Matchers.anyString());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                String userId = invocation.getArgumentAt(0, String.class);
                JSONObject menuJSON = invocation.getArgumentAt(1, JSONObject.class);
                menus.put(userId, menuJSON);
                return null;
            }
        }).when(manager).storeMenuJSON(Matchers.anyString(), Matchers.any(JSONObject.class));
        return manager;
    }
}