package utility;

import java.io.StringReader;
import java.util.*;
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

    /**
     * private Pattern object.
     */
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

    /**
     * Try parsing an English number to numeric number.
     * @param sentence input sentence probably is a english number
     * @return string of numeric number
     */
    public static String sentenceToNumber(String sentence) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("hundred", "");
        mapping.put("and", "");
        mapping.put("zero", "0");
        mapping.put("one", "1");
        mapping.put("two", "2");
        mapping.put("three", "3");
        mapping.put("four", "4");
        mapping.put("five", "5");
        mapping.put("six", "6");
        mapping.put("seven", "7");
        mapping.put("eight", "8");
        mapping.put("nine", "9");
        mapping.put("ten", "1");
        mapping.put("eleven", "11");
        mapping.put("twelve", "12");
        mapping.put("thirteen", "13");
        mapping.put("fourteen", "14");
        mapping.put("fifteen", "15");
        mapping.put("sixteen", "16");
        mapping.put("seventeen", "17");
        mapping.put("eighteen", "18");
        mapping.put("nineteen", "19");
        mapping.put("twenty", "2");
        mapping.put("thirty", "3");
        mapping.put("forty", "4");
        mapping.put("fifty", "5");
        mapping.put("sixty", "6");
        mapping.put("seventy", "7");
        mapping.put("eighty", "8");
        mapping.put("ninety", "9");
        List<String> words = getTokens(sentence);
        List<String> numbers = new ArrayList<>();
        for (String word : words) {
            if (!mapping.keySet().contains(word)) return null;
            numbers.add(mapping.get(word));
        }
        return String.join("", numbers);
    }

    /**
     * Utility function for deciding whether two String iterable has one match.
     * @param it1 Input that needs to be matched
     * @param it2 Template that input matched against
     * @return matched item in it2, or null if no match found
     */
    public static String getMatch(Iterable<String> it1, Iterable<String> it2) {
        for (String s1 : it1) for (String s2 : it2) {
            if (s1.equals(s2)) return s2;
        }
        return null;
    }
}