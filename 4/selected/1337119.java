package edu.sdsc.rtdsm.drivers.turbine.util;

import java.util.Vector;

public abstract class TurbineClientWrapper {

    protected TurbineServer server;

    protected Vector<String> channelNamesVec = new Vector<String>();

    protected Vector<Integer> channelDatatypesVec = new Vector<Integer>();

    public TurbineClientWrapper(TurbineServer server) {
        this.server = server;
    }

    public TurbineServer getServer() {
        return server;
    }

    public void addChannel(String channelName, Integer datatype) {
        channelNamesVec.addElement(channelName);
        channelDatatypesVec.addElement(datatype);
    }

    public Vector<String> getChannelNames() {
        return channelNamesVec;
    }

    public Vector<Integer> getChannelDataTypesVec() {
        return channelDatatypesVec;
    }

    public void resetChannelVecs(Vector<String> channelVec, Vector<Integer> channelDatatypeVec) {
        if (channelVec.size() != channelDatatypeVec.size()) {
            throw new IllegalArgumentException("The channel vector and its " + "datatype vector should be of the same size");
        }
        this.channelNamesVec = channelVec;
        this.channelDatatypesVec = channelDatatypeVec;
    }
}
