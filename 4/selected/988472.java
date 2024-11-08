package net.peddn.typebattle.lib.remote;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private short channel;

    private String message;

    public ChatMessage(short channel, String message) {
        this.channel = channel;
        this.message = message;
    }

    public short getChannel() {
        return channel;
    }

    public void setChannel(short channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
