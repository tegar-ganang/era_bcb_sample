package edu.sdsc.rtdsm.dig.sites.lake;

import java.util.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.dig.sites.*;

public class SensorMetaData {

    String id = null;

    String buoyId = null;

    Vector<String> dataIdVec = new Vector<String>();

    Vector<Integer> dataIdDatatypeVec = new Vector<Integer>();

    String lakeId = null;

    String loggerId = null;

    double sampleRate = -1.0;

    String sensorType = null;

    String tableName = null;

    String webserviceString = null;

    SiteSource source = null;

    SrcConfig srcConfig = null;

    public SensorMetaData(String id) {
        this.id = id;
        init();
    }

    public void init() {
        addDataId(Constants.TIMESTAMP_CHANNEL_NAME);
    }

    public void setProperty(String key, String value) {
        if (Constants.SITE_META_DATA_SERVICE_BUOY_ID.equals(key)) {
            buoyId = value;
        } else if (key.startsWith(Constants.SITE_META_DATA_SERVICE_DATA_ID)) {
            addDataId(value);
        } else if (Constants.SITE_META_DATA_SERVICE_LAKE_ID.equals(key)) {
            lakeId = value;
        } else if (Constants.SITE_META_DATA_SERVICE_LOGGER_ID.equals(key)) {
            loggerId = value;
        } else if (Constants.SITE_META_DATA_SERVICE_SAMPLE_RATE.equals(key)) {
            sampleRate = Double.parseDouble(value);
        } else if (Constants.SITE_META_DATA_SERVICE_SENSOR_TYPE.equals(key)) {
            sensorType = value;
        } else if (Constants.SITE_META_DATA_SERVICE_TABLE_NAME.equals(key)) {
            tableName = value;
        } else {
            throw new IllegalArgumentException("The key \"" + key + "\" is not " + "recognized as a parameter for the sensor in the meta data " + "information");
        }
    }

    public void resetInfo(boolean runningInstanceToo) {
        dataIdVec = new Vector<String>();
        dataIdDatatypeVec = new Vector<Integer>();
        init();
        webserviceString = null;
        if (runningInstanceToo) {
            if (source != null) {
                source.disconnect();
                srcConfig = null;
            }
        }
    }

    public void setWebServiceString(String str) {
        webserviceString = str;
    }

    public String getWebServiceString() {
        return webserviceString;
    }

    public void addDataId(String value) {
        dataIdVec.addElement(value);
        dataIdDatatypeVec.addElement(Constants.DATATYPE_DOUBLE_OBJ);
    }

    private void parseAndSetDataIds(String ids) {
        StringTokenizer st = new StringTokenizer(ids, Constants.SITE_META_DATA_SERVICE_DATA_IDS_DELIM);
        String dataId;
        while (st.hasMoreTokens()) {
            dataId = st.nextToken();
            dataIdVec.addElement(dataId);
            dataIdDatatypeVec.addElement(Constants.DATATYPE_DOUBLE_OBJ);
        }
    }

    public void printMetaData(int level) {
        Debugger.debug(level, "Id : " + id);
        Debugger.debug(level, "Buoy id : " + buoyId);
        Debugger.debug(level, "Lake id : " + lakeId);
        Debugger.debug(level, "Logger id : " + loggerId);
        Debugger.debug(level, "Sampling rate : " + sampleRate);
        Debugger.debug(level, "Sensor Type: " + sensorType);
        Debugger.debug(level, "Table name: " + tableName);
        Debugger.debug(level, "Data channels:type:");
        for (int i = 0; i < dataIdVec.size(); i++) {
            Debugger.debug(level, "\t " + dataIdVec.elementAt(i) + ":" + dataIdDatatypeVec.elementAt(i));
        }
    }

    public String getId() {
        return id;
    }

    public int getNumChannels() {
        return dataIdVec.size();
    }

    public Vector<String> getChannels() {
        return dataIdVec;
    }

    public Vector<Integer> getChannelDatatypes() {
        return dataIdDatatypeVec;
    }

    public void setSrcConfig(SrcConfig srcConfig) {
        this.srcConfig = srcConfig;
    }

    public void setSource(SiteSource src) {
        this.source = src;
    }

    public SiteSource getSource() {
        return source;
    }

    public int getChannelDatatype(String chName) {
        Debugger.debug(Debugger.TRACE, "============chanName=" + chName);
        for (int i = 0; i < dataIdVec.size(); i++) {
            Debugger.debug(Debugger.TRACE, "chanName=" + chName + " SmdName=" + dataIdVec.elementAt(i));
            if (dataIdVec.elementAt(i).equals(chName)) {
                return dataIdDatatypeVec.elementAt(i).intValue();
            }
        }
        return -1;
    }

    public String getTableName() {
        return tableName;
    }
}
