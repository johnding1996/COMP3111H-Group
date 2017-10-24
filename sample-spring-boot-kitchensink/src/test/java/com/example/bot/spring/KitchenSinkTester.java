package com.example.bot.spring;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.MessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.LineBotMessages;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.Environment;
import reactor.bus.EventBus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static reactor.bus.selector.Selectors.$;

import com.example.bot.spring.DatabaseEngine;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { KitchenSinkTester.class, ModuleController.class, ParserMessageJSON.class, MsgJSON.class, TextMessageJSON.class, FormatterMessageJSON.class})
public class KitchenSinkTester {

	
	@Autowired
	private ParserMessageJSON PM;

	@Autowired
	private FormatterMessageJSON FM = new FormatterMessageJSON();
	
	@Autowired
	TextMessageJSON msg = new TextMessageJSON();

	@Autowired
	private ModuleController moduleController;

	@Test
	public void testParserMessageJSON() throws Exception {
		this.PM.setUserId("abc");
		String id = this.PM.getUserId();
		assertThat(id).isEqualTo("abc");
	}

	@Test
	public void testFormatterMessageJSON() throws Exception {
		this.FM.setUserId("agong");
		this.FM.setType("text");
		this.FM.getMessages().add(this.msg);
	}

	@Test
	public void testMsgJSON() throws Exception {
		msg.setId("1");
		msg.setType("text");
		msg.setTextContent("def");
		this.PM.getMessages().add(this.msg);
		//log.info("Array: {}", this.PM.getMessages());
		//assertThat(this.PM.getMessages()).isEqualTo("[{text, 1, def}]");
	}

	@Test
	public void testFormatter() {
		this.FM.setUserId("agong");
		this.moduleController.getEventBus().on($("FormatterMessageJSON"), this.moduleController.getFormatter());
		//assertThat(this.moduleController.getFormatterPublisher()).isEqualTo(null);
		
		try {
			this.moduleController.getFormatterPublisher().publishFormatterMessageJSON(this.FM);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//assertThat(this.moduleController.getFormatter().getFormatterMessageJSON()).isEqualTo(null);
		assertThat(this.moduleController.getFormatter().getFormatterMessageJSON().getUserId()).isEqualTo("agong");
		
	}
	
	
}
