package controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.Environment;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingClientImpl;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * ControllerFactory
 * 
 * Construct all beans
 * Register modules to channels
 */
@Slf4j
// @Configuration
public class ControllerFactory {
    // @Bean
    // public Publisher createPublisher() {
    //     return new Publisher();
    // }

    // @Bean 
    // public Formatter createFormatter() {
    //     return new Formatter();
    // }

    // @Bean @Primary
    // public DebugReceiver createDebugReceiver() {
    //     return new DebugReceiver();
    // }

    // @Bean
    // public Controller createController() {
    //     return new Controller();
    // }
}
