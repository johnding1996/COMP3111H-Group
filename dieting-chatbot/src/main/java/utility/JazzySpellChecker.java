package utility;

import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.swabunga.spell.event.DefaultWordFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JazzySpellChecker implements SpellCheckListener {
    private SpellChecker spellChecker;
    private List<String> misspelledWords = new ArrayList<>();
    private SpellDictionaryHashMap dictionaryHashMap;
    private static final String dictPath = "/static/english.txt";

    /**
     * Constructor of JazzySpellChecker with given dictionary.
     */
    public JazzySpellChecker() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(this.getClass()
                .getResourceAsStream(dictPath));
            br = new BufferedReader(isr);
            dictionaryHashMap = new SpellDictionaryHashMap(br);
        } catch (FileNotFoundException e) {
            log.info(e.toString());
        } catch (IOException e) {
            log.info(e.toString());
        } finally {
            try {
                if (br != null) br.close();
                if (isr != null) isr.close();
            } catch (IOException e) {
                log.info("IOException while closing file: {}", e.toString());
			}
        }
        spellChecker = new SpellChecker(dictionaryHashMap);
        spellChecker.addSpellCheckListener(this);
    }

    /**
     * Correct the misspelled words in the input string.
     * @param text Input text
     * @return corrected input text
     */
    public String getCorrectedText(String text) {
        text = text.toLowerCase();
        log.info("SPELL_CHECKER: {}", text);
        StringWordTokenizer tk = new StringWordTokenizer(text,
            new DefaultWordFinder());
        spellChecker.checkSpelling(tk);

        for (String word : misspelledWords) {
            List<String> suggestions = getSuggestions(word);
            if (suggestions.size() == 0) {
                log.info("No suggestion for misspelled word {}", word);
                continue;
            }
            String bestSuggestion = suggestions.get(0);
            text = text.replace(word, bestSuggestion);
        }
        log.info("SPELL_CHECKER: {}", text);
        return text;
    }

    /**
     * Get a list of suggestions for a misspelled word.
     * @param misspelledWord The misspelled word for suggestions
     * @return A list of suggestions
     */
    public List<String> getSuggestions(String misspelledWord) {
        @SuppressWarnings("unchecked")
        List<Word> words = spellChecker.getSuggestions(misspelledWord, 0);
        List<String> suggestions = new ArrayList<String>();
        for (Word suggestion : words) {
            suggestions.add(suggestion.getWord());
        }
        return suggestions;
    }

    /**
     * Event listener for spell check event.
     * @param event A SpellCheckEvent
     */
    @Override
    public void spellingError(SpellCheckEvent event) {
        event.ignoreWord(true);
        misspelledWords.add(event.getInvalidWord());
    }
}