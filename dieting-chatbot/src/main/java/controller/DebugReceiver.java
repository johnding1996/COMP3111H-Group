package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static reactor.bus.selector.Selectors.$;

import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DebugReceiver implements Consumer<Event<MessageJSON>> {

    @Autowired
    private EventBus eventBus;

    @Autowired(required=false)
    private Publisher publisher;

    public FormatterMessageJSON formatterMessageJSON = null;
    public ParserMessageJSON parserMessageJSON = null;
    public void accept(Event<MessageJSON> ev) {
        MessageJSON json = ev.getData();
        if (json instanceof FormatterMessageJSON) {
            formatterMessageJSON = (FormatterMessageJSON)json;
        }
        if (json instanceof ParserMessageJSON) {
            parserMessageJSON = (ParserMessageJSON)json;
            if (parserMessageJSON.getMessageType().equals("text"))
                echoTextMessage(parserMessageJSON);
        }
        log.info("\nDEBUGGER:\n" + ev.getData().toString());
    }

    @PostConstruct
    public void init() {
        eventBus.on($("FormatterMessageJSON"), this);
        eventBus.on($("ParserMessageJSON"), this);
    }

    private void echoTextMessage(ParserMessageJSON psr) {
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", psr.get("userId"))
           .set("type", "reply")
           .set("replyToken", psr.get("replyToken"))
           .appendTextMessage(psr.getTextContent());
        log.info("DEBUGGER: Echo message");
        log.info(fmt.toString());
        publisher.publish(fmt);
    }
}