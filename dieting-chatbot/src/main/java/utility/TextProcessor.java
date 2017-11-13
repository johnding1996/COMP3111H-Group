package utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TextProcessor: text processing utilities.
 * @author szhouan
 * @version v1.0.0
 */
public class TextProcessor {
    /**
     * Get a list of words from a sentence.
     * @param sentence A sentence in String.
     * @return A list of lowercase word in String,
     *         ordered accordingly.
     *         Punctuation marks are discarded.
     */
    static public List<String> sentenceToWords(String sentence) {
        String[] words = sentence.split("\\s+");
        for (int i = 0; i < words.length; ++i) {
            words[i] = words[i].replaceAll("[^\\w]", "").toLowerCase();
        }
        return new ArrayList<String>(Arrays.asList(words));
    }
}