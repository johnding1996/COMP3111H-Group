package agent;

import org.springframework.stereotype.Component;

import controller.State;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

/**
 * CampaignManager: manage sharing, coupon claiming and coupon image upload.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class CampaignManager extends Agent {

    private boolean inCampaign = false;

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
        this.addHandler(0, (psr) -> branchHandler(psr));
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
        fmt.appendTextMessage("Great, you want to invite friend!");
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
        fmt.appendTextMessage("Great, you want to claim coupon!");
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

        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("Great, you want to manage campaign!");
        publisher.publish(fmt);

        controller.setUserState(userId, State.IDLE);
        return END_STATE;
    }
}