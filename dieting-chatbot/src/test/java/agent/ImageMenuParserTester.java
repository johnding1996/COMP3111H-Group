package agent;

import lombok.extern.slf4j.Slf4j;
import utility.JazzySpellChecker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ImageMenuParser.class, JazzySpellChecker.class})
public class ImageMenuParserTester {

    private static final String sampleMenuPath = "/static/sample_menu_1.png";

    @Autowired
    private ImageMenuParser imageMenuParser;

    @Test
    public void test() {
        assert imageMenuParser != null;
    }

    // @Test
    // public void buildMenuTester() {
    //     imageMenuParser.buildMenu();

    // }



    
}