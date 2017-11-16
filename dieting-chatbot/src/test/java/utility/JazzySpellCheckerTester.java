package utility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {JazzySpellChecker.class})
public class JazzySpellCheckerTester {
    @Autowired
    private JazzySpellChecker spellChecker;

    @Test
    public void testConstruct() {
        assert spellChecker != null;
    }

    @Test
    public void testSpellChecking1() {
        String text = "A peice of tetx. Tyr to corect me.";
        assert spellChecker.getCorrectedText(text).toLowerCase()
            .equals("a piece of text. try to correct me.");
    }

    @Test
    public void testSpellChecking2() {
        String text = "Helllo woorld\nnow of ne.";
        assert spellChecker.getCorrectedText(text).toLowerCase()
            .equals("hello world\nnow of new.");
    }

    @Test
    public void testSpellChecking3() {
        String text = "Helllo woorld\nnow of ne.";
        spellChecker.getCorrectedText("Teesst Teesst Teest");
        assert spellChecker.getCorrectedText(text).toLowerCase()
            .equals("hello world\nnow of new.");
    }
}