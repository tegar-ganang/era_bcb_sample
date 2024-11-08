package edu.sdsc.rtdsm.dataint;

import java.text.DateFormat;
import java.util.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.data.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.dig.dsw.DswSink;
import edu.sdsc.rtdsm.dig.sites.*;
import edu.sdsc.rtdsm.dig.sites.lake.*;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineSinkConfig;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineServer;

public class LakeSinkControlChannelListener implements SinkCallBackListener, Runnable {

    TurbineSinkConfig controlSinkConfig;

    TurbineSinkConfig actualSinkConfig;

    DswSink controlSink = null;

    DswSink actualSink = null;

    public volatile boolean restartNeeded = false;

    Thread actualSinkThread = null;

    Hashtable<String, SensorMetaData> sensorsAddedForSink = new Hashtable<String, SensorMetaData>();

    ThreadGroup threadGroup = new ThreadGroup("ControlChannel");

    LakeSinkControlChannelListener(String sinkName, TurbineSinkConfig controlSinkConfig, SinkCallBackListener mainListener) {
        this.controlSinkConfig = controlSinkConfig;
        this.actualSinkConfig = new TurbineSinkConfig(sinkName);
        actualSinkConfig.setCallBackListener(mainListener);
    }

    /**
   * The callback handler method for the Control channel
   * @return The handle to the sink config of the current instance
   */
    public synchronized void callBack(DataPacket dataPkt) {
        Debugger.debug(Debugger.TRACE, "ControlChannel:Got some Data");
        for (int i = 0; i < dataPkt.getSize(); i++) {
            String[] data = (String[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                Date timestamp = new Date((long) (dataPkt.getTimestampAt(i, j)));
                String time = DateFormat.getDateTimeInstance().format(timestamp);
                Debugger.debug(Debugger.TRACE, "Received|" + controlSinkConfig.getName() + "|" + chanName + "|" + time + "|" + "|" + j + "|" + data[j]);
                handleControlData(data[j]);
            }
        }
        Debugger.debug(Debugger.TRACE, "-----------------------------------");
        restartIfNeeded();
    }

    private synchronized void restartIfNeeded() {
        if (restartNeeded) {
            if (actualSink != null) {
                Debugger.debug(Debugger.TRACE, "Terminating the sink");
                actualSink.disconnect();
            }
            actualSink = new DswSink(actualSinkConfig, true);
            Debugger.debug(Debugger.TRACE, "Starting a NEW sink thread...");
            actualSinkConfig.printSinkData(Debugger.TRACE);
            Debugger.debug(Debugger.TRACE, "Creation: Callback listener=" + actualSink.getCallBackListener());
            actualSinkThread = new Thread(threadGroup, this, "DataSink");
            Debugger.debug(Debugger.TRACE, "Creating Sink thread from control Listener: " + actualSinkThread);
            actualSinkThread.start();
            restartNeeded = false;
        }
    }

    public void handleControlData(String msg) {
        if (msg.startsWith(Constants.LAKE_CONTROL_LOOKUP_PREFIX)) {
            int start = Constants.LAKE_CONTROL_LOOKUP_PREFIX.length();
            msg = msg.substring(start, msg.length());
            StringTokenizer st = new StringTokenizer(msg, Constants.LAKE_CONTROL_SEPARATOR);
            String srcName = null;
            String webSerStr = null;
            String serverAddr = null;
            String username = null;
            String password = null;
            if (st.hasMoreTokens()) {
                srcName = st.nextToken();
                Debugger.debug(Debugger.TRACE, "Source=" + srcName);
            }
            if (st.hasMoreTokens()) {
                serverAddr = st.nextToken();
                Debugger.debug(Debugger.TRACE, "ServerAddr=" + serverAddr);
            }
            if (st.hasMoreTokens()) {
                username = st.nextToken();
                if (Constants.NONEMPTY_DUMMY_USER_NAME_OR_PASSWORD.equals(username)) {
                    username = "";
                }
                Debugger.debug(Debugger.TRACE, "Username=" + username);
            }
            if (st.hasMoreTokens()) {
                password = st.nextToken();
                if (Constants.NONEMPTY_DUMMY_USER_NAME_OR_PASSWORD.equals(password)) {
                    password = "";
                }
                Debugger.debug(Debugger.TRACE, "Password=" + password);
            }
            if (st.hasMoreTokens()) {
                webSerStr = st.nextToken();
                Debugger.debug(Debugger.TRACE, "webserStr=\n" + webSerStr);
            }
            if (srcName == null || serverAddr == null || username == null || password == null || webSerStr == null) {
                throw new IllegalStateException("The lookup message on control channel" + " is corrupted");
            }
            SensorMetaData smd = SiteMetaDataParser.parse(srcName, webSerStr);
            SensorMetaDataManager.getInstance().insertMetaData(smd);
            TurbineServer server = actualSinkConfig.getTurbineServer(serverAddr, username, password);
            addSinkForSensor(smd, server);
            this.restartNeeded = true;
        }
    }

    public void addSinkForSensor(SensorMetaData smd, TurbineServer server) {
        String sourceName = smd.getId();
        if (!sensorsAddedForSink.containsKey(sourceName)) {
            boolean fbReqd = true;
            if (fbReqd) {
                Debugger.debug(Debugger.TRACE, "Enabling feedback for source " + sourceName);
                actualSinkConfig.enableFeedback(server, sourceName);
            }
            addSourceChannels(sourceName, server);
            sensorsAddedForSink.put(sourceName, smd);
        }
    }

    public void listenToControlChannels() {
        controlSinkConfig.printSinkData(Debugger.TRACE);
        Debugger.debug(Debugger.TRACE, "ControlChannel: Trying to connect...");
        controlSink = new DswSink(controlSinkConfig, false);
        controlSink.connectAndWait();
    }

    /**
   * The method searchs for the SensorMetaData for the given <tt>source</tt>
   * sensor and subscribes the source channels for the given ORB. For the 
   * DataTurbine orb, it subscribes in "Monitor" mode with "-1" timeout
   * (indicating infinite wait till the data arrives)
   * @param sourceName The name of the source (sensor ID)
   * @param server The server under which the sink is present
   */
    public void addSourceChannels(String sourceName, TurbineServer server) {
        SensorMetaData smd = SensorMetaDataManager.getInstance().getSensorMetaDataIfPresent(sourceName);
        if (smd == null) {
            Debugger.debug(Debugger.RECORD, "Requesting Meta data for \"" + sourceName + "\"");
            SiteMetaDataRequester mdr = new SiteMetaDataRequester(sourceName);
            smd = mdr.call();
        }
        Vector<String> completeChannelStrs = new Vector<String>();
        Vector<Integer> channelDatatypeVec = new Vector<Integer>();
        Vector<Integer> reqModeVec = new Vector<Integer>();
        Vector<Integer> intervalOrToutVec = new Vector<Integer>();
        Vector<String> actChannelsVec = smd.getChannels();
        for (int i = 0; i < actChannelsVec.size(); i++) {
            completeChannelStrs.addElement(sourceName + "/" + actChannelsVec.elementAt(i));
            reqModeVec.addElement(new Integer(TurbineSinkConfig.MONITOR_MODE));
            intervalOrToutVec.addElement(new Integer(Constants.DEFAULT_MONITOR_TIMEOUT));
        }
        server.resetSinkWrapperChannelVecs(completeChannelStrs, smd.getChannelDatatypes(), reqModeVec, intervalOrToutVec);
    }

    public void run() {
        Debugger.debug(Debugger.TRACE, "ActualSink Thread: Callback listener=" + actualSink.getCallBackListener());
        actualSink.connectAndWait();
    }

    public DswSink getActualSink() {
        return actualSink;
    }
}
