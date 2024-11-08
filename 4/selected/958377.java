package edu.sdsc.rtdsm.dig.dsw;

import java.util.*;
import edu.sdsc.rtdsm.drivers.turbine.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.framework.src.*;

public class DswSource {

    TurbineManager mgr = TurbineManager.getInstance();

    TurbineSource src;

    TurbineSink feedbackSink;

    public DswSource(SrcConfig srcConfig) {
        if (srcConfig instanceof TurbineSrcConfig) {
            src = new TurbineSource((TurbineSrcConfig) srcConfig);
        } else {
            throw new IllegalStateException("DswSources currently support only data turbine");
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
        return src.connect();
    }

    public void insertData(int channel, Object data) {
        src.insertData(channel, data);
    }

    public void insertData(String chName, Object data) {
        src.insertData(chName, data);
    }

    public int flush() {
        return src.flush();
    }

    /**
   * 
   * @return void  
   */
    public void processFeedback() {
    }
}
