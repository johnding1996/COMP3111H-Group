package utility;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * FormatterMessageJSON: used by agent to send message through line API.
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
public class FormatterMessageJSON extends MessageJSON {
    /**
     * Constructor for FormatterMessageJSON.
     * Empty message array for acknowledgement.
     * @param userId String of user Id.
     */
    public FormatterMessageJSON(String userId) {
        json.put("userId", userId)
            .put("messages", new JSONArray());
    }

    /**
     * Get message array of FormatterMessageJSON.
     * @return JSONArray containing the messages.
     */
    public JSONArray getMessageArray() {
        return json.getJSONArray("messages");
    }

    /**
     * Append text message to the messages JSONArray.
     * @param textContent String of text content.
     * @return this object.
     */
    public FormatterMessageJSON appendTextMessage(String textContent) {
        JSONObject text = new JSONObject();
        text.put("type", "text")
            .put("textContent", textContent);
        json.append("messages", text);
        return this;
    }

    /**
     * Append image message to the messages JSONArray.
     * @param originalContentUrl Url of original image.
     * @param previewContentUrl Url of preview image, may be same as originalConentUrl.
     * @return this object.
     */
    public FormatterMessageJSON appendImageMessage(
        String originalContentUrl, String previewContentUrl) {
        JSONObject image = new JSONObject();
        image.put("type", "image")
             .put("originalContentUrl", originalContentUrl)
             .put("previewContentUrl", previewContentUrl);
        json.append("messages", image);
        return this;
    }
}