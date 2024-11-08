package edu.sdsc.rtdsm.stubs;

import java.io.IOException;
import java.util.*;
import java.text.DateFormat;
import edu.sdsc.rtdsm.dig.dsw.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.data.DataPacket;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineSinkConfig;

public class DswSinkStubPull implements SinkCallBackListener {

    boolean connected = false;

    public String sinkName;

    public String configFile;

    DswSink sink;

    SinkConfig sinkConfig;

    public DswSinkStubPull(String sinkConfigFile, String sinkName) {
        this.configFile = sinkConfigFile;
        this.sinkName = sinkName;
        SinkConfigParser parser = new SinkConfigParser();
        parser.fileName = sinkConfigFile;
        parser.parse();
        sinkConfig = parser.getSinkConfig(sinkName);
        ((TurbineSinkConfig) sinkConfig).callbackHandler = this;
    }

    public void connect() {
        System.out.println("Trying to connect...");
        sink = new DswSink(sinkConfig);
        connected = true;
        sink.connectAndWait();
    }

    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("Usage: java stubs.DswSinkStubPull " + "<sinkConfig xml file> <sink name>");
            return;
        }
        String configFile = args[0];
        String sinkName = args[1];
        DswSinkStubPull dswSink = new DswSinkStubPull(configFile, sinkName);
        dswSink.connect();
    }

    public void callBack(DataPacket dataPkt) {
        System.out.println("Stub:Got some Data");
        for (int i = 0; i < dataPkt.getSize(); i++) {
            double[] data = (double[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                String time = DateFormat.getDateTimeInstance().format(new Date((long) (dataPkt.getTimestampAt(i, j))));
                System.out.println("Received|" + sink.getName() + "|" + chanName + "|" + time + "|" + j + "|" + data[j]);
            }
        }
        System.out.println("-----------------------------------");
    }
}
