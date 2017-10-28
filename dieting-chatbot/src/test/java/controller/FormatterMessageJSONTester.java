package controller;

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
@SpringBootTest(classes = { FormatterMessageJSON.class })
public class FormatterMessageJSONTester {
    @Test
    public void testSetValidField() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("type", "push")
           .set("userId", "agong")
           .set("replyToken", "1234")
           .set("stateTransition", "foo");
        assert fmt.get("type").equals("push");
        assert fmt.get("userId").equals("agong");
        assert fmt.get("replyToken").equals("1234");
        assert fmt.get("stateTransition").equals("foo");
        log.info(fmt.toString());
    }

    @Test
    public void testInvalidField() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("foo", "bar");
        fmt.set("fuz", "baz");
        assert fmt.get("foo") == null;
        assert fmt.get("fuz") == null;
        log.info(fmt.toString());
    }

    @Test
    public void testInvalidValue() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("type", "push");
        assert fmt.get("type").equals("push");
        fmt.set("type", "Hello");
        assert fmt.get("type").equals("push");
        fmt.set("type", "reply");
        assert fmt.get("type").equals("reply");
        log.info(fmt.toString());
    }

    @Test
    public void testAppendTextMessage() {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", "szhouan")
           .appendTextMessage("Hello world")
           .appendTextMessage("Java");
        JSONArray messages = (JSONArray)fmt.get("messages");
        assert messages.getJSONObject(0).get("textContent").equals("Hello world");
        assert messages.getJSONObject(1).get("textContent").equals("Java");
        log.info(fmt.toString());
    }
}