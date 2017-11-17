package controller;

import com.linecorp.bot.client.MessageContentResponse;
import edu.cmu.sphinx.result.WordResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import database.keeper.StateKeeper;
import agent.IntentionClassifier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import utility.Validator;
import utility.FormatterMessageJSON;
import utility.ParserMessageJSON;
import utility.TextProcessor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.$;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;

/**
 * ChatbotController: interfacing with LINE API, handle global state transition.
 * @author szhouan
 * @version v2.0.0
 */
@Slf4j
@Service
@LineMessageHandler
public class ChatbotController
    implements Consumer<reactor.bus.Event<
        FormatterMessageJSON>> {

    private HashMap<String, ScheduledFuture<?>> noReplyFutures = new HashMap<>();

    @Autowired(required = true)
    private LineMessagingClient lineMessagingClient;

    @Autowired
    private Publisher publisher;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired(required = false)
    private IntentionClassifier classifier;

    private static final int NO_REPLY_TIMEOUT = 1;

    private Configuration sphinxConfiguration;

    /**
     * Set of global key words escaping used by various handlers.
     */
    Set<String> keyWords = new HashSet<>(Arrays.asList("CANCEL"));

    /**
     * Register on eventBus.
     */
    @PostConstruct
    public void init() {
        if (eventBus != null) {
            eventBus.on($("FormatterMessageJSON"), this);
            log.info("ChatbotController register on eventBus");
        }
        // Config the CMU Sphinx data path
        sphinxConfiguration = new Configuration();
        sphinxConfiguration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        sphinxConfiguration.setDictionaryPath("resource:/language/chatbot.dic");
        sphinxConfiguration.setLanguageModelPath("resource:/language/chatbot.lm");
    }

    /**
     * EventBus FormatterMessageJSON event handler.
     * Send LINE message if required.
     * @param ev FormatterMessageJSON event
     */
    public void accept(Event<FormatterMessageJSON> ev) {
        FormatterMessageJSON fmt = ev.getData();
        String userId = fmt.getUserId();
        if (noReplyFutures.containsKey(userId)) {
            ScheduledFuture<?> future = noReplyFutures.remove(userId);
            if (future != null) future.cancel(false);
            log.info("No reply future cancelled for user {}", userId);
        }

        // build message list
        List<Message> messages = new ArrayList<>();
        JSONArray arr = fmt.getMessageArray();
        if (arr.length() == 0) {
            return;
        }
        for (int i=0; i<arr.length(); ++i) {
            JSONObject obj = arr.getJSONObject(i);
            switch (obj.getString("type")) {
                case "text":
                    messages.add(new TextMessage(obj.getString("textContent")));
                    break;
                case "image":
                    messages.add(new ImageMessage(obj.getString("originalContentUrl"),
                        obj.getString("previewContentUrl")));
                    break;
                default:
                    log.info("Invalid message type {}", obj.getString("type"));
            }
        }
        log.info("CONTROLLER: Send push message");
        PushMessage pushMessage = new PushMessage("U"+userId, messages);
        if (lineMessagingClient != null)
            lineMessagingClient.pushMessage(pushMessage);
    }

    /**
     * Event handler for LINE text message.
     * @param event LINE text message event
     */
    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {

        String userId = event.getSource().getUserId();
        String textContent = event.getMessage().getText();
        String messageId = event.getMessage().getId();

        // remove first letter 'U' from userId
        userId = userId.substring(1);

        log.info("textContent: {}", textContent);

        // cancel session?
        if (textContent.trim().equals("CANCEL")) {
            log.info("Session cancelled by user {}", userId);
            setUserState(userId, State.IDLE);

            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("OK, the session is cancelled.");
            publisher.publish(fmt);
            return;
        }

        // publish message
        ParserMessageJSON psr = new ParserMessageJSON(userId, "text");
        psr.set("messageId", messageId)
           .set("textContent", textContent)
           .setState(getUserState(userId).toString());
        registerNoReplyCallback(userId);
        if (getUserState(userId) != State.IDLE) {
            publisher.publish(psr);
        } else {
            // Prevent race condition
            Event<ParserMessageJSON> ev = new Event<>(null, psr);
            if (classifier != null) {
                classifier.accept(ev);
            }
        }
    }

    /**
     * Event handler for LINE audio message.
     * Making use of the CMU Sphinx recognition package to do speech recognition.
     * The dictionary and language model files are at /resources/language folder.
     * @param event LINE image message event
     */
    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) {

        String userId = event.getSource().getUserId();
        String messageId = event.getMessage().getId();

        // remove first letter 'U' from userId
        userId = userId.substring(1);

        MessageContentResponse response;
        List<String> messages = new ArrayList<>();
        try {
            // Prompt recognition in progress
            FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
            fmt.appendTextMessage("Speech recognition in progress, please wait...");
            publisher.publish(fmt);

            // Initialization
            StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(sphinxConfiguration);
            response = lineMessagingClient.getMessageContent(messageId).get();

            // Conversion to correct format
            InputStream inputStream = new BufferedInputStream(response.getStream());
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            AudioFormat oldFormat = audioInputStream.getFormat();
            AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
            log.info("Old audio format:" + oldFormat.toString());
            log.info("New audio format:" + newFormat.toString());
            AudioInputStream recognitionInputStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);

            // Recognition
            recognizer.startRecognition(recognitionInputStream);
            SpeechResult result;
            log.info("Entered recognition part.");
            while ((result = recognizer.getResult()) != null) {
                String word = result.getHypothesis();
                messages.add(word);
            }
            recognizer.stopRecognition();
            recognitionInputStream.close();

            // Force garbage collection
            System.gc();
        } catch (InterruptedException | ExecutionException | IOException | UnsupportedAudioFileException e) {
            log.error("Error in recognition.", e);
        }

        // Join results
        String msg = String.join(" ", messages).trim();
        // Escape lowing for the key words
        if (!keyWords.contains(msg)) msg = msg.toLowerCase();
        // Try parsing number
        if (TextProcessor.sentenceToNumber(msg) != null) msg = TextProcessor.sentenceToNumber(msg);
        log.info("Speech recognized message: " + msg);

        // Show recognition result
        FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
        fmt.appendTextMessage("RECOGNIZED: " + msg);
        publisher.publish(fmt);

        // Sleep to pursue the order of replied messages
        sleep(500);

        // Pack into text message content and call text handler
        TextMessageContent textContent = new TextMessageContent(event.getMessage().getId(), msg);
        MessageEvent<TextMessageContent> textEvent = new MessageEvent<>(
                event.getReplyToken(), event.getSource(), textContent, event.getTimestamp()
        );
        this.handleTextMessageEvent(textEvent);
    }


    /**
     * Event handler for LINE unfollow event.
     * @param event LINE unfollow event.
     */
    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        String userId = event.getSource().getUserId();
        userId = userId.substring(1);

        log.info("Unfollowed by user userId {}", userId);
        setUserState(userId, State.UNFOLLOWING);
    }

    /**
     * Event handler for LINE follow event.
     * @param event LINE follow event.
     */
    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String userId = event.getSource().getUserId();
        userId = userId.substring(1);

        log.info("Followed by user {}", userId);
        setUserState(userId, State.FOLLOWING);
    }

    /**
     * Get state of a user.
     * @param userId String of user Id
     * @return State of the user. State.INVALID is returned if there
     *         is no record for this user
     */
    public State getUserState(String userId) {
        StateKeeper keeper = new StateKeeper();
        String stateName = keeper.get(userId);
        keeper.close();
        return State.getStateByName(stateName);
    }

    /**
     * Set state of a user, register timeout callback, and publish the transition.
     * @param userId String of user Id.
     * @param newState New state for the user.
     * @return Whether state transition succeeds.
     */
    public boolean setUserState(String userId, State newState) {
        State currentState = getUserState(userId);
        if (currentState == newState) {
            log.info("State will not change for user {}", userId);
            return false;
        }
        log.info("State of user {} changed to {}", userId, newState.toString());
        try {
            // prevent race condition
            // Message handled by one agent module will not be handled by another
            Thread.sleep(600);
        } catch (Exception e) {
            log.info(e.toString());
        }
        setKeeperState(userId, newState);

        // publish state transition
        ParserMessageJSON psr = new ParserMessageJSON(userId, "transition");
        // prevent null value
        psr.set("textContent", "")
           .set("messageId", "")
           .setState(getUserState(userId).toString());
        publisher.publish(psr);

        // timeout callback if new state is not IDLE
        if (newState != State.IDLE) {
            taskScheduler.schedule(getTimeoutCallback(userId,
                newState, newState==State.RECOMMEND?State.RECORD_MEAL:State.IDLE),
                State.getTimeoutDate());
        }
        return true;
    }

    /**
     * Set global state using StateKeeper.
     * @param userId String of user Id
     * @param newState New state to set
     */
    protected void setKeeperState(String userId, State newState) {
        StateKeeper keeper = new StateKeeper();
        keeper.set(userId, newState.toString());
        keeper.close();
    }

    /**
     * Helper function returning callback function for timeout event.
     * @param userId String of user Id.
     * @param currentState Current state of user when the function is called.
     *                     Must not be State.INVALID.
     * @param nextState The next state of the user when timeout happens.
     * @return A runnable object as callback function.
     */
    private Runnable getTimeoutCallback(String userId,
        State currentState, State nextState) {
        return new Runnable() {
            @Override
            public void run() {
                State state = getUserState(userId);
                if (currentState != state) return;
                setUserState(userId, nextState);
            }
        };
    }

    private static final String[] replies = {
        "Sorry, but I don't understand what you said.",
        "Oops, that is complicated for me.",
        "Well, that doesn't make sense to me.",
        "Well, I really do not understand that."
    };
    /**
     * Register no-reply callback for user input.
     * If no agent module replies the user, the controller will reply default message.
     * @param userId String of user Id.
     */
    private void registerNoReplyCallback(String userId) {
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
                    FormatterMessageJSON fmt = new FormatterMessageJSON(userId);
                    int randomNum = ThreadLocalRandom.current()
                        .nextInt(0, replies.length);
                    fmt.appendTextMessage(replies[randomNum]); // general reply
                    State state = getUserState(userId);
                    switch (state) {
                        case IDLE:
                        fmt.appendTextMessage("To set your personal info, " +
                            "send 'setting'.\nIf you want to obtain recommendation, " +
                            "please say 'recommendation'.\n" +
                            "You can aways cancel an operation by saying 'CANCEL'");
                        break;

                        default:
                        fmt.appendTextMessage("You could cancel the session by saying CANCEL");
                        break;
                    }
                    publisher.publish(fmt);
                    noReplyFutures.remove(userId);
                }
            },
            new Date((new Date()).getTime() + 1000 * NO_REPLY_TIMEOUT)));
        log.info("Register new no reply callback for user {}", userId);
    }

    /**
     * Sleep for a few milliseconds, used for pursuing the order or messages.
     * @param milliseconds the number of seconds to sleep.
     */
    public void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e ) {
            log.warn("Sleeping in controller got interrupted.");
        }
    }
}