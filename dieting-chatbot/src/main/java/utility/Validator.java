package utility;

import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class Validator {
    /**
     * Get a list of words from a sentence
     * @param sentence A sentence in String
     * @return A list of lowercase word in String,
     *         ordered accordingly
     *         Punctuation marks are discarded
     */
    static public List<String> sentenceToWords(String sentence) {
        String[] words = sentence.split("\\s+");
        for (int i = 0; i < words.length; ++i) {
            words[i] = words[i].replaceAll("[^\\w]", "").toLowerCase();
        }
        return new ArrayList<String>(Arrays.asList(words));
    }

    /**
     * Check whether a String is an interger
     * @param str Input string
     * @return whether the string is an integer
     */
    public static boolean isInteger(String str) {  
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * check whether a String represents gender
     * @param str Input string
     * @return whether the string is a gender
     */
    public static boolean isGender(String str) {
        List<String> words = sentenceToWords(str);
        for (String word : words) {
            switch (word) {
                case "male": case "female":
                case "man": case "woman":
                return true;
                
                default:
            }
        }
        return false;
    }

    /**
     * check whether integer is valid weight
     * @param weight An integer representing weight
     * @return whether the weight is valid
     */
    public static boolean validateWeight(int weight) {
        return weight >= 25 && weight <= 225;
    }

    /**
     * check whether integer is valid height
     * @param height An integer representing height
     * @return whether the height is valid
     */
    public static boolean validateHeight(int height) {
        return height >= 25 && height <= 225;
    }

    /**
     * check whether a String is a date after today
     * @param str Input String
     * @param format expected date format
     * @return whether the string is a date after today
     *         return false if input format is incorrect
     */
    public static boolean isFutureDate(String str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            Date future = sdf.parse(str);
            Calendar today = Calendar.getInstance();
            return !today.after(future);
        } catch(ParseException pe) {
            return false;
        }
    }
}