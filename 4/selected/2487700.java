package edu.sdsc.rtdsm.dataint;

import java.io.IOException;
import java.util.*;
import java.text.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import edu.sdsc.rtdsm.framework.sink.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.framework.db.*;
import edu.sdsc.rtdsm.dig.dsw.*;
import edu.sdsc.rtdsm.framework.data.*;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineSinkConfig;
import edu.sdsc.rtdsm.dig.sites.lake.*;
import edu.sdsc.rtdsm.dig.sites.*;

/**
 * The end client which acts as a sink. This class is responsible for
 * collecting the data values from the Object Ring Buffer and inserting 
 * the data to the database.
 * @author Vinay Kolar
 * @version 1.0
 **/
public class LakeSink implements SinkCallBackListener {

    /** A boolean indicator to check if the sink is connected to the ORB */
    boolean connected = false;

    /** A name of the sink */
    public String sinkName;

    /** A name of the xml configuration file */
    public String sinkConfigFile;

    /** Hashtable to store the association between the sensor name and the
   * respective sensor meta data */
    Hashtable<String, SensorMetaData> smdHash = new Hashtable<String, SensorMetaData>();

    /** A dummy variable to count the number of packets received since
   * the feedback has been sent. This is used for sinks which have enabled
   * feedback 
   **/
    private int numPkts = 0;

    /** A dummy variable to associate the number of packets received by a given
   *  sensor since the last feedback.
   * This is used for sinks which have enabled feedback 
   **/
    Hashtable<String, Integer> numPktsPerSrcHash = new Hashtable<String, Integer>();

    LakeSinkConfig lsc = null;

    /** The construtor which inputs the sink configuration xml file and the sink
   * name. This parses the xml file, associates the callback handler to the
   * method callback method of this object. 
   * @param sinkConfigFile The xml configuration file name. The xml format
   * should be as indicated in sinkConfig.dtd
   * @param sinkName The name of the sink to be started. The sinkName should 
   * be one of the sinks whose configuration is specified in the xml file. 
   */
    public LakeSink(String sinkConfigFile, String sinkName) {
        this.sinkConfigFile = sinkConfigFile;
        this.sinkName = sinkName;
        lsc = new LakeSinkConfig(sinkConfigFile, sinkName, this);
        lsc.parse();
    }

    /** 
   * This method should be invoked to connect the sink to the given orb. This
   * method does not return. Whenever data is available to the sink the
   * callback method of the instance is called and the sink waits for the next
   * data packet
   **/
    public void connect() {
        Debugger.debug(Debugger.TRACE, "Trying to connect...");
        if (lsc == null) {
            throw new IllegalStateException("The LakeSinkConfig object " + "has not yet been initialized for the Sink");
        }
        LakeSinkControlChannelListener controlListener = lsc.getControlChannelListener();
        if (controlListener == null) {
            throw new IllegalStateException("The control channel has not yet " + "been initialized for the Sink");
        }
        connected = true;
        lsc.getControlChannelListener().listenToControlChannels();
    }

