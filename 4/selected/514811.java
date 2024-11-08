package de.jochenbrissier.backyard.core;

import java.util.UUID;

/**
 * an memessage obj. not final 
 * 
 *TODO: IMRPOVE ...
 * @author jochen
 *
 */
public class Message {

    private long channelid;

    private String memberid;

    private String data;

    private String channelName;

    private String memberNane;

    private String methode;

    private UUID uID = UUID.randomUUID();

    public Message(String data) {
        this.data = data;
    }

    public Message() {
    }

    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    public long getChannelid() {
        return channelid;
    }

    public void setChannelid(long channelid) {
        this.channelid = channelid;
    }

    public String getMemberid() {
        return memberid;
    }

    public void setMemberid(String memberid) {
        this.memberid = memberid;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getMemberNane() {
        return memberNane;
    }

    public void setMemberNane(String memberNane) {
        this.memberNane = memberNane;
    }

    public UUID getuID() {
        return uID;
    }

    public void setuID(UUID uID) {
        this.uID = uID;
    }
}
