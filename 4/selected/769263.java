package edu.sdsc.rtdsm.dig.dsw;

import java.util.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.drivers.turbine.*;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.src.*;

/** 
 * Temporary class for transforming sink input to source output
 **/
public class DswEnd2EndHelper {

    boolean connected = false;

    public String sinkName;

    public String sinkConfigFile;

    DswSink sink;

    SinkConfig sinkConfig;

    int cacheSize = 0;

    String[] sourceNames = null;

    String tmpStr;

    Vector<DswSource> sourceVec = new Vector<DswSource>();

    Vector<Vector<Integer>> chnlIndexVec = new Vector<Vector<Integer>>();

    Vector<Vector<String>> chnlNamesVec = new Vector<Vector<String>>();

    String srcConfigFile;

    Vector<SrcConfig> srcConfigVec = new Vector<SrcConfig>();

    Hashtable sourceNameHash = new Hashtable<String, Integer>();

    public DswEnd2EndHelper(String sinkConfigFile, String sinkName, String srcConfigFile, String[] sourceNames, SinkCallBackListener listener) {
        this.sinkConfigFile = sinkConfigFile;
        this.sinkName = sinkName;
        SinkConfigParser sinkParser = new SinkConfigParser();
        sinkParser.fileName = sinkConfigFile;
        sinkParser.parse();
        sinkConfig = sinkParser.getSinkConfig(sinkName);
        ((TurbineSinkConfig) sinkConfig).callbackHandler = listener;
        this.srcConfigFile = srcConfigFile;
        SrcConfigParser srcParser = new SrcConfigParser();
        srcParser.fileName = srcConfigFile;
        srcParser.parse();
        this.sourceNames = sourceNames;
        for (int i = 0; i < sourceNames.length; i++) {
            srcConfigVec.addElement(srcParser.getSourceConfig(sourceNames[i]));
            sourceNameHash.put(sourceNames[i], new Integer(i));
        }
    }

    public void connectSinkAndWait() {
        System.out.println("Trying to connect...");
        sink = new DswSink(sinkConfig);
        connected = true;
        sink.connectAndWait();
    }

    public void connectSources() {
        for (int i = 0; i < srcConfigVec.size(); i++) {
            DswSource source = new DswSource(srcConfigVec.elementAt(i));
            sourceVec.addElement(source);
            boolean connected = source.connect();
            chnlIndexVec.addElement(source.getChannelIndicies());
            chnlNamesVec.addElement(source.getChannelNames());
        }
    }

    public void connectAndWait() {
        connectSources();
        connectSinkAndWait();
    }

    public void insertData(String sourceName, String channelName, Object data) {
        int srcIndex = getSourceIndex(sourceName);
        sourceVec.elementAt(srcIndex).insertData(channelName, data);
    }

    public int flush(String sourceName) {
        int srcIndex = getSourceIndex(sourceName);
        return sourceVec.elementAt(srcIndex).flush();
    }

    private int getSourceIndex(String sourceName) {
        Integer srcInt = (Integer) sourceNameHash.get(sourceName);
        if (srcInt == null) {
            throw new IllegalArgumentException("Source by name \"" + sourceName + "\" does not exist.");
        }
        return srcInt.intValue();
    }
}
