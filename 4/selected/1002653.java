package com.cbsgmbh.xi.af.edifact.jca;

import com.sap.aii.af.ra.cci.XIConnectionSpec;

public class CCIConnectionSpec implements XIConnectionSpec {

    private String type;

    private String user;

    private String password;

    private String channelId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return user;
    }

    public void setUserName(String user) {
        this.user = user;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
