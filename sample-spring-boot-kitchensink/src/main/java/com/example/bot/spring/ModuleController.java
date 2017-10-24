package com.example.bot.spring;

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

//@ComponentScan({"com.example.bot.spring.formatterPublisher", "com.example.bot.spring.Formatter", "com.example,bot.spring.ParserReceiver"})

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
    public FormatterPublisher fp() {
        return new FormatterPublisher();
    }

    @Bean 
    public Formatter f() {
        return new Formatter();
    }

    @Bean
    public ParserReceiver pr() {
        return new ParserReceiver();
    }

	@Autowired
    private EventBus eventBus;
    
    @Autowired
    private FormatterPublisher formatterPublisher;

    @Autowired
    private Formatter formatter;
    
    /*
    @Autowired
    private Parser parser;
    */

    @Autowired
    private ParserReceiver parserReceiver;

    public EventBus getEventBus() {
        return eventBus;
    }
    
    public FormatterPublisher getFormatterPublisher() {
        return formatterPublisher;
    }

    public Formatter getFormatter() {
        return formatter;
    }
    /*
    public Parser getParser() {
        return parser;
    }
    */
    public ParserReceiver getParserReceiver() {
        return parserReceiver;
    }
 
}
