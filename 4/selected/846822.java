package edu.sdsc.rtdsm.dig.sites;

import java.util.*;
import edu.sdsc.rtdsm.drivers.turbine.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.feedback.*;
import edu.sdsc.rtdsm.framework.util.*;

/**
 * Class SiteSourceImpl
 */
public class SiteSource {

    TurbineManager mgr = TurbineManager.getInstance();

    TurbineSource src;

    FeedbackSink feedbackSink = null;

    boolean feedbackReqd = false;

    SrcFeedbackListener feedbackListener = null;

    /**
   * This constructor creates a SiteSource without the feedback channel
   * @param srcConfig The SrcConfig describing the "main" source properties
  **/
    public SiteSource(SrcConfig srcConfig) {
        this(srcConfig, null);
    }

    /**
   * @param srcConfig The SrcConfig describing the "main" source properties
   * @param feedbackSinkConfig The SinkConfig describing the feedback channel
   *                that the site source listens to. Can be "null" if feedback
   *                is not desired by the site source
  **/
    public SiteSource(SrcConfig srcConfig, SrcFeedbackListener feedbackListener) {
        if (srcConfig instanceof TurbineSrcConfig) {
            src = new TurbineSource((TurbineSrcConfig) srcConfig);
        } else {
            throw new IllegalStateException("Sites currently support only data turbine");
        }
        Debugger.debug(Debugger.TRACE, "Feedback Listener = " + feedbackListener);
        if (feedbackListener != null) {
            this.feedbackReqd = true;
            this.feedbackListener = feedbackListener;
        }
    }

    public String getName() {
        return src.getName();
    }

    public Vector<Integer> getChannelIndicies() {
        return src.srcChannelIndicies;
    }

    public Vector<String> getChannelNames() {
        return src.srcChannelNames;
    }

    public boolean connect() {
        boolean connected = false;
        connected = src.connect();
        Debugger.debug(Debugger.TRACE, "FeedbackReqd = " + feedbackReqd);
        if (connected && feedbackReqd == true) {
            createFeedbackSink();
            connected = feedbackSink.spawnFeedbackThread();
        }
        return connected;
    }

    private void createFeedbackSink() {
        String feedbackSinkName = src.getName() + Constants.FEEDBACK_SINK_SUFFIX;
        TurbineSinkConfig sinkConfig = new TurbineSinkConfig(feedbackSinkName);
        TurbineServer server = sinkConfig.getTurbineServer(src.getServerName(), src.getUsername(), src.getPassword());
        String reqPath = src.getName() + Constants.FEEDBACK_SRC_SUFFIX + "/" + Constants.FEEDBACK_CHANNEL_NAME;
        Integer datatype = Constants.DATATYPE_STRING_OBJ;
        Integer reqMode = new Integer(TurbineSinkConfig.MONITOR_MODE);
        Integer intervalOrTimeout = new Integer(-1);
        server.addSinkChannel(reqPath, datatype, new Integer(reqMode), intervalOrTimeout);
        TurbineRawSink sink = new TurbineRawSink(server, feedbackSinkName, feedbackListener);
        Debugger.debug(Debugger.TRACE, "Feedback Sink for the source:");
        sinkConfig.printSinkData(Debugger.TRACE);
        feedbackSink = new FeedbackSink(sink);
    }

    public void insertData(int channel, Object data) {
        src.insertData(channel, data);
    }

    public int flush() {
        return src.flush();
    }

    public void disconnect() {
        src.disconnect();
    }
}
