package agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UrlMenuParser {

    /**
     * Read String from a Reader
     * @param rd The Reader to use
     * @return The String read from rd
     */
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Parse JSON object from url
     * @param url URL string to use
     * @return A JSON array
     */
    public static JSONArray readJsonFromUrl(String url)
        throws IOException, JSONException {

        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(
                new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray arr = new JSONArray(jsonText);
            return arr;
        } finally {
            is.close();
        }
    }

    /**
     * Build menu information from JSON array
     * @param arr A JSON array
     * @return A JSON array; null if no dish is successfully parsed
     */
    public static JSONArray buildMenu(JSONArray arr) {
        JSONArray ret = new JSONArray();
        JSONObject json;
        for (int i=0; i<arr.length(); ++i) {
            json = arr.getJSONObject(i);
            try {
                assert json.get("name") instanceof String;
                JSONObject dish = new JSONObject();
                dish.put("name", (String)json.get("name"));
                ret.put(dish);
            } catch (Exception e) {
                log.info("This JSON is not a valid dish:" +
                    json.toString(4));
                log.info("Error message: {}", e.toString());
            }
        }
        if (ret.length() > 0) return ret;
        else {
            log.info("Error: empty menu list!");
            return null;
        }
    }

    /**
     * Build menu information from given URL
     * @param url String of URL
     * @return A JSON array; null if no dish is successfully parsed
     */
    public static JSONArray buildMenu(String url) {
        try {
            JSONArray arr = readJsonFromUrl(url);
            return buildMenu(arr);
        } catch (Exception e) {
            log.info("Error in parsing JSON from the given URL: {}",
                url);
            return null;
        }
    }
}