package com.example.bot.spring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class FormatterImageMessageJSON extends MsgJSON{
    
    String originalContentUrl;
    String previewContentUrl;
    
    FormatterImageMessageJSON() {
        originalContentUrl = null;
        previewContentUrl = null;
        type = "image";
    }

    public String getOriginalContentUrl() {
        return originalContentUrl;
    }

    public String getPreviewContentUrl() {
        return previewContentUrl;
    }

    public void setOriginalContentUrl(String originalContentUrl) {
        this.originalContentUrl = originalContentUrl;
    }

    public void setPreviewContentUrl(String previewContentUrl) {
        this.previewContentUrl = previewContentUrl;
    }
}