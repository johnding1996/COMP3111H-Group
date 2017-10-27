package com.example.bot.spring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class TextMessageJSON extends MsgJSON {
    String id;
    String textContent;

    TextMessageJSON() {
        id = null;
        textContent = null;
        type = "text";
    }

    public String getId() {
        return id;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
}