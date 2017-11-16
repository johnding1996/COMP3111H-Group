package database.keeper;

import database.connection.RedisPool;

import org.junit.*;
import redis.clients.jedis.Jedis;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)

public class CampaignKeeperTester {
    private static Jedis jedis;
    private static CampaignKeeper campaignKeeper;
    private static String parent_id = "913f61a35fbb9cc3adc28da525abf1fe";
    private static String code = "123456";
    private static String badCode = "123";
    private static String badId = "SungKim";

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
        jedis.del("campaign:image");
        jedis.del("campaign:count");
        jedis.del("campaign:parent:" + code);
        jedis.del("campaign:parent:" + badCode);
    }

    @After
    public void tearDown() {
        jedis.del("campaign:image");
        jedis.del("campaign:count");
        jedis.del("campaign:parent:" + code);
        jedis.del("campaign:parent:" + badCode);
    }

    @Test
    public void testSetCouponImgSuccess(){
        boolean result = campaignKeeper.setCouponImg("foobar");
        String actual = campaignKeeper.getCouponImg();
        assertTrue(result && actual.equals("foobar"));
    }

    @Test
    public void testSetCouponCntSuccess(){
        boolean result = campaignKeeper.resetCouponCnt();
        assertTrue(result && campaignKeeper.getCouponCnt().equals("0"));
    }

    @Test
    public void testIncrCouponCntSuccess(){
        campaignKeeper.resetCouponCnt();
        long result = campaignKeeper.incrCouponCnt();
        assertTrue((result==1) && campaignKeeper.getCouponCnt().equals("1"));
    }

    @Test
    public void testSetParentUserIdSuccess(){
        assertTrue(campaignKeeper.setParentUserId(code, parent_id));
        String actual = campaignKeeper.getParentUserId(code);
        assertTrue(actual.equals(parent_id));
    }

    @Test
    public void testSetCodeUserIdPairFailure(){
        assertTrue(!campaignKeeper.setParentUserId(badCode,badId));
    }

}
