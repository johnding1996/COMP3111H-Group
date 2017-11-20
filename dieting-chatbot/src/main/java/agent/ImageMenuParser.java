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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import utility.JazzySpellChecker;

import com.asprise.ocr.Ocr;

/**
 * ImageMenuParser: parser image into JSONArray using OCR and SpellChecker.
 * @author agong, szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class ImageMenuParser {

    private static JazzySpellChecker spellChecker = new JazzySpellChecker();

    /**
     * Parse JSON object from Image uri.
     * @param type can be either "uri" or "path"
     * @param uri URL or path string to use 
     * @return A JSON array
     */
    public static String readJsonFromImage(String type, String uri) {
        Ocr.setUp(); // one time setup
        Ocr ocr = new Ocr(); // create a new OCR engine
        ocr.startEngine("eng", Ocr.SPEED_FASTEST, "PROP_IMG_PREPROCESS_TYPE=custom|"
            + "PROP_IMG_PREPROCESS_CUSTOM_CMDS=deskew();grayscale();bin1();autoRotate()");

        String s = "";
        if (type.equals("uri")) {
            URL url = null;
            // handle Exception
            try {
                url = new URL(uri);
                s = ocr.recognize(new URL[] { url }, Ocr.RECOGNIZE_TYPE_ALL,
                    Ocr.OUTPUT_FORMAT_PLAINTEXT);
            } catch (MalformedURLException e) {
                log.info("The URL is not valid.");
            }
        }
        else if (type.equals("path")) {
            s = ocr.recognize(new File[] { new File(uri) },
                Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        }
        
        log.info("OCR result:\n{}", s);
        ocr.stopEngine();
        return s;
    }

    /**
     * Build menu information from JSON array.
     * @param type can either be uri or path, delegate to OCR
     * @param uri uri of the image menu
     * @return A JSON array; null if no dish is successfully parsed
     */
    public static JSONArray buildMenu(String type, String uri) {
        String text = readJsonFromImage(type, uri);
        JSONArray arr = new JSONArray();

        for (String line : text.split("\n")) {
            log.info("OCR line: {}", line);
            // filter special characters
            line = line.replaceAll("[^\\p{Alnum}^\\p{Space}]+", "");
            // filter numeric values, left only alpha characters
            line = line.replaceAll("[^A-Za-z]+", " ");
            line = line.trim();
            if (line.equals("")) {
                log.info("OCR: the line is empty after filtering");
                continue;
            }
            log.info("OCR filtered line: {}", line);

            String correctedSentence = spellChecker
                .getCorrectedText(line.toLowerCase());
            JSONObject dish = new JSONObject();
            dish.put("name", correctedSentence);
            arr.put(dish);
        }
        return arr.length() == 0 ? null : arr;
    }
}