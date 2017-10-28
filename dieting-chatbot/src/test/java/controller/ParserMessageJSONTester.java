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
@SpringBootTest(classes = { ParserMessageJSON.class, ControllerFactory.class })
public class ParserMessageJSONTester {
    @Autowired
    private DebugReceiver dbg;

    @Autowired
    private Publisher publisher;

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
}