package controller;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.bus.EventBus;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;

/**
 * Publisher.
 * Event publish utility.
 * @author szhouan
 * @version v1.1.0
 */
@Service
public class Publisher {
    @Autowired
    private EventBus eventBus;

    /**
     * Publish a FormatterMessageJSON object.
     * @param formatterMessageJSON The object to publish
     */
    public void publish(FormatterMessageJSON formatterMessageJSON) {
        eventBus.notify("FormatterMessageJSON", Event.wrap(formatterMessageJSON));
    }

    /**
     * Publish a ParserMessageJSON object.
     * @param parserMessageJSON The object to publish
     */
    public void publish(ParserMessageJSON parserMessageJSON) {
        eventBus.notify("ParserMessageJSON", Event.wrap(parserMessageJSON));
    }
}
