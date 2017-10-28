package controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import com.linecorp.bot.model.profile.UserProfileResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.AudioMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import reactor.Environment;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;
import java.net.URI;

/**
 * TODO: copy other handler
 */
@Slf4j
@LineMessageHandler
@Component
public class Controller {
    private HashMap<String, StateMachine> stateMachines;

    @Autowired
    private Publisher publisher;

    @Autowired
    private DebugReceiver dbg;

    @Autowired
    private EventBus eventBus;

    // @Bean
    // Environment env() {
    //     return Environment.initializeIfEmpty()
    //                       .assignErrorJournal();
    // }
    
    // @Bean
    // EventBus createEventBus(Environment env) {
    //     return EventBus.create(env, Environment.THREAD_POOL);
    // }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event)
        throws Exception {

        handleTextContent(event.getReplyToken(), event,
            event.getMessage(), event.getMessage().getId());
    }

    // @EventMapping
    // public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event)
    //     throws IOException {

    //     final MessageContentResponse response;
    //     DownloadedContent jpg = saveContent("jpg", response);
    //     handleImageContent(event.getReplyToken(), event,
    //         event.getMessage().getId());    
    // }

    /**
     * Event Handler for Text
     */
    private void handleTextContent(String replyToken, Event event,
        TextMessageContent content, String id) throws Exception {

        // ParserMessageJSON parserMessageJSON = new ParserMessageJSON();
        // parserMessageJSON.set("userId", event.getSource().getUserId())
        //     .set("state", "Idle").set("replyToken", replyToken)
        //     .setTextMessage(id, content.getText());
        log.info("Handling text info from {}", id);
        // publisher.publish(parserMessageJSON);
    }

    /**
     * Event Handler for Image
     */
    private void handleImageContent(String replyToken, Event event, String id) {
        ParserMessageJSON parserMessageJSON = new ParserMessageJSON();
        parserMessageJSON.set("userId", event.getSource().getUserId())
            .set("state", "Idle").set("replyToken", replyToken)
            .setImageMessage(id);
        publisher.publish(parserMessageJSON);
    }


    static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).build().toUriString();
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} => {}", Arrays.toString(args), i);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
        log.info("Got content-type: {}", responseBody);

        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.getStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-'
            + UUID.randomUUID().toString() + '.' + ext;
        Path tempFile = DietingChatbotApplication.downloadedContentDir
            .resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }

    // @Bean
    // public Publisher createPublisher() {
    //     return new Publisher();
    // }

    // @Bean 
    // public Formatter createFormatter() {
    //     return new Formatter();
    // }

    // @Bean
    // public TemplateModule createTemplateModule() {
    //     return new TemplateModule();
    // }

    // @Bean
    // public DebugReceiver createDebugReceiver() {
    //     return new DebugReceiver();
    // }
}
