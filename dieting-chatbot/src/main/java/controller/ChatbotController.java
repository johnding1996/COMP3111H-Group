package controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
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

import utility.TextProcessor;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import reactor.Environment;
import reactor.bus.EventBus;

import static reactor.bus.selector.Selectors.$;

import javax.annotation.PostConstruct;
import java.net.URI;

import utility.Validator;

@Slf4j
@Service
@LineMessageHandler
public class ChatbotController
    implements reactor.fn.Consumer<reactor.bus.Event<
        FormatterMessageJSON>> {

    private HashMap<String, StateMachine> stateMachines = new
        HashMap<String, StateMachine>();

    @Autowired(required = false)
    private LineMessagingClient lineMessagingClient;

    @Autowired(required = false)
    private Publisher publisher;

    @Autowired
    private Formatter formatter;

    @Autowired(required=false)
    private EventBus eventBus;

    /**
     * Register on eventBus
     */
    @PostConstruct
    public void init() {
        log.info("Register for FormatterMessageJSON");
        try {
            eventBus.on($("FormatterMessageJSON"), this);
        } catch (Exception e) {
            log.info("Failed to register on eventBus: " +
                e.toString());
        }
    }

    /**
     * EventBus FormatterMessageJSON event handle
     * @param ev FormatterMessageJSON event
     */
    public void accept(reactor.bus.Event<FormatterMessageJSON> ev) {
        FormatterMessageJSON formatterMessageJSON = ev.getData();
        log.info("\nChatbotController:\n" + formatterMessageJSON.toString());
        
        /* Handle state transition if any */
        if (formatterMessageJSON.get("stateTransition") != null) {
            String userId = (String)formatterMessageJSON.get("userId");
            String transition = (String)formatterMessageJSON.get("stateTransition");
            StateMachine stateMachine = getStateMachine(userId);
            log.info("User {} triggers state transition {}",
                userId, transition);
            stateMachine.toNextState(transition);
        }
        if (!formatterMessageJSON.get("type").equals("transition"))
            formatter.sendMessage(formatterMessageJSON);
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event)
        throws Exception {

        String userId = event.getSource().getUserId();
        String replyToken = event.getReplyToken();
        String textContent = event.getMessage().getText();
        String messageId = event.getMessage().getId();

        StateMachine stateMachine = getStateMachine(userId);
        String state = stateMachine.getState();

        /* update state */
        if (state.equals("Idle")) {
            if (isRecommendationRequest(textContent)) {
                stateMachine.toNextState("recommendationRequest");
            } else if (isInitialInputRequest(textContent)) {
                stateMachine.toNextState("initialInputRequest");
            } else if (isFeedbackRequest(textContent)) {
                stateMachine.toNextState("feedbackRequest");
            }
            state = stateMachine.getState();
            log.info("State transition handled by Controller");
            log.info("userId={}, newState={}", userId, state);
        }
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", userId)
           .set("state", state)
           .set("replyToken", replyToken)
           .setTextMessage(messageId, textContent);
        publisher.publish(psr);
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event)
        throws IOException {

        final MessageContentResponse response;
        String messageId = event.getMessage().getId();
        String replyToken = event.getReplyToken();
        try {
            response = lineMessagingClient.getMessageContent(messageId).get();
        } catch (InterruptedException | ExecutionException e) {
            log.info("Cannot get image: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        DownloadedContent jpg = saveContent("jpg", response);
        handleImageContent(replyToken, event, messageId);
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

    /* ------------------------ LOGIC START ------------------------ */
    /**
     * Clear all state machines
     */
    public void clearStateMachines() {
        stateMachines.clear();
        log.info("Clear all state machines");
    }

    /**
     * Get state machine corresponding to the user
     * @param userId String of user id
     * @return StateMachine
     *         If there is no record before, initialize to Idle
     */
    public StateMachine getStateMachine(String userId) {
        if (!stateMachines.containsKey(userId)) {
            stateMachines.put(userId, new StateMachine(userId));
        }
        return stateMachines.get(userId);
    }

    /**
     * Check whether a text is a recommendation request
     * @param msg String from user
     */
    static public boolean isRecommendationRequest(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (recommendKeywords.contains(word)) return true;
        }
        return false;
    }
    static private HashSet<String> recommendKeywords;
    static {
        recommendKeywords = new HashSet<String>();
        recommendKeywords.add("recommendation");
        recommendKeywords.add("recommendations");
        recommendKeywords.add("recommend");
        recommendKeywords.add("menu");
        recommendKeywords.add("suggestion");
        recommendKeywords.add("suggest");
    }

    /**
     * Check whether a text is an initial input request
     * @param msg String from user
     */
    static public boolean isInitialInputRequest(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (initialInputKeywords.contains(word)) {
                return true;
            }
        }
        return false;
    }
    static private HashSet<String> initialInputKeywords;
    static {
        initialInputKeywords = new HashSet<String>();
        initialInputKeywords.add("setting");
        initialInputKeywords.add("settings");
        initialInputKeywords.add("personal");
    }

    /**
     * Check whether a text is a feedback request
     * @param msg String from user
     */
    static public boolean isFeedbackRequest(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (feedbackKeywords.contains(word)) return true;
        }
        return false;
    }
    static private HashSet<String> feedbackKeywords;
    static {
        feedbackKeywords = new HashSet<String>();
        feedbackKeywords.add("feedback");
        feedbackKeywords.add("report");
        feedbackKeywords.add("digest");
    }
    /* ------------------------ LOGIC END ------------------------ */

    static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(path).build().toUriString();
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

    private static DownloadedContent saveContent(String ext,
        MessageContentResponse responseBody) {

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
        return new DownloadedContent(tempFile, createUri("/downloaded/"
            + tempFile.getFileName()));
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }
}
