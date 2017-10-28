package controller;

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

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.bus.Event;
import reactor.fn.Consumer;
import retrofit2.Response;
import java.util.concurrent.CountDownLatch;

@Service
public class Formatter implements Consumer<Event<FormatterMessageJSON> > {

    private LineMessagingClient lineMessagingClient;

    @Autowired
    private FormatterMessageJSON formatterMessageJSON;

	public void accept(Event<FormatterMessageJSON> ev) {
        this.formatterMessageJSON = ev.getData();
        //formatting();
    }
    
    /*
    public void setLineMessagingClient(LineMessagingClient lineMessagingClient) {
        this.lineMessagingClient = lineMessagingClient;
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
       try {
           BotApiResponse apiResonse = this.lineMessagingClient.replyMessage(new ReplyMessage(replyToken, messages)).get();
       } catch (InterruptedException | ExecutionException e) {
           throw new RuntimeException(e);
        }
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }
    
    private void push(@NonNull String userId, @NonNull List<Message> messages) {
        PushMessage pushMessage = new PushMessage(userId, messages);
        this.lineMessagingClient.pushMessage(pushMessage);
    }
    */
    
    public FormatterMessageJSON getFormatterMessageJSON() {
        return this.formatterMessageJSON;
    }
    
    /*
    public void formatting() {
        List<Message> messages = new ArrayList<Message>();
        for(int i = 0; i < this.formatterMessageJSON.getMessages().size(); i++) {
            JsonObject obj = this.formatterMessageJSON.getMessages().getJsonObject(i);
            
            switch(obj.getString("type")) {
                case "text":
                messages.add(new TextMessage(obj.getString("textContent")));
                break;
                case "image":
                messages.add(new ImageMessage(obj.getString("originalContentUrl"), obj.getString("previewContentUrl")));
                break;
            }

        }

        String type = this.formatterMessageJSON.getType();
        switch(type) {
            case "reply":
                reply(this.formatterMessageJSON.getReplyToken(), messages);
                break;
            case "push":
                reply(this.formatterMessageJSON.getUserId(), messages);
            break;
        }
    }
    */
}