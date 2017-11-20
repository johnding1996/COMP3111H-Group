package controller;

import lombok.extern.slf4j.Slf4j;
import utility.JazzySpellChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.junit4.SpringRunner;
import com.linecorp.bot.client.MessageContentResponse;
import controller.ImageControl;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ImageControl.class})
public class ImageControlTester {

    private static final String sampleMenu = "sample-menu.png";
    private static final String sampleMenu2 = "sample-menu2.jpg";

    @Before
    public void init() {
        DietingChatbotApplication.downloadedContentDir = Paths.get(this.getClass().getClassLoader().getResource(sampleMenu)
        .getPath()).getParent();
    }

    @Test
    public void downloadedContentTester() {
        ImageControl.DownloadedContent i = new ImageControl.DownloadedContent(Paths.get(this.getClass().getClassLoader().getResource(sampleMenu)
        .getPath()), "haha");
    }

    @Test
    public void addBorderOnTestTester() {
        File file = new File(this.getClass().getClassLoader().getResource(sampleMenu).getFile());
        log.info("FILE: {}", file.toString());
        String outputFileUri = ImageControl.addBorder(file, "test");  
    }
    
    @Test
    public void addBorderOnServerTester() {
        File file = new File(this.getClass().getClassLoader().getResource(sampleMenu).getFile());
        log.info("FILE: {}", file.toString());
        String outputFileUri = ImageControl.addBorder(file, "server"); 
    }

    @Test
    public void saveContentTester() {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(this.getClass().getClassLoader().getResource(sampleMenu).getFile());
        }
        catch(Exception e) {
            log.info("GG");
        }
        MessageContentResponse mcr = MessageContentResponse.builder().mimeType("image/jpeg").stream(fi).build();
        ImageControl.saveContent(mcr, "DB");
    }
}