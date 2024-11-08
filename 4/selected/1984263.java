package edu.sdsc.rtdsm.framework.data;

import java.util.Vector;
import edu.sdsc.rtdsm.framework.util.Debugger;

public class DataPacket {

    private Vector<Object> dataVec;

    private Vector<Integer> channelVec;

    private Vector<String> channelNameVec;

    private Vector<Vector<Long>> timestampVec;

    long startTime;

    int size = 0;

    public DataPacket() {
        dataVec = new Vector<Object>();
        channelVec = new Vector<Integer>();
        timestampVec = new Vector<Vector<Long>>();
        channelNameVec = new Vector<String>();
        size = 0;
    }

    public void addData(Object data, Integer channel, String channelName, long timestamp, Vector<Long> timestampVecForChannel) {
        if (channelVec.size() != dataVec.size() || timestampVec.size() != dataVec.size() || channelNameVec.size() != dataVec.size()) {
            throw new IllegalStateException("Data addition to the data packet is " + "not synchronized with the timestamp and channel addition.");
        }
        dataVec.addElement(data);
        channelVec.addElement(channel);
        timestampVec.addElement(timestampVecForChannel);
        channelNameVec.addElement(channelName);
        startTime = timestamp;
        size++;
    }

    public Vector<Object> getDataVec() {
        return dataVec;
    }

    public Object getDataAt(int index) {
        return dataVec.elementAt(index);
    }

    public Vector<Integer> getChannelVec() {
        return channelVec;
    }

    public Integer getChannelAt(int index) {
        return channelVec.elementAt(index);
    }

    public Vector<Vector<Long>> getTimestampVec() {
        return timestampVec;
    }

    public long getTimestampAt(int chIndex, int dataIndex) {
        Vector<Long> tv = timestampVec.elementAt(chIndex);
        Debugger.debug(Debugger.TRACE, "Got timestamp vector at " + chIndex);
        long retVal = tv.elementAt(dataIndex).longValue();
        Debugger.debug(Debugger.TRACE, "\tGot timestamp at " + dataIndex);
        return retVal;
    }

    public Vector<String> getChannelNameVec() {
        return channelNameVec;
    }

    public String getChannelNameAt(int index) {
        return channelNameVec.elementAt(index);
    }

    public int getSize() {
        return size;
    }
}
