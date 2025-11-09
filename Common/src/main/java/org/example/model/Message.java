package org.example.model;

import java.time.LocalDateTime;

public class Message {

    private final String message;
    private final String from;
    private final LocalDateTime time;
    private boolean isRead;

    public Message(String message, String from, LocalDateTime time, boolean isRead) {
        this.message = message;
        this.from = from;
        this.time = time;
        this.isRead = isRead;
    }

    public String getMessage() {
        return message;
    }

    public String getFrom() {
        return from;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
