package edu.sdsc.rtdsm.drivers.turbine;

import com.rbnb.sapi.*;
import java.util.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.feedback.SinkFeedbackAgent;
import edu.sdsc.rtdsm.framework.util.*;

/**
 * Class TurbineRawSink
 * 
 */
public class TurbineRawSink extends TurbineClient implements DataSink {

    Sink sink;

    TurbineSinkWrapper sinkWrapper;

    TurbineSinkFetchHandler handler;

    SinkCallBackListener callbackHandler;

    Vector<Integer> reqPollIntervals;

    boolean connected;

    Vector<Integer> reqDatatypes;

    Vector<String> reqChannels;

    Vector<Integer> reqPathIndicies;

    public TurbineRawSink(TurbineServer server, String sinkName, SinkCallBackListener callbackHandler) {
        super(server, sinkName);
        this.sinkWrapper = server.getSinkWrapper();
        this.callbackHandler = callbackHandler;
    }

    public void connectAndWait() {
        connectNoHangup();
        waitForData();
    }

    public void waitForData() {
        handler.initFetch();
    }

    public void connectNoHangup() {
        this.reqChannels = sinkWrapper.getChannelNames();
        this.reqDatatypes = sinkWrapper.getChannelDataTypesVec();
        sink = new Sink();
        setClient(sink);
        this.connected = connect();
        reqPathIndicies = addChannels(reqChannels);
        switch(sinkWrapper.getRequestMode()) {
            case TurbineSinkConfig.MONITOR_MODE:
                handler = new TurbineSinkMonitorHandler(this, callbackHandler, sinkWrapper.getTimeout());
                break;
            case TurbineSinkConfig.POLL_MODE:
                this.reqPollIntervals = sinkWrapper.getPollIntervals();
                handler = new TurbineSinkPollHandler(this, callbackHandler);
                break;
            default:
                throw new IllegalStateException("Only monitor mode is currently " + "supported. More to come soon");
        }
        Debugger.debug(Debugger.TRACE, "RBNB sink=" + sink);
    }

    public ChannelMap getMap() {
        return map;
    }

    public Sink getSink() {
        return sink;
    }

    public Vector<Integer> getReqIndicies() {
        return reqPathIndicies;
    }

    public Vector<Integer> getReqDatatypes() {
        return reqDatatypes;
    }

    public Vector<String> getReqChannelNames() {
        return reqChannels;
    }

    public Vector<Integer> getReqPollIntervals() {
        return reqPollIntervals;
    }

    public boolean isConnected() {
        return connected;
    }

    public Object pullData(int channelId) {
        return null;
    }

    public void terminate() {
        handler.terminate();
        Debugger.debug(Debugger.TRACE, "Closing RBNB Connection()");
        sink.CloseRBNBConnection();
        Debugger.debug(Debugger.TRACE, "Closed RBNB Connection()");
        connected = false;
    }
}
