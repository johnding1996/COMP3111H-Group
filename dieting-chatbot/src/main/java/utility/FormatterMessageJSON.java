package utility;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
<<<<<<< HEAD
 * FormatterMessageJSON: used by agent to send message through line API
=======
 * FormatterMessageJSON: used by agent to send message through line API.
>>>>>>> develop
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
public class FormatterMessageJSON extends MessageJSON {
    /**
     * Constructor for FormatterMessageJSON.
     * Empty message array for acknowledgement.
<<<<<<< HEAD
     * @param userId String of user Id
=======
     * @param userId String of user Id.
>>>>>>> develop
     */
    public FormatterMessageJSON(String userId) {
        json.put("userId", userId)
            .put("messages", new JSONArray());
    }

    /**
<<<<<<< HEAD
     * Get message array of FormatterMessageJSON
     * @return JSONArray containing the messages
=======
     * Get message array of FormatterMessageJSON.
     * @return JSONArray containing the messages.
>>>>>>> develop
     */
    public JSONArray getMessageArray() {
        return json.getJSONArray("messages");
    }

    /**
<<<<<<< HEAD
     * Append text message to the messages JSONArray
     * @param textContent String of text content
     * @return this object
=======
     * Append text message to the messages JSONArray.
     * @param textContent String of text content.
     * @return this object.
>>>>>>> develop
     */
    public FormatterMessageJSON appendTextMessage(String textContent) {
        JSONObject text = new JSONObject();
        text.put("type", "text")
            .put("textContent", textContent);
        json.append("messages", text);
        return this;
    }

    /**
<<<<<<< HEAD
     * Append image message to the messages JSONArray
     * @param originalContentUrl Url of original image
     * @param previewContentUrl Url of preview image, may be same as originalConentUrl
     * @return this object
=======
     * Append image message to the messages JSONArray.
     * @param originalContentUrl Url of original image.
     * @param previewContentUrl Url of preview image, may be same as originalConentUrl.
     * @return this object.
>>>>>>> develop
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