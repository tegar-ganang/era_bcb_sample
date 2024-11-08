package com.peterhi.client.impl.managers;

import java.net.PasswordAuthentication;
import com.peterhi.beans.ChannelBean;
import com.peterhi.client.Manager;

public class StoreManager implements Manager {

    private Integer id;

    private ChannelBean channel;

    private PasswordAuthentication auth;

    public ChannelBean getChannel() {
        return channel;
    }

    public void setChannel(ChannelBean channel) {
        this.channel = channel;
    }

    public PasswordAuthentication getAuth() {
        return auth;
    }

    public void setAuth(PasswordAuthentication auth) {
        this.auth = auth;
    }

    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        if (auth == null) return null;
        return auth.getUserName();
    }

    public void onConfigure() {
    }
}
