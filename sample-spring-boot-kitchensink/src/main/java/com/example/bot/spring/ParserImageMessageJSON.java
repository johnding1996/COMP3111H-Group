package com.example.bot.spring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class ParserImageMessageJSON extends MsgJSON {
    String id;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}