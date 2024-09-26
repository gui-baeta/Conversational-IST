package com.monkeys123.conversationalist;

import com.monkeys123.conversationalist.Data.ChatMessage;

import java.sql.Time;
import java.time.Instant;

public class ChatMessageDTO {
    String fileType;
    long timeStamp;
    int userID;
    String userName;

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    ChatMessage.MessageType messageType;
    String message;

    public ChatMessageDTO(){}

    public ChatMessageDTO(int userID, String userName, String message, ChatMessage.MessageType messageType, String fileType) {
        this.timeStamp = Time.from(Instant.now()).getTime();
        this.userID = userID;
        this.userName = userName;
        this.message = message;
        this.messageType = messageType;
        this.fileType = fileType;
    }


    public ChatMessageDTO(ChatMessage chatMessage) {
        this.timeStamp = chatMessage.getTimeStamp();
        this.userID = chatMessage.getUserID();
        this.userName = chatMessage.getUserName();
        this.messageType = chatMessage.getMessageType();
        this.message = chatMessage.getMessage();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ChatMessage.MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(ChatMessage.MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}