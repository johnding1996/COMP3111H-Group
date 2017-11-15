package utility;

import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator: user input validation.
 * @author szhouan
 * @version v1.0.0
 */
@Slf4j
public class Validator {
    /**
     * Check whether a String is an interger.
     * @param str Input string.
     * @return whether the string is an integer.
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
     * check whether a String represents gender.
     * @param str Input string.
     * @return whether the string is a gender.
     */
    public static boolean isGender(String str) {
        List<String> words = TextProcessor.getTokens(str);
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
     * check whether integer is valid age.
     * @param age An integer representing age.
     * @return whether the age is valid.
     */
    public static boolean validateAge(int age) {
        return age >= 5 && age <= 95;
    }

    /**
     * check whether integer is valid weight.
     * @param weight An integer representing weight.
     * @return whether the weight is valid.
     */
    public static boolean validateWeight(int weight) {
        return weight >= 25 && weight <= 225;
    }

    /**
     * check whether integer is valid height.
     * @param height An integer representing height.
     * @return whether the height is valid.
     */
    public static boolean validateHeight(int height) {
        return height >= 25 && height <= 225;
    }

    /**
     * check whether a String is a date after today.
     * @param str Input String.
     * @param format expected date format.
     * @return whether the string is a date after today.
     *         return false if input format is incorrect.
     */
    public static boolean isFutureDate(String str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setLenient(false);
            Date future = sdf.parse(str);
            Date today = new Date();
            log.info(future.toString());
            log.info(today.toString());
            return today.compareTo(future) <= 0;
        } catch(ParseException pe) {
            return false;
        }
    }
}