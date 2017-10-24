package com.example.bot.spring;

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
public class FormatterPublisher {

  
	@Autowired
	EventBus eventBus; 
	//= EventBus.create(Environment.initializeIfEmpty().assignErrorJournal(), Environment.THREAD_POOL);

	//@Autowired
	//CountDownLatch latch = new CountDownLatch(10); 

	public void publishFormatterMessageJSON(FormatterMessageJSON formatterMessageJSON) throws InterruptedException {

        eventBus.notify("FormatterMessageJSON", Event.wrap(formatterMessageJSON));
        
		//latch.await();

	}

}
