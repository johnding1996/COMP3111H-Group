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
import org.languagetool.JLanguageTool;
import org.languagetool.Language;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.language.BritishEnglish;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;

/**
 * ImageMenuParser: parser image into JSONArray using OCR and SpellChecker.
 * @author agong, szhouan
 * @version v1.0.0
 */
@Slf4j
@Component
public class ImageMenuParser {
    @Autowired
    private JazzySpellChecker spellChecker;

    /**
     * Parse JSON object from Image uri.
     * @param type can either be uri or path
     * @param uri URL or path string to use 
     * @return A JSON array
     */
    public static String readJsonFromImage(String type, String uri) {
        log.info("Entering readJsonFromImage");
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
            } catch (MalformedURLException e) {
                log.info("The URL is not valid.");
            }
            s = ocr.recognize(new URL[] { url }, Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        }
        else if (type.equals("path")) {
            s = ocr.recognize(new File[] { new File(uri) }, Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        }
        
        
        log.info("Result: " + s);
        // ocr more images here ...
        ocr.stopEngine();
        return s;
    }

    /**
     * Build menu information from JSON array.
     * @param type can either be uri or path, delegate to OCR
     * @param uri build menu from uri
     * @return A JSON array; null if no dish is successfully parsed
     */
    public JSONArray buildMenu(String type, String uri) {
        String text = "";
        log.info("Entering image menu parser build menu");
        text = readJsonFromImage(type, uri);
        JSONArray arr = new JSONArray();
        String[] lines = text.split("\n");

        for (String line : lines) {
            log.info("this line has content: {}", line);
            // parse out special characters
            line = line.replaceAll("[^\\p{Alnum}^\\p{Space}]+", "");
            // parse out numeric values, left only alpha characters
            line = line.replaceAll("[^A-Za-z]+", " ");
            log.info("after parse out number and special character: {}", line);
            if (line.equals("") || line.trim().length() <= 0)
                continue;

            String correctedSentence = spellChecker.getCorrectedText(line);
            JSONObject dish = new JSONObject();
            dish.put("name", correctedSentence);
            arr.put(dish);
        }
        return arr.length() == 0 ? null : arr;
    }
}