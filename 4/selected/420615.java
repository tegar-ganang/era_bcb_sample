package edu.sdsc.rtdsm.stubs;

import java.io.IOException;
import java.util.*;
import java.text.DateFormat;
import edu.sdsc.rtdsm.dig.dsw.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.util.Debugger;
import edu.sdsc.rtdsm.framework.data.DataPacket;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineSinkConfig;

public class DswSinkStub implements SinkCallBackListener {

    boolean connected = false;

    public String sinkName;

    public String configFile;

    DswSink sink;

    SinkConfig sinkConfig;

    boolean feedbackReqd = true;

    public DswSinkStub(String sinkConfigFile, String sinkName) {
        this.configFile = sinkConfigFile;
        this.sinkName = sinkName;
        SinkConfigParser parser = new SinkConfigParser();
        parser.fileName = sinkConfigFile;
        parser.parse();
        sinkConfig = parser.getSinkConfig(sinkName);
        ((TurbineSinkConfig) sinkConfig).callbackHandler = this;
    }

    public void connect() {
        Debugger.debug(Debugger.TRACE, "Trying to connect...");
        sink = new DswSink(sinkConfig, feedbackReqd);
        connected = true;
        sink.connectAndWait();
    }

    public void callBack(DataPacket dataPkt) {
        System.out.println("Stub:Got some Data");
        String source = null;
        for (int i = 0; i < dataPkt.getSize(); i++) {
            double[] data = (double[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                String time = DateFormat.getDateTimeInstance().format(new Date((long) (dataPkt.getTimestampAt(i, j))));
                System.out.println("Received|" + sink.getName() + "|" + chanName + "|" + time + "|" + "|" + j + "|" + data[j]);
                if (source == null) {
                    int index = chanName.indexOf('/');
                    if (index != -1) {
                        source = chanName.substring(0, index);
                    }
                }
            }
        }
        if (feedbackReqd) {
            sink.sendFeedback(source, "ghijkl");
        }
        System.out.println("-----------------------------------");
    }

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("Usage: java stubs.DswSinkStub " + "<sinkConfig xml file> <sink name>");
            return;
        }
        String configFile = args[0];
        String sinkName = args[1];
        DswSinkStub dswSink = new DswSinkStub(configFile, sinkName);
        dswSink.connect();
    }
}
