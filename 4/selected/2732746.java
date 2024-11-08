package com.expantion.model.chat;

import java.util.Date;

public class ChatMessage {

    private int id;

    private String message;

    private String auteur;

    private String channel;

    private Date date;

    public ChatMessage(int id, String message, String auteur, String channel, Date date) {
        super();
        this.id = id;
        this.message = message;
        this.auteur = auteur;
        this.channel = channel;
        this.date = date;
    }

    public ChatMessage() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
