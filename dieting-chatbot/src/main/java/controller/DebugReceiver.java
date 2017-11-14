package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static reactor.bus.selector.Selectors.$;

import reactor.fn.Consumer;
import utility.MessageJSON;
import utility.ParserMessageJSON;
import utility.FormatterMessageJSON;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * DebugReceiver: listen to MessageJSON event and log the message.
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
@Service
public class DebugReceiver implements Consumer<Event<MessageJSON>> {
    @Autowired
    private EventBus eventBus;


    /**
     * FormatterMessageJSON object for debug usage.
     */

    public FormatterMessageJSON formatterMessageJSON = null;
    /**
     * ParserMessageJSON object for debug usage.
     */
    public ParserMessageJSON parserMessageJSON = null;

    /**
     * EventBus ParserMessageJSON and FormatterMessageJSON event handler.
     * @param ev MessageJSON event
     */
    public void accept(Event<MessageJSON> ev) {
        MessageJSON json = ev.getData();
        if (json instanceof FormatterMessageJSON) {
            formatterMessageJSON = (FormatterMessageJSON)json;
        }
        if (json instanceof ParserMessageJSON) {
            parserMessageJSON = (ParserMessageJSON)json;
        }
        log.info("\nDEBUGGER:\n" + json.toString());
    }

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        eventBus.on($("FormatterMessageJSON"), this);
        eventBus.on($("ParserMessageJSON"), this);
    }
}