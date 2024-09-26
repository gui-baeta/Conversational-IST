package com.monkeys123.conversationalist;


import com.monkeys123.conversationalist.Data.GroupChat;

public class ChatroomDTO {
    private int id;

    private String name;

    private GroupChat.GroupType type;

    private double lng = 0;

    private double lat = 0;

    private double radius = 0;

    public ChatroomDTO() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GroupChat.GroupType getType() {
        return type;
    }

    public void setType(GroupChat.GroupType type) {
        this.type = type;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }


}