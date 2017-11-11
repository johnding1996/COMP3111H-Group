package controller;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import com.linecorp.bot.model.profile.UserProfileResponse;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;

import utility.Validator;

import java.io.File;
import com.asprise.ocr.Ocr;

@Slf4j
@Service
@LineMessageHandler
public class ChatbotController
    implements reactor.fn.Consumer<reactor.bus.Event<
        FormatterMessageJSON>> {

    private HashMap<String, StateMachine> stateMachines = new
        HashMap<String, StateMachine>();
    private HashMap<String, ScheduledFuture<?>> noReplyFutures = new
        HashMap<String, ScheduledFuture<?>>();

    @Autowired(required = false)
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private Publisher publisher;

    @Autowired
    private Formatter formatter;

    @Autowired
    private EventBus eventBus;

    @Autowired
    public TaskScheduler taskScheduler;

    private static final boolean debugFlag = true;
    public static final String DEBUG_COMMAND_PREFIX = "$$$";
    private static final int NO_REPLY_TIMEOUT = 3;
 
    /**
     * Register on eventBus
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            log.info("Register FormatterMessageJSON on eventBus");
            eventBus.on($("FormatterMessageJSON"), this);
        }
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "GMT+08")
    public void askWeight() {
        log.info("AskForWeight: **************************");
        List<String> userIdList = getUserIdList();
        for (String userId : userIdList) {
            toNextState(userId, "askWeightTrigger");
            String state = getStateMachine(userId).getState();
            ParserMessageJSON psr = new ParserMessageJSON();
            psr.set("userId", userId)
               .set("state", state);
            publisher.publish(psr);
        }
    }

    /**
     * EventBus FormatterMessageJSON event handle
     * @param ev FormatterMessageJSON event
     */
    public void accept(reactor.bus.Event<FormatterMessageJSON> ev) {
        FormatterMessageJSON formatterMessageJSON = ev.getData();
        log.info("\nChatbotController:\n" + formatterMessageJSON.toString());
        
        String userId = (String)formatterMessageJSON.get("userId");
        /* Handle state transition if any */
        if (formatterMessageJSON.get("stateTransition") != null) {
            String transition = (String)formatterMessageJSON.get("stateTransition");
            log.info("User {} triggers state transition {}",
                userId, transition);
            toNextState(userId, transition);
        }
        if (!formatterMessageJSON.get("type").equals("transition")) {
            formatter.sendMessage(formatterMessageJSON);
            if (noReplyFutures.containsKey(userId)) {
                ScheduledFuture<?> future = noReplyFutures.get(userId);
                future.cancel(false);
                log.info("No reply future cancelled for user {}", userId);
            }
        }
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event)
        throws Exception {

        String userId = event.getSource().getUserId();
        String replyToken = event.getReplyToken();
        String textContent = event.getMessage().getText();
        String messageId = event.getMessage().getId();

        // remove first letter 'U' from userId
        int endIndex = userId.length();
        userId = userId.substring(1, endIndex);

        StateMachine stateMachine = getStateMachine(userId);
        String state = stateMachine.getState();

        registerNoReplyCallback(userId);

        boolean isSpecial = textContent.startsWith(DEBUG_COMMAND_PREFIX);
        // isSpecial = isSpecial || textContent.equals("SETTING")
        //     || textContent.equals("RECOMMEND");
        isSpecial = isSpecial || textContent.equals("CANCEL");
        if (debugFlag && isSpecial) {
            log.info("User initiated state transition using command");
            // if (textContent.equals("SETTING")) textContent = "$$$InitialInput";
            // if (textContent.equals("RECOMMEND")) textContent = "$$$ParseMenu";
            if (textContent.equals("CANCEL")) textContent = "$$$Idle";
            changeStateByCommand(userId, textContent);
            publishStateTransition(userId);
            return;
        }

        /* update state */
        if (state.equals("Idle")) {
            if (isRecommendationRequest(textContent)) {
                toNextState(userId, "recommendationRequest");
            } else if (isInitialInputRequest(textContent)) {
                toNextState(userId, "initialInputRequest");
            } else if (isFeedbackRequest(textContent)) {
                toNextState(userId, "feedbackRequest");
            }
            state = stateMachine.getState();
            log.info("State transition handled by Controller");
            log.info("userId={}, newState={}", userId, state);
        } else if (state.equals("Recommend")) {
            if (isFinishMeal(textContent)) {
                toNextState(userId, "timeout");
            }
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
        log.info("Get IMAGE message from user: {} !!!!!!!!!!!!!!!!!!!!!!!!!!", event.getSource().getUserId());
        String messageId = event.getMessage().getId();
        String replyToken = event.getReplyToken();
        final MessageContentResponse response;
        try {
            response = lineMessagingClient.getMessageContent(messageId).get();
        } catch (InterruptedException | ExecutionException e) {
            log.info("cannot get image: " + e.getMessage());
            throw new RuntimeException(e);
        }
        //TEST: store the image I uploaded in a static url
        if(event.getSource().getUserId().equals("U60ee860ae5e086599f9e2baff5efcf15")) {
            log.info("Get image sent from Lucis");
            
            Path bgPath = DietingChatbotApplication.staticPath.resolve("pikachu.png");
            DownloadedContent background = new DownloadedContent(bgPath
            , createUri("/static/" + bgPath.getFileName()));
            // Boolean bool = background.createNewFile;
            // log.info("File created: " + bool);
            try (OutputStream outputStream = Files.newOutputStream(background.path)) {
                ByteStreams.copy(response.getStream(), outputStream);
                log.info("Saved pikachu: {}", background);
                List<Message> messages = new ArrayList<Message>();
                messages.add(new ImageMessage(background.getUri(), background.getUri()));
                lineMessagingClient.replyMessage(new ReplyMessage(replyToken, messages));
            } catch(IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }


        DownloadedContent png = saveContent("png", response);
        log.info("Get png uri {}", png.getUri());
        handleImageContent(replyToken, event, png.getUri());
    }

    @EventMapping
	public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        String data = event.getPostbackContent().getData();
        log.info("Got postback " + data);
        String action = data.split("&")[0].split("=")[1];
        String label = data.split("&")[1].split("=")[1];
        switch(action) {
            case "Setting":
                switch(label) {
                    case "InitialSetting":
                    case "ChangeWeight":
                    case "SetGoal":
                }
            break;
            case "MenuInput":
                switch(label) {
                    case "Text":
                    case "URI":
                    case "Image":
                }
            break;
            case "Feedback":
                switch(label) {
                    case "DailyReport":
                    case "WeeklyReport":
                    case "GetChart":
                }
            break;
        }
    }
    
    /**
     * Event Handler for Image
     */
    private void handleImageContent(String replyToken, Event event, String uri) {
        URL url = null;
        
        // handle Exception
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            System.out.println("The URL is not valid.");
            System.out.println(e.getMessage());
        }
        Ocr.setUp(); // one time setup
        Ocr ocr = new Ocr(); // create a new OCR engine
        ocr.startEngine("eng", Ocr.SPEED_FASTEST); // English
        String s = ocr.recognize(new URL[] {url}
        , Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
        log.info("Result: " + s);
        // ocr more images here ...
        ocr.stopEngine();
        //ParserMessageJSON parserMessageJSON = new ParserMessageJSON();
        // parserMessageJSON.set("userId", event.getSource().getUserId())
        //     .set("state", "Idle").set("replyToken", replyToken)
        //     .setImageMessage(png.getUri());
        // publisher.publish(parserMessageJSON);
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
            log.info("Creating state machine for {}", userId);
            stateMachines.put(userId, new StateMachine(userId));
        }
        return stateMachines.get(userId);
    }


    /**
     * Get user Id list
     * @return List of user Id in String
     */
    public List<String> getUserIdList() {
        ArrayList<String> ret = new ArrayList<>();
        ret.add("U60ee860ae5e086599f9e2baff5efcf15");
        return ret;
    }

    /**
     * State transition wrapper, to next state and register callback
     * And publish state transition
     * @param userId String of user id
     * @param transition String representing the state transition
     */
    public void toNextState(String userId, String transition) {
        StateMachine stateMachine = getStateMachine(userId);
        boolean isStateChanged = stateMachine.toNextState(transition);
        if (!isStateChanged) return;
        registerStateTransitionCallback(userId);

        publishStateTransition(userId);
    }

    /**
     * Publish state transition message
     * @param userId String of user Id
     */
    public void publishStateTransition(String userId) {
        log.info("PUBLISHER: publishing state transition");
        StateMachine stateMachine = getStateMachine(userId);
        ParserMessageJSON psr = new ParserMessageJSON();
        psr.set("userId", userId)
           .set("state", stateMachine.getState())
           .set("replyToken", "invalid")
           .setTextMessage("noId", DEBUG_COMMAND_PREFIX);
        publisher.publish(psr);
    }

    /**
     * Register callback after state transition
     * @param userId String of user Id
     */
    private void registerStateTransitionCallback(String userId) {
        StateMachine stateMachine = getStateMachine(userId);
        State state = stateMachine.getStateObject();
        int timeout = state.getTimeout();
        String timeoutState = state.getTimeoutState();
        if (!state.getName().equals("Idle")) {
            log.info("register call back that will run after {} sec", timeout);
            taskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    toNextState(userId, "timeout");
                    ParserMessageJSON psr = new ParserMessageJSON();
                    psr.set("userId", userId)
                       .set("state", timeoutState);
                    publisher.publish(psr);
                }
            }, new Date(1000*timeout + (new Date()).getTime()));
        } else {
            log.info("remove state machine for {} after {} sec",
                userId, timeout);
            taskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    stateMachines.remove(userId);
                    log.info("state machine for {} removed", userId);
                }
            }, new Date(1000*State.DEFAULT_TIMEOUT
                + (new Date()).getTime()));
        }
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

    /**
     * Check whether a text means finish meal
     */
    static public boolean isFinishMeal(String msg) {
        for (String word : TextProcessor.sentenceToWords(msg)) {
            if (finishMealWords.contains(word)) return true;
        }
        return false;
    }
    static private HashSet<String> finishMealWords;
    static {
        List<String> list = Arrays.asList(
            "finish", "done"
        );
        finishMealWords = new HashSet<String>(list);
    }

    /**
     * A debug helper function for changing state
     * @param userId String of user Id
     * @param command Command for state transition
     */
    public void changeStateByCommand(String userId, String command) {
        assert command.startsWith(DEBUG_COMMAND_PREFIX);
        String newState = command.substring(DEBUG_COMMAND_PREFIX.length());

        /* Send push message */
        FormatterMessageJSON fmt = new FormatterMessageJSON();
        fmt.set("userId", userId)
           .set("type", "push")
           .appendTextMessage("New state: " + newState);
        if (State.validateStateName(newState))
            fmt.appendTextMessage("Change state succeed");
        else fmt.appendTextMessage("Change state failed, invalid state " +
            newState);
        publisher.publish(fmt);

        StateMachine stateMachine = getStateMachine(userId);
        stateMachine.setState(newState);
        registerStateTransitionCallback(userId);
    }


    static CarouselColumn setting = new CarouselColumn("https://murmuring-headland-42797.herokuapp.com/static/pikachu.png"
    , "Setting"
    , "Configure your information by clicking on the buttons", 
    Arrays.asList(
        new PostbackAction("Initial Setting", "action=Setting&label=InitialSetting"),
        new MessageAction("Change Weight", "action=Setting&label=ChangeWeight"),
        new PostbackAction("Set Goal", "action=Setting&label=SetGoal")
    ));
    static CarouselColumn menuInput = new CarouselColumn("https://example.com/bot/images/item2.jpg"
    , "Menu Input"
    , "You can give the menu to me in text, URI, or Image format", 
    Arrays.asList(
        new PostbackAction("Text", "action=MenuInput&label=Text"),
        new PostbackAction("URI", "action=MenuInput&label=URI"),
        new PostbackAction("Image", "action=MenuInput&label=Image")
    ));
    static CarouselColumn feedback = new CarouselColumn("https://example.com/bot/images/item1.jpg"
    , "Feedback", 
    "You can get your recent information from me", 
    Arrays.asList(
        new PostbackAction("Daily Report", "action=Feedback&label=DailyReport"),
        new PostbackAction("Weekly Report", "action=Feedback&label=WeeklyReport"),
        new PostbackAction("Get Chart", "action=Feedback&label=GetChart")
    ));
    static CarouselTemplate carouselTemplate = new CarouselTemplate(Arrays.asList(
        setting,
        menuInput,
        feedback
    ));
    static TemplateMessage idleMessage = new TemplateMessage("Carousel Template Test", carouselTemplate);

    /**
     * Register callback for no reply case
     * @param userId String of user Id
     */
    public void registerNoReplyCallback(String userId) {
        // cancel previous callback
        if (noReplyFutures.containsKey(userId)) {
            ScheduledFuture<?> future = noReplyFutures.get(userId);
            if (future != null) future.cancel(false);
            log.info("Cancel previous no reply callback for user {}", userId);
        }
        noReplyFutures.put(userId, taskScheduler.schedule(
            new Runnable() {
                @Override
                public void run() {
                    FormatterMessageJSON fmt = new FormatterMessageJSON();
                    String[] replies = {
                        "Sorry but I don't understand what you said.",
                        "Oops, that is complicated for me.",
                        "Well, that doesn't make sense to me.",
                        "Well, I really do not understand that."
                    };
                    int randomNum = ThreadLocalRandom.current()
                        .nextInt(0, replies.length);
                    fmt.set("type", "push")
                       .set("userId", userId)
                       .appendTextMessage(replies[randomNum]);
                    String state = getStateMachine(userId).getState();
                    switch (state) {
                        case "Idle":
                        fmt.appendTextMessage("To set your personal info, " +
                            "send 'setting'.\nIf you want to obtain recommendation, " +
                            "please say 'recommendation'.\n" +
                            "You can aways cancel an operation by saying 'CANCEL'");
                        log.info("Send template message");
                        PushMessage pushMessage = new PushMessage("U" + userId, idleMessage);
                        lineMessagingClient.pushMessage(pushMessage);
                        break;

                        case "Recommend":
                        fmt.appendTextMessage("You mean you've finished your meal? " +
                            "If yes, say 'finish' and I will record what you eat");
                    }
                    publisher.publish(fmt);
                    noReplyFutures.remove(userId);
                }
            },
            new Date((new Date()).getTime() + 1000 * NO_REPLY_TIMEOUT)));
        log.info("Register new no reply callback for user {}", userId);
    }
    /* ------------------------ LOGIC END ------------------------ */

    static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path(path).build().toUriString();
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
