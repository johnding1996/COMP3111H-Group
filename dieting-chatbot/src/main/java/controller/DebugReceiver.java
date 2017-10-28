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
public class DebugReceiver
    implements Consumer<Event<FormatterMessageJSON>> {

    @Autowired
    private EventBus eventBus;

    public FormatterMessageJSON formatterMessageJSON;
    public void accept(Event<FormatterMessageJSON> ev) {
        formatterMessageJSON = ev.getData();
        log.info("\nDEBUGGER:\n" + ev.getData().toString());
    }

    @PostConstruct
    public void init() {
        eventBus.on($("FormatterMessageJSON"), this);
    }
}