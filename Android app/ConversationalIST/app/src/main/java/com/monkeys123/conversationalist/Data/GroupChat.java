package com.monkeys123.conversationalist.Data;

import android.location.Location;
import android.util.Log;

import java.util.TreeSet;

public class GroupChat {
    boolean preloaded = false;
    int groupID;
    String groupName;
    String groupLogo;
    String shareLink;
    GroupChat.GroupType type;
    Location location = null;
    boolean newMessages = false;

    private double radius = 0;

    public boolean isPreloaded() {
        return preloaded;
    }

    public void setPreloaded(boolean preloaded) {
        this.preloaded = preloaded;
    }


    TreeSet<ChatMessage> messages = new TreeSet<>(new Helper());

    public TreeSet<ChatMessage> getMessages() {
        return messages;
    }

    public String getShareLink() {
        return shareLink;
    }

    public String getGroupLogo() {
        return groupLogo;
    }

    public long getGroupID() {
        return groupID;
    }

    public String getGroupName() {
        return groupName;
    }

    public GroupType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public enum GroupType {
        Public,
        Private,
        Geofenced,
    }

    public GroupChat(int groupID, String groupName, GroupChat.GroupType type, Location location, double radius) {
        this.groupLogo = createLogo(groupName);
        this.groupID = groupID;
        this.groupName = groupName;
        this.type = type;
        this.location = location;
        this.radius = radius;
        this.shareLink = "www.conversationalist.pt/chatrooms/" + groupID;
        this.preloaded = false;
    }

    public GroupChat(int groupID, String groupName, GroupChat.GroupType type) {
        this.groupLogo = createLogo(groupName);
        this.groupID = groupID;
        this.groupName = groupName;
        this.type = type;
        this.shareLink = "www.conversationalist.pt/chatrooms/" + groupID;
        this.preloaded = false;
    }


    public boolean appendMessage(ChatMessage chatMessage) {
        return messages.add(chatMessage);
    }

    public static String createLogo(String groupName) {
        long word_count = groupName.chars().filter(ch -> ch == ' ').count() + 1;
        if (word_count == 1) {
            return "" + Character.toUpperCase(groupName.charAt(0)) + Character.toUpperCase(groupName.charAt(groupName.length() - 1));
        } else {
            return "" + Character.toUpperCase(groupName.charAt(0)) + Character.toUpperCase(groupName.charAt(groupName.lastIndexOf(' ') + 1));
        }
    }

    public double getRadius() {
        return radius;
    }

    public boolean removeMessage(ChatMessage message) {
        return messages.remove(message);
    }


}
