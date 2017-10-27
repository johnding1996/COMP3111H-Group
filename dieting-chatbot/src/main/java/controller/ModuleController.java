package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import reactor.Environment;
import reactor.bus.EventBus;
import com.google.common.io.ByteStreams;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static reactor.bus.selector.Selectors.$;

//@ComponentScan({"com.example.bot.spring.formatterPublisher", "com.example.bot.spring.Formatter", "com.example,bot.spring.TemplateModule"})

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

    @Bean
	public CountDownLatch latch() {
		return new CountDownLatch(10);
	}

    @Bean
    public Publisher commonPublisher() {
        return new Publisher();
    }

    @Bean 
    public Formatter controllerFormatter() {
        return new Formatter();
    }

    @Bean
    public TemplateModule template() {
        return new TemplateModule();
    }

	@Autowired
    private EventBus eventBus;
    
    @Autowired
    private Publisher Publisher;

    @Autowired
    private Formatter formatter;
    
    /*
    @Autowired
    private Parser parser;
    */

    @Autowired
    private TemplateModule templateModule;

    public EventBus getEventBus() {
        return eventBus;
    }
    
    public Publisher getPublisher() {
        return Publisher;
    }

    public Formatter getFormatter() {
        return formatter;
    }
    /*
    public Parser getParser() {
        return parser;
    }
    */
    public TemplateModule getTemplateModule() {
        return templateModule;
    }

    public void registration() {
        this.eventBus.on($("ParserMessageJSON"), this.templateModule);
    }
 
}
