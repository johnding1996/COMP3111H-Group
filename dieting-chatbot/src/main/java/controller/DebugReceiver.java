package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import reactor.fn.Consumer;
import reactor.bus.Event;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DebugReceiver implements Consumer<Event<FormatterMessageJSON>> {
    @Autowired
    private EventBus eventBus;

    //FormatterMessageJSONReceiver fmt = new FormatterMessageJSONReceiver();
    // @Service
    // class FormatterMessageJSONReceiver
    //     implements Consumer<Event<FormatterMessageJSON>> {
        public FormatterMessageJSON json;
        public void accept(Event<FormatterMessageJSON> ev) {
            json = ev.getData();
        }
    // }

    public void init() {
        eventBus.on($("FormatterMessageJSON"), this);
    }
}