package com.example.bot.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.bus.Event;
import reactor.fn.Consumer;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.json.JsonObject;

@Service
class TemplateModule implements Consumer<Event<ParserMessageJSON> > {

    @Autowired
    ParserMessageJSON parserMessageJSON;

    @Autowired
    Publisher publisher;
    
	public void accept(Event<ParserMessageJSON> ev) {
        this.parserMessageJSON = ev.getData();
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
    public JsonObject getMessage() {
        return parserMessageJSON.getMessage();
    }
    public Publisher getPublisher() {
        return publisher;
    }
}