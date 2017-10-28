package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.Environment;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * ControllerFactory
 * 
 * Construct all beans
 * Register modules to channels
 */
@Slf4j
@Component
@Configuration
public class ControllerFactory {
    @Autowired
    private EventBus eventBus;

    @Autowired
    private DebugReceiver dbg;

    @Bean
    Environment env() {
        return Environment.initializeIfEmpty()
                          .assignErrorJournal();
    }
    
    @Bean
    EventBus createEventBus(Environment env) {
	    return EventBus.create(env, Environment.THREAD_POOL);
    }

    /**
     * register each module to its subscribed channel(s)
     */
    @PostConstruct
    public void registration() {
        eventBus.on($("ParserMessageJSON"), this.templateModule);
    }
 
    @Bean
    public Publisher createPublisher() {
        return new Publisher();
    }

    @Bean 
    public Formatter createFormatter() {
        return new Formatter();
    }

    @Bean
    public TemplateModule createTemplateModule() {
        return new TemplateModule();
    }

    @Bean
    public DebugReceiver createDebugReceiver() {
        return new DebugReceiver();
    }

    @Autowired
    private TemplateModule templateModule;
}
