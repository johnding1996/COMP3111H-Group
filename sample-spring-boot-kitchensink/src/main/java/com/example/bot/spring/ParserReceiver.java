package com.example.bot.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.bus.Event;
import reactor.fn.Consumer;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Service
class ParserReceiver implements Consumer<Event<ParserMessageJSON> > {

    ParserMessageJSON parserMessageJSON;
    
    @Autowired
	CountDownLatch latch;

	public void accept(Event<ParserMessageJSON> ev) {
        this.parserMessageJSON = ev.getData();
        latch.countDown();
	}
	
	public String getUserId() {
        return parserMessageJSON.getUserId();
    }
    public String getState() {
        return parserMessageJSON.getState();
    }
    public String getReplyToken() {
        return parserMessageJSON.getReplyToken();
    }
    public List<MsgJSON> getMessages() {
        return parserMessageJSON.getMessages();
    }
}