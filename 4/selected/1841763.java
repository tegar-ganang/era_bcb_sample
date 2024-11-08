package com.walsai.mxbot;

import org.jibble.pircbot.PircBot;

public abstract class Module extends Thread {

    private String[] args;

    private PircBot bot;

    private String channel;

    private String sender;

    public String getChannel() {
        return channel;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public abstract void run();

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public PircBot getBot() {
        return bot;
    }

    public void setBot(PircBot bot) {
        this.bot = bot;
    }
}
