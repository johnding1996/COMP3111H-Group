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

    @Test
    public void buildMenuTester() {
        String fileUrl = this.getClass().getClassLoader()
            .getResource(sampleMenu).toString(); 
        JSONArray arr = ImageMenuParser.buildMenu("uri", fileUrl);
        log.info("get menu JSON Array: {}", arr);
        assert arr.getJSONObject(0).getString("name").toLowerCase()
            .contains("spicy bean cured law minced pork served law rice");
    }
    
    @Test
    public void addBorderTester() {
        File file = new File(this.getClass().getClassLoader().getResource(sampleMenu).getFile());
        log.info("FILE: {}", file.toString());
        String outputFileUri = ImageControl.addBorder(file, "test"); 
        JSONArray arr = ImageMenuParser.buildMenu("path", outputFileUri);
        log.info("get menu JSON Array for menu 1 after bordered: {}", arr);
        log.info("Output border file for menu 1 with uri: {}", outputFileUri);
    }
}