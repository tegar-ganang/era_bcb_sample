package de.jlab.lab;

import de.jlab.boards.Board;

public class SubChannelUpdatedNotification {

    static enum SUBCHANNEL_VALUE_TYPE {

        TEXT, NUMBER
    }

    ;

    int address;

    int subchannel;

    double doubleValue;

    String stringValue;

    String commChannel;

    String originalReply;

    public String getOriginalReply() {
        return originalReply;
    }

    public void setOriginalReply(String originalReply) {
        this.originalReply = originalReply;
    }

    SUBCHANNEL_VALUE_TYPE subchannelValueType = SUBCHANNEL_VALUE_TYPE.NUMBER;

    public SubChannelUpdatedNotification(String commChannel, int address, int subchannel, double doubleValue, String stringValue, SUBCHANNEL_VALUE_TYPE subchannelValueType) {
        this.commChannel = commChannel;
        this.address = address;
        this.subchannel = subchannel;
        this.doubleValue = doubleValue;
        this.stringValue = stringValue;
        this.subchannelValueType = subchannelValueType;
    }

    public int getAddress() {
        return address;
    }

    public int getSubchannel() {
        return subchannel;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public SUBCHANNEL_VALUE_TYPE getSubchannelValueType() {
        return subchannelValueType;
    }

    public String getCommChannel() {
        return commChannel;
    }

    public boolean fitsBoardAndChannel(int boardChannel, Board aBoard) {
        return (boardChannel == subchannel && aBoard.getAddress() == address && aBoard.getCommChannel().getChannelName().equals(commChannel));
    }

    public boolean fitsBoard(Board aBoard) {
        return (aBoard.getAddress() == address && aBoard.getCommChannel().getChannelName().equals(commChannel));
    }
}
