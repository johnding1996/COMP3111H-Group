package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.Environment;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;

/**
 * ContextManager
 * 
 * Construct all beans
 * Register modules to channels
 */
@Component
public class ModuleController {
    
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
    public void registration() {
        this.eventBus.on($("ParserMessageJSON"), this.templateModule);
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
    public TemplateModule template() {
        return new TemplateModule();
    }

	@Autowired
    private EventBus eventBus;
    
    @Autowired
    private Publisher publisher;

    @Autowired
    private Formatter formatter;
    
    @Autowired
    private TemplateModule templateModule;

    public EventBus getEventBus() {
        return eventBus;
    }
    
    public Publisher getPublisher() {
        return publisher;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    public TemplateModule getTemplateModule() {
        return templateModule;
    }

}
