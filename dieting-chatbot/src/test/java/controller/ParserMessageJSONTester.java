package controller;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;

import static reactor.bus.selector.Selectors.$;

@Slf4j
@RunWith(SpringRunner.class)
public class ParserMessageJSONTester {
    @Test
    public void testSetValidField() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("state", "Idle")
           .set("userId", "szhouan")
           .set("replyToken", "20202020");
        assert psr.get("state").equals("Idle");
        assert psr.get("userId").equals("szhouan");
        assert psr.get("replyToken").equals("20202020");
        log.info(psr.toString());
    }

    @Test
    public void testInvalidField() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("foo", "bar");
        psr.set("fuz", "baz");
        assert psr.get("foo") == null;
        assert psr.get("fuz") == null;
        log.info(psr.toString());
    }

    @Test
    public void testInvalidValue() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("state", "Idle");
        assert psr.get("state").equals("Idle");
        psr.set("state", "InvalidState");
        assert psr.get("state").equals("Idle");
        psr.set("state", "RecordMeal");
        assert psr.get("state").equals("RecordMeal");
        log.info(psr.toString());
    }

    @Test
    public void testTextMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("state", "Idle")
           .set("userId", "szhouan")
           .set("replyToken", "30406040")
           .setTextMessage("1080", "This is a message");
        assert psr.getMessageType().equals("text");
        assert psr.getTextContent().equals("This is a message");
        assert psr.getImageContent() == null;
        log.info(psr.toString());
    }

    @Test
    public void testImageMessage() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("state", "Idle")
           .set("userId", "szhouan")
           .set("replyToken", "30406040")
           .setImageMessage("4096");
        assert psr.getMessageType().equals("image");
        assert psr.getImageContent().equals("4096");
        assert psr.getTextContent() == null;
        log.info(psr.toString());
    }

    /*
    @Test
    public void testPublish() {
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("state", "AskMeal")
           .set("userId", "Hartshorne")
           .set("replyToken", "0x02589323")
           .setTextMessage("2017", "A text message");
        publisher.publish(psr);
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert psr.toString().equals(dbg.parserMessageJSON.toString());
    }
    */

    @Test
    public void testConstruct() {
        try {
            ParserMessageJSON psr = new ParserMessageJSON();
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}