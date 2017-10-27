package com.example.bot.spring;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)
public class StateTransitionLogJSON<UTC_timestamp_format> {
    UTC_timestamp_format timestamp;
    String userId;
    String event;
    String oldState;
    String newState;

    public String getUserId() {
        return userId;
    }
    public String getEvent() {
        return event;
    }
    public String getOldState() {
        return oldState;
    }
    public String getNewState() {
        return newState;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public void setOldState(String oldState) {
        this.oldState = oldState;
    }
    public void setNewState(String newState) {
        this.newState = newState;
    }
}