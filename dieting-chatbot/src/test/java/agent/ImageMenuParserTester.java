package agent;

import lombok.extern.slf4j.Slf4j;
import utility.JazzySpellChecker;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.junit4.SpringRunner;

import controller.ImageControl;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ImageMenuParser.class, JazzySpellChecker.class, ImageControl.class})
public class ImageMenuParserTester {

    private static final String sampleMenu = "sample-menu.png";
    private static final String sampleMenu2 = "sample-menu2.jpg";

    @Autowired
    private ImageMenuParser imageMenuParser;
    
    @Test
    public void testConstruct() {
        assert imageMenuParser != null;
    }

    @Test
    public void buildMenuTester() {
        String fileUrl = this.getClass().getClassLoader()
            .getResource(sampleMenu).toString(); 
        JSONArray arr = imageMenuParser.buildMenu(fileUrl);
        log.info("get menu JSON Array: {}", arr);
        assert arr.getJSONObject(0).getString("name").toLowerCase()
            .contains("spicy bean cured law minced pork served law rice");
    }
    
    @Test
    public void addBorderTester() {
        String outputFileUri = ImageControl.addBorder(new File(this.getClass()
            .getClassLoader().getResource(sampleMenu).getFile()), "test");
        log.info("Output border file with uri: {}", outputFileUri);
    }

    @Test
    public void borderedOCRTester() {
        String outputFileUri = ImageControl.addBorder(new File(this.getClass()
            .getClassLoader().getResource(sampleMenu2).getFile()), "test"); 
        JSONArray arr = imageMenuParser.buildMenu(outputFileUri);
        log.info("get menu JSON Array after bordered: {}", arr);
        assert arr.getJSONObject(0).getString("name").toLowerCase()
            .contains("ah");
    }


    
}