package database.keeper;
import database.connection.RedisPool;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.skyscreamer.jsonassert.JSONAssert;
import redis.clients.jedis.Jedis;

import java.util.List;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)

public class CampaignKeeperTester {
    private static Jedis jedis;
    private static CampaignKeeper campaignKeeper;
    private static String user_id = "813f61a35fbb9cc3adc28da525abf1fe";
    private static String parent_id = "913f61a35fbb9cc3adc28da525abf1fe";
    private static String code = "123456";

    @BeforeClass
    public static void setUpClass() {
        jedis = RedisPool.getConnection();
        campaignKeeper = new CampaignKeeper(jedis);
    }

    @AfterClass
    public static void tearDownClass() {
        campaignKeeper.close();
    }

    @Before
    public void setUp() {
        jedis.del("campaign:" + user_id);
    }

    @After
    public void tearDown() {
        jedis.del("campaign:" + user_id);
    }

    @Test
    public void testSetCouponImgSuccess(){
        campaignKeeper.setCouponImg("foobar");
        String actual = campaignKeeper.getCouponImg();
        assertTrue(actual.equals("foobar"));
    }

    @Test
    public void testSetCouponCntSuccess(){
        campaignKeeper.resetCouponCnt();
        assertTrue(campaignKeeper.getCouponCnt().equals("1"));
        campaignKeeper.incrCouponCnt();
        assertTrue(campaignKeeper.getCouponCnt().equals("2"));

    }

    @Test
    public void testSetSharingCodeSuccess(){
        assertTrue(campaignKeeper.setSharingCode(user_id, code));
        String actual = campaignKeeper.getSharingCode(user_id);
        assertTrue(actual.equals("123456"));
    }


    @Test
    public void testSetParentUserIdSuccess(){
        assertTrue(campaignKeeper.setParentUserId(user_id, parent_id));
        String actual = campaignKeeper.getParentUserId(user_id);
        assertTrue(actual.equals(parent_id));
    }

//    @Test
//    public void testSetSuccess() {
//        campaignKeeper.set(user_id, goodCampaignJson);
//        List<String> actual = jedis.lrange("campaign:" + user_id, 0, -1);
//        JSONAssert.assertEquals(goodCampaignJson, new JSONObject(actual.get(0)), false);
//    }
//
//    @Test
//    public void testSetFailure() {
//        boolean result = campaignKeeper.set(user_id, badCampaignJson);
//        assertTrue(!result);
//    }
//
//    @Test
//    public void testGetSuccess() {
//        campaignKeeper.set(user_id, goodCampaignJson);
//        campaignKeeper.set(user_id, goodCampaignJson);
//        campaignKeeper.set(user_id, goodCampaignJson);
//        campaignKeeper.set(user_id, goodCampaignJson);
//        JSONArray actual = campaignKeeper.get(user_id, 5);
//        JSONArray expected  = new JSONArray();
//        expected.put(goodCampaignJson);
//        expected.put(goodCampaignJson);
//        expected.put(goodCampaignJson);
//        expected.put(goodCampaignJson);
//        JSONAssert.assertEquals(expected, actual, false);
//    }
//
//    @Test
//    public void testGetFailure() {
//        JSONArray result = campaignKeeper.get(user_id, 5);
//        assertNull(result);
//    }
}
