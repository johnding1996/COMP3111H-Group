package utility;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.Word; 
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

/**
 * TextProcessor: text processing utilities.
 * @author szhouan
 * @version v1.0.0
 */
public class TextProcessor {
    private static Pattern pattern = Pattern.compile("\\p{Punct}");

    /**
     * Get a list of words from a sentence.
     * @param sentence A sentence in String.
     * @return A list of lowercase words in String,
     *         ordered accordingly.
     *         Punctuation marks are discarded.
     */
    public static List<String> sentenceToWords(String sentence) {
        String[] words = sentence.split("\\s+");
        for (int i = 0; i < words.length; ++i) {
            words[i] = words[i].replaceAll("[^\\w]", "").toLowerCase();
        }
        return new ArrayList<String>(Arrays.asList(words));
    }

    /**
     * Get a list of words from a sentence using PTBTokenizer.
     * @param sentence A sentence in String.
     * @return A list of lowercase tokens in String.
     */
    public static List<String> getTokens(String sentence) {
        PTBTokenizer<Word> ptbt = new PTBTokenizer<>(
            new StringReader(sentence), new WordTokenFactory(), ""
        );
        List<String> words = new ArrayList<>();
        while (ptbt.hasNext()) {
            String word = ptbt.next().word();
            if (!pattern.matcher(word).matches()) {
                words.add(word.toLowerCase());
            }
        }
        return words;
    }
}