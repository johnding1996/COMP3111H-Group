package agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.asprise.ocr.Ocr;

@Slf4j
@Component
public class ImageMenuParser{

    /**
     * Parse JSON object from Image uri 
     * @param uri URL string to use
     * @return A JSON array
     */
    public static String readJsonFromImage(String uri)
        throws IOException, JSONException {
        
        URL url = null;
        
        // handle Exception
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            System.out.println("The URL is not valid.");
            System.out.println(e.getMessage());
        }
        Ocr.setUp(); // one time setup
        Ocr ocr = new Ocr(); // create a new OCR engine
        ocr.startEngine("eng", Ocr.SPEED_FASTEST); // English
        String s = ocr.recognize(new URL[] {url}
        , Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        log.info("Result: " + s);
        // ocr more images here ...
        ocr.stopEngine();
        return s;
        
    }

    /**
     * Build menu information from JSON array
     * @param arr A JSON array
     * @return A JSON array; null if no dish is successfully parsed
     */
//    public static JSONArray buildMenu(String str) {
        // JSONArray ret = new JSONArray();
        // JSONObject json;
        // for (int i=0; i<arr.length(); ++i) {
        //     json = arr.getJSONObject(i);
        //     try {
        //         assert json.get("name") instanceof String;
        //         JSONObject dish = new JSONObject();
        //         dish.put("name", (String)json.get("name"));
        //         ret.put(dish);
        //     } catch (Exception e) {
        //         log.info("This JSON is not a valid dish:" +
        //             json.toString(4));
        //         log.info("Error message: {}", e.toString());
        //     }
        // }
        // if (ret.length() > 0) return ret;
        // else {
        //     log.info("Error: empty menu list!");
        //     return null;
        // }
    //}

    
}