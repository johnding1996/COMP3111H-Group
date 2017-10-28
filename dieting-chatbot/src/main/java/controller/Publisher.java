package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
