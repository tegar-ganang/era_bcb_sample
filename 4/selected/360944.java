package edu.sdsc.rtdsm.drivers.turbine;

import com.rbnb.sapi.*;
import java.util.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.util.*;

/**
 * Class TurbineSource
 * 
 */
public class TurbineSource extends TurbineClient implements DataSource {

    Source src;

    public Vector<Integer> srcChannelIndicies;

    public Vector<String> srcChannelNames;

    Vector<Integer> srcChannelDataTypes;

    TurbineSrcConfig srcConfig;

    int bufferingType;

    boolean connected = false;

    public TurbineSource(TurbineSrcConfig srcConfig) {
        super(srcConfig.getServer(), srcConfig.getName());
        initSrc(srcConfig);
    }

    public void initSrc(SrcConfig srcConfigVal) {
        if (!(srcConfigVal instanceof TurbineSrcConfig)) {
            throw new IllegalArgumentException("The config parameter has to be " + "an instance of TurbineSrcConfig");
        }
        this.srcConfig = (TurbineSrcConfig) srcConfigVal;
        this.srcChannelNames = srcConfig.getChannelNames();
        this.srcChannelDataTypes = srcConfig.getChannelDataTypes();
        src = new Source(srcConfig.cacheSize, srcConfig.archiveMode, srcConfig.archiveSize);
        setClient(src);
        this.srcChannelIndicies = addChannels(srcChannelNames);
    }

    public void insertData(String chName, Object data) {
        int chIndex = getChannelIndex(chName);
        insertData(chIndex, data);
    }

    public void insertData(int channel, Object data) {
        try {
            Debugger.debug(Debugger.RECORD, "Call to insert data:" + srcConfig.getName() + ":" + channel);
            switch(((Integer) srcChannelDataTypes.elementAt(channel)).intValue()) {
                case Constants.DATATYPE_DOUBLE:
                    double[] dataDouble = (double[]) data;
                    map.PutDataAsFloat64(srcChannelIndicies.elementAt(channel).intValue(), dataDouble);
                    Debugger.debug(Debugger.RECORD, src.GetClientName() + ": Inserted data at chnl(" + ((Integer) srcChannelIndicies.elementAt(channel)) + " " + srcChannelNames.elementAt(channel) + ")=");
                    for (int i = 0; i < dataDouble.length; i++) {
                        Debugger.debug(Debugger.RECORD, "\t" + i + "=" + dataDouble[i]);
                    }
                    break;
                case Constants.DATATYPE_STRING:
                    String dataString = (String) data;
                    map.PutDataAsString(srcChannelIndicies.elementAt(channel).intValue(), dataString);
                    Debugger.debug(Debugger.RECORD, src.GetClientName() + ": Inserted data at chnl(" + ((Integer) srcChannelIndicies.elementAt(channel)) + " " + srcChannelNames.elementAt(channel) + ")=" + dataString);
                    break;
                default:
                    throw new UnsupportedOperationException("The datatype specified " + "is not supported");
            }
        } catch (SAPIException se) {
            se.printStackTrace();
        }
    }

    public int flush() {
        try {
            int numChannelsFlushed = src.Flush(map);
            Debugger.debug(Debugger.RECORD, "Flushed now:" + numChannelsFlushed);
            map.PutTimeAuto("timeofday");
            return numChannelsFlushed;
        } catch (SAPIException se) {
            se.printStackTrace();
        }
        return -1;
    }

    public TurbineServer getServer() {
        return srcConfig.getServer();
    }

    public String getServerName() {
        return srcConfig.getServer().getServerAddr();
    }

    public String getUsername() {
        return srcConfig.getServer().getUsername();
    }

    public String getPassword() {
        return srcConfig.getServer().getPassword();
    }
}
