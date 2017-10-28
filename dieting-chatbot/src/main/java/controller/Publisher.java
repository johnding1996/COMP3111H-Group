package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * Publisher
 * 
 * Event publish utility
 */
@Service
public class Publisher {
    @Autowired
    EventBus eventBus;

    public void publish(FormatterMessageJSON formatterMessageJSON) {
        eventBus.notify("FormatterMessageJSON", Event.wrap(formatterMessageJSON));
    }

    public void publish(ParserMessageJSON parserMessageJSON) {
        eventBus.notify("ParserMessageJSON", Event.wrap(parserMessageJSON));
    }
}
