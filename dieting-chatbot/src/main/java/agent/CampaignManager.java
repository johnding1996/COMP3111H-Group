package agent;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import controller.State;
import database.keeper.CampaignKeeper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import utility.Validator;

/**
 * CampaignManager: manage sharing, coupon claiming and coupon image upload.
 * 
 * State mapping:
 *      0 - branching (assign sharing code/check claimed code/start admin mode)
 *      1 - enable admin access
 *      2 - update available coupon (campaign already started)
 *      3 - set available coupon (campaign not started)
 *      4 - set coupon image
 *      5 - check coupon code
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class CampaignManager extends Agent {

    @Autowired
    private UserManager userManager;

    int availableCoupon = 0;
    static final String ADMIN_ACCESS_CODE = "I am Sung Kim!!";
    Instant campaignStartInstant;
    String couponTextContent;
    int currentCouponId = 0;

    /**
     * Initialize campaign manager agent.
     */
    @Override
    public void init() {
        agentName = "CampaignManager";
        agentStates = new HashSet<>(
            Arrays.asList(State.INVITE_FRIEND, State.CLAIM_COUPON, State.MANAGE_CAMPAIGN)
        );
        handleImage = false;
        useSpellChecker = false;
        this.addHandler(0, (psr) -> branchHandler(psr))
            .addHandler(1, (psr) -> enableAdminAccess(psr))
            .addHandler(2, (psr) -> updateCouponNumber(psr))
            .addHandler(3, (psr) -> startCampaign(psr))
            .addHandler(4, (psr) -> setCouponImage(psr))
            .addHandler(5, (psr) -> checkCoupon(psr));
    }

    /**
     * Handler for first interaction, providing branching.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int branchHandler(ParserMessageJSON psr) {
        State state = psr.getState();
        if (state == State.INVITE_FRIEND) {
            return inviteFriendHandler(psr);
        } else if (state == State.CLAIM_COUPON) {
            return claimCouponHandler(psr);
        } else {
            return campaignManageHandler(psr);
        }
    }

    /**
     * Handler for inviting friend.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int inviteFriendHandler(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (availableCoupon <= 0) {
            fmt.appendTextMessage("Sorry, the campaign is not open at this stage. " +
                "Please keep an eye on this so that you won't miss it!");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        CampaignKeeper keeper = getCampaignKeeper();
        int couponCount = Integer.parseInt(keeper.getCouponCnt());
        if (couponCount >= availableCoupon) {
            fmt.appendTextMessage("Sorry, but we don't have more available coupon now. " +
                "Please be earlier next time!");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            keeper.close();
            return END_STATE;
        }

        fmt.appendTextMessage("Thank you for promoting our service!")
           .appendTextMessage(String.format("This is your sharing code: %06d", currentCouponId));
        keeper.setParentUserId(String.format("%06d", currentCouponId), userId);
        keeper.close();
        ++currentCouponId;

        publisher.publish(fmt);
        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * Handler for claiming coupon.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int claimCouponHandler(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Great! What is your code for the coupon? " +
            "Please input only the 6 digit code.");
        publisher.publish(fmt);
        return 5;
    }

    /**
     * Handler for checking coupon id.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int checkCoupon(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").trim();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (text.length() != 6 || !Validator.isInteger(text)) {
            fmt.appendTextMessage("Sorry, the format for claiming coupon is invalid")
               .appendTextMessage("The correct format is 'code <6-digit number>'.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        if (availableCoupon <= 0) {
            fmt.appendTextMessage("Sorry, the campaign is not open at this stage. " +
                "Please stay tuned on this :)");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        CampaignKeeper keeper = getCampaignKeeper();
        // log.info("COUPON COUNT = " + keeper.getCouponCnt());
        int couponCount = Integer.parseInt(keeper.getCouponCnt());
        if (couponCount >= availableCoupon) {
            fmt.appendTextMessage("Sorry, but all the available coupons are now distributed. " +
                "Please be earlier next time ~");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            keeper.close();
            return END_STATE;
        }

        String parentUserId = keeper.getParentUserId(text);
        keeper.close();
        if (parentUserId == null) {
            fmt.appendTextMessage("Well, this is not a valid sharing Id.")
               .appendTextMessage("Please double check. Session cancelled.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        JSONObject userJSON = userManager.getUserJSON(userId);
        if (userJSON == null) {
            fmt.appendTextMessage("Well, I don't have your personal information yet, " +
                "so you cannot claim the coupon now.")
               .appendTextMessage("Please do so by 'setting'. Session cancelled.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        if (userJSON.keySet().contains("parentUserId")) {
            fmt.appendTextMessage("Well, one user can only claim the coupon using 'code' once.")
               .appendTextMessage("But you can share this chatbot with your friends to " +
                "get more coupons!");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        if (parentUserId.equals(userId)) {
            fmt.appendTextMessage("Well, seems that this code is issued as your sharing code, " +
                "so you cannot self-claim it :(");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        // check whether is new user

        // update database
        userJSON.put("parentUserId", parentUserId);
        userManager.storeUserJSON(userId, userJSON);
        keeper = getCampaignKeeper();
        keeper.incrCouponCnt();
        keeper.close();

        fmt.appendTextMessage("Congratulations! This is the coupon you claimed:")
           .appendTextMessage(couponTextContent);
        publisher.publish(fmt);
        fmt = new FormatterMessageJSON(parentUserId);
        fmt.appendTextMessage("Hey, one more user follows our chatbot by your promotion ~")
           .appendTextMessage("This coupon is rewarded to you:")
           .appendTextMessage(couponTextContent);
        publisher.publish(fmt);

        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * Handler for campaign management.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int campaignManageHandler(ParserMessageJSON psr) {
        String userId = psr.getUserId();

        JSONObject userJSON = userManager.getUserJSON(userId);
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (userJSON == null) {
            fmt.appendTextMessage("Sorry, we cannot verify your admin identity. " +
                "Please do 'setting' first. Session cancelled.");
            publisher.publish(fmt);

            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }

        boolean isAdmin = true;
        if (!userJSON.keySet().contains("isAdmin")) isAdmin = false;
        else if (!userJSON.getBoolean("isAdmin")) isAdmin = false;
        if (!isAdmin) {
            fmt.appendTextMessage("You are not an admin user now. " +
                "If you want to enable your admin priviledge, please input your admin access code.");
            publisher.publish(fmt);
            return 1;
        }

        fmt.appendTextMessage("Hi admin user! ^_^");
        if (availableCoupon > 0) {
            fmt.appendTextMessage("The campaign is now open, and the number of " +
                "available coupon is " + availableCoupon + ".")
               .appendTextMessage("Do you want to update it? Tell me a number or say 'skip'.");
            publisher.publish(fmt);
            return 2;
        } else {
            fmt.appendTextMessage("The campaign is not open now. Do you want to start it now?")
               .appendTextMessage("Input a positive number as the number of available coupons " +
                "for this campaign, or say 'no'.");
            publisher.publish(fmt);
            return 3;
        }
    }

    /**
     * Handler for enabling admin access.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int enableAdminAccess(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (text.equals(ADMIN_ACCESS_CODE)) {
            fmt.appendTextMessage("Admin mode has been enabled for you.");
            publisher.publish(fmt);
            sleep();

            log.info("{}: update UserJSON", agentName);
            JSONObject userJSON = userManager.getUserJSON(userId);
            userJSON.put("isAdmin", true);
            userManager.storeUserJSON(userId, userJSON);

            return campaignManageHandler(psr);
        } else {
            fmt.appendTextMessage("Incorrect access code. Access denied.");
            publisher.publish(fmt);
            controller.setUserState(userId, State.IDLE);
            return END_STATE;
        }
    }

    /**
     * Handler for updating number of available coupons.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int updateCouponNumber(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent").toLowerCase();

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (Validator.isInteger(text)) {
            int couponNumber = Integer.parseInt(text);
            if (couponNumber <= 0) {
                availableCoupon = 0;
                fmt.appendTextMessage("Set available coupon to non-positive, campaign stopped.");
                publisher.publish(fmt);

                controller.setUserState(userId, State.IDLE);
                return END_STATE;
            } else {
                availableCoupon = couponNumber;
                fmt.appendTextMessage("Set available coupon to " + availableCoupon + ".");
            }
        } else {
            if (TextProcessor.getMatch(TextProcessor.getTokens(text),
                Arrays.asList("no", "nope", "skip", "next")) != null) {
                fmt.appendTextMessage("Update coupon number skipped.");
            } else {
                rejectUserInput(psr, "Please tell me the updated available coupon number, " +
                    "or ask me to skip this part explicitly.");
                return 2;
            }
        }

        states.get(userId).put("usePrefix", true);
        fmt.appendTextMessage("Now do you want to update the text content of the coupon? " +
            "If yes, say 'UPDATE: <text>'. Otherwise, I will assume you skip this part.");
        publisher.publish(fmt);
        return 4;
    }

    /**
     * Handler for starting campaign.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int startCampaign(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (Validator.isInteger(text)) {
            int num = Integer.parseInt(text);
            if (num > 0) {
                availableCoupon = num;
                states.get(userId).put("usePrefix", false);
                campaignStartInstant = Instant.now();
                fmt.appendTextMessage("Start campaign with a total of " + num + " coupon(s).");
                fmt.appendTextMessage("Now please set the text content of the coupon:");
                publisher.publish(fmt);

                CampaignKeeper keeper = getCampaignKeeper();
                keeper.resetCouponCnt();
                keeper.close();
                return 4;
            } else {
                rejectUserInput(psr, "Either input a positive number, or say you do not want " +
                    "to start a campaign explicitly.");
                return 3;
            }
        } else {
            if (TextProcessor.getMatch(TextProcessor.getTokens(text),
                Arrays.asList("no", "nope", "n't")) != null) {
                fmt.appendTextMessage("OK, cancelling to start a campaign.");
                publisher.publish(fmt);
                controller.setUserState(userId, State.IDLE);
                return END_STATE;
            } else {
                rejectUserInput(psr, "I don't understand what you have said.");
                return 3;
            }
        }
    }

    /**
     * Handler for setting coupon image.
     * @param psr Input ParserMessageJSON
     * @return next state
     */
    public int setCouponImage(ParserMessageJSON psr) {
        String userId = psr.getUserId();
        String text = psr.get("textContent");
        boolean usePrefix = states.get(userId).getBoolean("usePrefix");

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        if (usePrefix) {
            if (text.startsWith("UPDATE: ")) {
                couponTextContent = text.replaceFirst("UPDATE: ", "");
                fmt.appendTextMessage("Update coupon text content to " + couponTextContent +".");
            } else {
                fmt.appendTextMessage("Cancel to update coupon text content.");
            }
        } else {
            couponTextContent = text;
            fmt.appendTextMessage("Set coupon text content to " + couponTextContent +".");
        }

        fmt.appendTextMessage("Leaving admin mode for campaign management.");
        publisher.publish(fmt);
        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }

    /**
     * Get campaign keeper.
     * @return A campaign keeper
     */
    CampaignKeeper getCampaignKeeper() {
        return new CampaignKeeper();
    }
}