    /**
   * This method will be called whenever a data is available for the sink.
   * This function may be called from multiple threads. So it is necessary
   * to keep this method synchronized for thread safety if global parameters
   * are accessed in the callback method or one of the functions which is
   * called directly or indirectly by callback. 
   * Note: Data packet does not have reference to other non-local variables 
   * which needs thread safety. However, we are making database calls. This 
   * should be syncronized. Things to be considered: 
   * (1) What happens if we change the sensor meta data(say from some other 
   * thread that updates meta data) while we are getting some data for that 
   * particular sensor? This needs synchronization. But this should not happen 
   * because, loading meta data, etc are done in the main thread and these 
   * threads are started after the sensor meta data have been updated. 
   * @param dataPkt The data packet that is of interest to the Lake Sink
   **/
    public synchronized void callBack(DataPacket dataPkt) {
        Debugger.debug(Debugger.TRACE, "LakeSink:Got some Data");
        for (int i = 0; i < dataPkt.getSize(); i++) {
            double[] data = (double[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                Date timestamp = new Date((long) (dataPkt.getTimestampAt(i, j)));
                String time = DateFormat.getDateTimeInstance().format(timestamp);
                Debugger.debug(Debugger.TRACE, "Received|" + getSink().getName() + "|" + chanName + "|" + time + "|" + "|" + j + "|" + data[j]);
            }
        }
        Debugger.debug(Debugger.TRACE, "-----------------------------------");
        updateIntoDb(dataPkt);
        sendFeedbackIfNecessary();
    }

    /**
   * This method checks if: 
   * (1) The sink has the option of sending feedback
   * (2) The sink has the feedback enabled for the particular source
   * Current version of the method sends the feedback to all the sources
   * that sent the message in the most recent call to the callback. This
   * can be modified to suit the sink logic of sending the feedback. The
   * current feedback message contains a string of the form
   * <tt>time@sinkName:FeedbackMsg</tt>
   * where <tt>time</tt> is the time at the sink when the packet was received 
   * (number of milliseconds since the epoch). <tt>sinkName</tt> is the name
   * of the this sink and <tt>FeedbackMsg</tt> is the number of data elements
   * received since the last feedback. <tt>FeedbackMsg</tt> currently consists
   * of the string <tt>PktsRecvd=[number of data elements]</tt>. The feedback
   * message is sent after the source has sent more than 10 data elements since 
   * its last feedback.
   **/
    public void sendFeedbackIfNecessary() {
        Debugger.debug(Debugger.TRACE, "FEEDBACK: enabled=" + getSink().isFeedbackEnabled());
        if (getSink().isFeedbackEnabled()) {
            Enumeration<String> keys = numPktsPerSrcHash.keys();
            while (keys.hasMoreElements()) {
                String src = keys.nextElement();
                Debugger.debug(Debugger.TRACE, "FEEDBACK: enabledForSrc(" + src + ")=" + getSink().isFeedbackEnabledForSource(src));
                if (getSink().isFeedbackEnabledForSource(src)) {
                    int value = numPktsPerSrcHash.get(src).intValue();
                    Debugger.debug(Debugger.TRACE, "FEEDBACK: " + src + " : NumPkts=" + value);
                    if (value >= 10) {
                        String numPktsStr = getSink().getName() + ":PktsRecvd=" + value;
                        getSink().sendFeedback(src, numPktsStr);
                        numPktsPerSrcHash.remove(src);
                    }
                }
            }
        }
    }

    /**
   * This is the utility method of aggregating the data that can
   * be added to the Lake database in an efficient fashion. The primary key of
   * the database is the timestamp. Hence all the sensor values from a given
   * sensor are collected and ordered according to the timestamp and a single
   * insert call is made to the database. Since each data packet categorizes
   * the data from different channels like timestamp and the other channel values,
   * multiple insert/update calls may be needed if the data packet is not ordered 
   * in the above way
   * @param dataPkt The data packet received at the callback method
   * @return A hastable of the source specific data items received
   */
    public Hashtable<String, SourceData> convertDataPktToTimeStampedTree(DataPacket dataPkt) {
        Hashtable<String, SourceData> srcHash = new Hashtable<String, SourceData>();
        Hashtable<String, Integer> recentNumPkts = new Hashtable<String, Integer>();
        for (int i = 0; i < dataPkt.getSize(); i++) {
            Object data = dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            int index = chanName.indexOf("/");
            String source = chanName.substring(0, index);
            String channel = chanName.substring(index + 1, chanName.length());
            int numPkts = 0;
            if (recentNumPkts.containsKey(source)) {
                numPkts = recentNumPkts.get(source).intValue() + 1;
                recentNumPkts.put(source, new Integer(numPkts));
            } else if (numPktsPerSrcHash.containsKey(source)) {
                numPkts = numPktsPerSrcHash.get(source).intValue() + 1;
                recentNumPkts.put(source, new Integer(numPkts));
            } else {
                recentNumPkts.put(source, new Integer(1));
            }
            SensorMetaData smd = SensorMetaDataManager.getInstance().getSensorMetaDataIfPresent(source);
            if (smd == null) {
                throw new IllegalArgumentException("No meta about the sensor \"" + source + "\" is registered");
            }
            int datatype = smd.getChannelDatatype(channel);
            if (!srcHash.containsKey(source)) {
                srcHash.put(source, new SourceData(source));
            }
            Debugger.debug(Debugger.TRACE, "Source[" + i + "]= " + source);
            Debugger.debug(Debugger.TRACE, "Channel[" + i + "]= " + channel);
            SourceData sd = srcHash.get(source);
            TimeStampedData tsData = null;
            switch(datatype) {
                case Constants.DATATYPE_DOUBLE:
                    double[] dataArr = (double[]) dataPkt.getDataAt(i);
                    Debugger.debug(Debugger.TRACE, "Data at " + i + " = " + dataArr + ": " + dataArr[0]);
                    for (int k = 0; k < dataArr.length; k++) {
                        tsData = sd.getTimeStampedData((long) (dataPkt.getTimestampAt(i, k)));
                        ChannelData chData = tsData.getChannelData(channel);
                        chData.addData(new Double(dataArr[k]));
                        Debugger.debug(Debugger.TRACE, "Adding " + dataArr[k] + " to " + channel + " (" + chData + ")");
                    }
                    break;
                default:
                    throw new IllegalStateException("Datatype of the data " + "unsupported for source/channel \"" + chanName);
            }
        }
        updateFeedbackInfo(recentNumPkts);
        return srcHash;
    }

    /**
   * A utility method to update the received data statistics
   * @param recentNumPkts The number of packets got in the recent call
   * to the callback method.
   **/
    public void updateFeedbackInfo(Hashtable<String, Integer> recentNumPkts) {
        Enumeration<String> keys = recentNumPkts.keys();
        while (keys.hasMoreElements()) {
            String src = keys.nextElement();
            numPktsPerSrcHash.put(src, recentNumPkts.get(src));
        }
    }

    /**
   * This method converts the time in number of milli seconds since epoch
   * to the String form of date that can be used for PostGres database. 
   * This is done by a call to the database. The query is 
   * <tt>select timestamp 'epoch' + interval ' [numSeconds] seconds</tt>
   * NOTE: The resolution of the time is in seconds
   * @param timeSinceEpoch the number of milliseconds since epoch
   * @return The string version of the time (resolution of seconds) that
   * can be used for inserting timestamp into the postgres database
   */
    private String getTimeStringForPostGresDb(double timeSinceEpoch) {
        double seconds = ((double) timeSinceEpoch) / 1000.00;
        DecimalFormat df = new DecimalFormat("#.###");
        String secStr = df.format(seconds);
        String timeSql = "select timestamp 'epoch' + interval '" + secStr + " seconds'";
        Debugger.debug(Debugger.TRACE, "TimeSinceEpoch = " + timeSinceEpoch);
        Debugger.debug(Debugger.TRACE, "SecStr = " + secStr);
        Debugger.debug(Debugger.TRACE, "Time sql = " + timeSql);
        Connection conn = ConnectionManager.getInstance().getConnection();
        Statement stmt;
        ResultSet rs;
        String timeStr = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(timeSql);
            if (rs.next()) {
                timeStr = rs.getString(1);
                Debugger.debug(Debugger.TRACE, "Time string = " + timeStr);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new IllegalStateException("SQL Error");
        }
        return timeStr;
    }

    /**
   * This method converts the time in number of milli seconds since epoch
   * to the String form of date that can be used for MySQL database. 
   * The timestring is of the form <tt>YYYY-MM-DD HH:MM:SEC</tt>
   * The maximum resolution of the time is in the order of seconds
   * @param timeSinceEpoch The milliseconds that has expired after epoch
   * @return The string format of the time
   */
    private String getTimeStringForMySqlDb(long timeSinceEpoch) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeSinceEpoch);
        StringBuffer sb = new StringBuffer("");
        sb = sb.append("" + cal.get(Calendar.YEAR));
        sb = sb.append("-");
        sb = sb.append("" + (cal.get(Calendar.MONTH) + 1));
        sb = sb.append("-");
        sb = sb.append("" + cal.get(Calendar.DAY_OF_MONTH));
        sb = sb.append(" ");
        sb = sb.append("" + cal.get(Calendar.HOUR_OF_DAY));
        sb = sb.append(":");
        sb = sb.append("" + cal.get(Calendar.MINUTE));
        sb = sb.append(":");
        sb = sb.append("" + cal.get(Calendar.SECOND));
        Debugger.debug(Debugger.TRACE, "Time string = " + sb);
        return sb.toString();
    }

    /**
   * This method intakes the datapacket from the callback method, structures
   * the data based on the the source(sensor) and updates the database
   * The database configuration is present in the file 
   * <tt>rtdsm.properties</tt>. The key values in the file for database are
   * <tt>dbDriver, dbJdbcUrl, dbUsername</tt> and <tt>dbPassword</tt>
   * The table name and structure corresponds the tablename as specified in 
   * the Sensor meta data for the sensor. The restriction is that all the
   * sensing values from a sensor (e.g: dissoved oxygen, temperature, etc) are
   * present as induvidual columns and timestamp is the primary key of the
   * table.
   * @param dataPkt Data packet that needs to be inserted
   **/
    public void updateIntoDb(DataPacket dataPkt) {
        Hashtable<String, SourceData> srcHash = convertDataPktToTimeStampedTree(dataPkt);
        Debugger.debug(Debugger.TRACE, "===========================================");
        Enumeration<String> enumSrcData = srcHash.keys();
        while (enumSrcData.hasMoreElements()) {
            String source = enumSrcData.nextElement();
            Debugger.debug(Debugger.TRACE, "Source=" + source);
            SensorMetaData smd = SensorMetaDataManager.getInstance().getSensorMetaData(source);
            if (smd == null) {
                throw new IllegalArgumentException("No meta about the sensor \"" + source + "\" is registered");
            }
            SourceData sd = srcHash.get(source);
            Enumeration<Long> enumTsData = sd.getTimeStamps();
            while (enumTsData.hasMoreElements()) {
                Long tsKey = enumTsData.nextElement();
                Debugger.debug(Debugger.TRACE, "\tTime=" + new Date(tsKey));
                TimeStampedData tsd = sd.getTimeStampedData(tsKey);
                String turbineTimeStr = getTimeStringForMySqlDb(tsKey.longValue());
                StringBuffer fieldNamesStr = new StringBuffer("(");
                StringBuffer valueStr = new StringBuffer("(");
                Enumeration<String> enumChannel = tsd.getChannels();
                while (enumChannel.hasMoreElements()) {
                    String channel = enumChannel.nextElement();
                    Debugger.debug(Debugger.TRACE, "\t\tChannel=" + channel);
                    if (Constants.TIMESTAMP_CHANNEL_NAME.equals(channel)) {
                        fieldNamesStr.append(Constants.LAKE_SENSORS_DB_TIME_FIELD_NAME);
                    } else {
                        fieldNamesStr = fieldNamesStr.append(channel);
                    }
                    fieldNamesStr = fieldNamesStr.append(",");
                    ChannelData cd = tsd.getChannelData(channel);
                    if (cd.getNumDataItems() > 1) {
                        throw new IllegalStateException("ERROR: Getting more than one " + "data values " + "for the same channel \"" + channel + "\" from source \"" + source + "\" at turbine time " + turbineTimeStr + ".");
                    }
                    for (int i = 0; i < cd.getNumDataItems(); i++) {
                        Debugger.debug(Debugger.TRACE, "\t\t\tDATA [ " + i + " ] = " + cd.getData(i));
                    }
                    if (Constants.TIMESTAMP_CHANNEL_NAME.equals(channel)) {
                        Double d = (Double) cd.getData(0);
                        long l = (long) d.doubleValue();
                        String timeStr = getTimeStringForMySqlDb(l);
                        valueStr = valueStr.append("'");
                        valueStr = valueStr.append(timeStr);
                        valueStr = valueStr.append("'");
                    } else {
                        valueStr = valueStr.append(cd.getData(0));
                    }
                    valueStr = valueStr.append(",");
                }
                valueStr = valueStr.delete(valueStr.length() - 1, valueStr.length());
                fieldNamesStr = fieldNamesStr.delete(fieldNamesStr.length() - 1, fieldNamesStr.length());
                fieldNamesStr = fieldNamesStr.append(")");
                valueStr = valueStr.append(")");
                Debugger.debug(Debugger.TRACE, "fieldNamesStr=" + fieldNamesStr);
                Debugger.debug(Debugger.TRACE, "valueStr=" + valueStr);
                insertIntoDb(smd, fieldNamesStr, valueStr);
            }
        }
        Debugger.debug(Debugger.TRACE, "===========================================");
    }

    /** The insertion routine. This is called from updateDb method
   * @param smd The sensor meta data for which the data needs to be inserted
   * @param fieldNamesStr a list of field names separated by comma
   * @param valueStr the list of values separated by comma
   **/
    private void insertIntoDb(SensorMetaData smd, StringBuffer fieldNamesStr, StringBuffer valueStr) {
        StringBuffer sql = new StringBuffer("INSERT INTO ");
        sql.append(smd.getTableName());
        sql.append(" ");
        sql.append(fieldNamesStr);
        sql.append(" VALUES ");
        sql.append(valueStr);
        Debugger.debug(Debugger.TRACE, "SQL=" + sql);
        Connection conn = ConnectionManager.getInstance().getConnection();
        Statement stmt;
        ResultSet rs;
        String timeStr = null;
        try {
            stmt = conn.createStatement();
            int rowsModified = stmt.executeUpdate(sql.toString());
            Debugger.debug(Debugger.TRACE, "Rows modified=" + rowsModified);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new IllegalStateException("SQL Error: SQL=" + sql.toString());
        }
    }

    /**
   * The main method. This creates an instance of LakeSink and waits for the
   * data from the ORB. On the data arrival event, the callback() method of
   * the created instance is called.
   * @param args args[0] corresponds to the xml configuration file which
   * descirbes the sink(s). args[1] is the name of the sink that needs to be
   * created.
   */
    public static void main(String args[]) {
        if (args.length != 2) {
            System.err.println("Usage: java stubs.LakeSink " + "<sinkConfig xml file> <sink name>");
            return;
        }
        String sinkConfigFile = args[0];
        String sinkName = args[1];
        LakeSink lakeSink = new LakeSink(sinkConfigFile, sinkName);
        lakeSink.connect();
    }

    public DswSink getSink() {
        return lsc.getControlChannelListener().getActualSink();
    }
}
