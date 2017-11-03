package controller;

import org.json.JSONArray;
import org.json.JSONObject;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;

import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.google.common.io.ByteStreams;

import java.nio.file.Path;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;
import retrofit2.Response;
import java.util.concurrent.CountDownLatch;

import static reactor.bus.selector.Selectors.$;

@Slf4j
@Component
public class Formatter {

    @Autowired(required=false)
    private LineMessagingClient lineMessagingClient;

    private void reply(@NonNull String replyToken,
        @NonNull List<Message> messages) {

        try {
            log.info("FORMATTER: send reply message");
            if (lineMessagingClient != null) {
                BotApiResponse apiResonse = lineMessagingClient.replyMessage(
                    new ReplyMessage(replyToken, messages)).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.info("FORMATTER: error in send reply message: "
                + e.toString());
            throw new RuntimeException(e);
        }
    }
    
    private void push(@NonNull String userId, @NonNull List<Message> messages) {
        log.info("FORMATTER: send push message");
        PushMessage pushMessage = new PushMessage("U"+userId, messages);
        if (lineMessagingClient != null)
            lineMessagingClient.pushMessage(pushMessage);
    }
    
    /**
     * Send a LINE message according to the formatter message JSON
     * @param fmt A Formatter Message JSON
     */
    public void sendMessage(FormatterMessageJSON fmt) {
        log.info(fmt.toString());
        List<Message> messages = new ArrayList<Message>();
        JSONArray arr = (JSONArray) fmt.get("messages"); 
        for(int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            switch(obj.getString("type")) {
                case "text":
                    messages.add(new TextMessage(obj.getString("textContent")));
                    break;
                case "image":
                    messages.add(new ImageMessage(obj.getString("originalContentUrl"),
                        obj.getString("previewContentUrl")));
                    break;
                default:
                    log.info("FORMATTER: Invalid message type {}",
                        obj.getString("type"));
            }
        }

        String type = fmt.get("type").toString();
        switch(type) {
            case "reply":
                // reply(fmt.get("replyToken").toString(), messages);
                // break;
            case "push":
                push(fmt.get("userId").toString(), messages);
                break;
            default:
                log.info("FORMATTER: Invalid message type {}", type);
        }
    }
}