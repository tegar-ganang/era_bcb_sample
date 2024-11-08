package edu.sdsc.rtdsm.dig.dsw;

import java.util.*;
import java.text.DateFormat;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.data.*;

public class DswProcess implements SinkCallBackListener {

    private DswEnd2EndHelper helper;

    private String sourceNames[];

    public DswProcess(String sinkConfigFile, String sinkName, String srcConfigFile, String[] sourceNames) {
        helper = new DswEnd2EndHelper(sinkConfigFile, sinkName, srcConfigFile, sourceNames, this);
        this.sourceNames = sourceNames;
    }

    public void connectAndWait() {
        helper.connectAndWait();
    }

    public void callBack(DataPacket dataPkt) {
        System.out.println("DswProcess:Got some Data");
        for (int i = 0; i < dataPkt.getSize(); i++) {
            double[] data = (double[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                String time = DateFormat.getDateTimeInstance().format(new Date((long) (dataPkt.getTimestampAt(i, j))));
                System.out.println("Received|" + helper.sink.getName() + "|" + chanName + "|" + time + "|" + "|" + j + "|" + data[j]);
            }
            String convertedChanName = tempGetChannelName(chanName);
            helper.insertData(sourceNames[0], convertedChanName, (Object) data);
            String time = DateFormat.getDateTimeInstance().format(new Date());
            for (int j = 0; j < data.length; j++) {
                System.out.println("Sent|" + sourceNames[0] + "/" + convertedChanName + "|" + time + System.currentTimeMillis() + "|" + i + "|" + data[j]);
            }
        }
        int numChannelsFlushed = helper.flush(sourceNames[0]);
        String time = DateFormat.getDateTimeInstance().format(new Date());
        System.out.println("Flushed|" + sourceNames[0] + "|" + time + "|" + System.currentTimeMillis());
        System.out.println("-----------------------------------");
    }

    private String tempGetChannelName(String chanName) {
        int index = chanName.lastIndexOf("/");
        return chanName.substring(index + 1, chanName.length());
    }

    public static void main(String args[]) {
        if (args.length < 4) {
            System.err.println("Usage:  " + "<sinkConfig xml file>  <sink name> <srcConfig xml> <sourceNames ...>");
            return;
        }
        String sinkConfigFile = args[0];
        String sinkName = args[1];
        String srcConfigFile = args[2];
        String[] sourceNames = new String[args.length - 3];
        System.out.println("length:" + args.length);
        for (int i = 0; i < args.length - 3; i++) {
            sourceNames[i] = args[3 + i];
            System.out.println("SourceName:" + sourceNames[i]);
        }
        DswProcess process = new DswProcess(sinkConfigFile, sinkName, srcConfigFile, sourceNames);
        process.connectAndWait();
    }
}
