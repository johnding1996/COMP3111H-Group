package agent;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import database.keeper.MenuKeeper;
import lombok.extern.slf4j.Slf4j;

/**
 * MenuManager: wrapper for MenuKeeper.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class MenuManager {
    /**
     * Get MenuJSON from MenuKeeper.
     * @param userId String of user Id
     * @return A MenuJSON
     */
    public JSONObject getMenuJSON(String userId) {
        MenuKeeper keeper = new MenuKeeper();
        JSONObject menuJSON = keeper.get(userId, 1).getJSONObject(0);
        keeper.close();
        return menuJSON;
    }

    /**
     * Store MenuJSON to MenuKeeper.
     * @param userId String of user Id
     * @param menuJSON MenuJSON to store
     */
    public void storeMenuJSON(String userId, JSONObject menuJSON) {
        MenuKeeper keeper = new MenuKeeper();
        keeper.set(userId, menuJSON);
        keeper.close();
    }
}