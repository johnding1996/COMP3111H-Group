package com.example.bot.spring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class MsgJSON {
    
    public String type;

    public String getType() {
        return type;
    }
}