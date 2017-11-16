package utility;

import java.util.Locale;

import org.junit.Before;
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

    @Before
    public void init() {
        Locale.setDefault(Locale.US);
    }

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
    public void testFood() {
        String text = "Spicy Bean Curd wllh Mlnced Pork served wllh RICe";
        assert spellChecker.getCorrectedText(text).toLowerCase()
            .equals("spicy bean curd with minced pork served with rice");
    }
}