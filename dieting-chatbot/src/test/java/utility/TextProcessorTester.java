package utility;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class TextProcessorTester {
    @Test
    public void testSentenceToWord1() {
        String sentence = "Hello! World!";
        List<String> words = TextProcessor.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 2;
        assert words.get(0).equals("hello");
        assert words.get(1).equals("world");
    }

    @Test
    public void testSentenceToWord2() {
        String sentence = "Hello!Wo?&rld!";
        List<String> words = TextProcessor.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 1;
        assert words.get(0).equals("helloworld");
    }

    @Test
    public void testSentenceToWord3() {
        String sentence = "\'This is a very, very long, sentence~'";
        List<String> words = TextProcessor.sentenceToWords(sentence);
        for (String word : words) {
            log.info(word);
        }
        assert words.size() == 7;
        assert words.get(0).equals("this");
        assert words.get(1).equals("is");
        assert words.get(2).equals("a");
        assert words.get(3).equals("very");
        assert words.get(4).equals("very");
        assert words.get(5).equals("long");
        assert words.get(6).equals("sentence");
    }
}