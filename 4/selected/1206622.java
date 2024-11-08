package com.softntic.meetmemanager.data.bean;

public class SNTMeetMeUser {

    private String roomId = "";

    private String userId = "";

    private String callerNum = "";

    private String callerId = "";

    private String channel = "";

    private boolean monitored = false;

    private String date = "";

    private boolean mute = false;

    public SNTMeetMeUser(String roomId, String lineToParse) {
        this.roomId = roomId;
        String[] item = lineToParse.split("!");
        userId = item[0];
        callerNum = item[1];
        callerId = item[2];
        channel = item[3];
        if (item[6].equals("1")) mute = true;
        date = item[9];
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean isMute() {
        return mute;
    }

    public String getCallerNum() {
        return callerNum;
    }

    public void setCallerNum(String callerNum) {
        this.callerNum = callerNum;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String toString() {
        return "roomId=" + roomId + " / userId=" + userId + " / ";
    }
}
