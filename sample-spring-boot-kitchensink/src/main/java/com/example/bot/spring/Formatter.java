package com.example.bot.spring;

import org.json.JSONObject;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;

import java.util.concurrent.ExecutionException;
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

public class Formatter {

    private LineMessagingClient lineMessagingClient;

    public Formatter(JSONObject jsonObject, LineMessagingClient lineMessagingClient) {
        // methods for decoposing JSON file
        this.lineMessagingClient = lineMessagingClient;
        String replyType = jsonObject.getString("replyType");
        String messageType = jsonObject.getString("messageType");
        switch(replyType) {
            case "reply":
                switch(messageType) {
                    case "text":
                        reply(jsonObject.getString("replyToken"), new TextMessage(jsonObject.getString("textContent")));
                        break;
                    case "image":
                        reply(jsonObject.getString("replyToken"), new ImageMessage(jsonObject.getString("originalContentUrl"), jsonObject.getString("previewContentUrl")));
                        break;
                }
            break;
            case "push":
                switch(messageType) {
                    case "text":
                        break;
                    case "image":
                        break;
                }
            break;
        }
        

        
        

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

}