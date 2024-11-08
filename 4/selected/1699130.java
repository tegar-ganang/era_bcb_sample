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
import edu.sdsc.rtdsm.framework.data.DataPacket;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineSinkConfig;

public class DataIntSink implements SinkCallBackListener {

    boolean connected = false;

    public String sinkName;

    public String sinkConfigFile;

    public String templateName;

    public String templateConfigFile;

    DswSink sink;

    SinkConfig sinkConfig;

    DbTemplateConfig templateConfig;

    public DataIntSink(String sinkConfigFile, String sinkName, String dbTemplateFile, String templateName) {
        this.sinkConfigFile = sinkConfigFile;
        this.sinkName = sinkName;
        this.templateConfigFile = dbTemplateFile;
        this.templateName = templateName;
        SinkConfigParser parser = new SinkConfigParser();
        parser.fileName = sinkConfigFile;
        parser.parse();
        sinkConfig = parser.getSinkConfig(sinkName);
        ((TurbineSinkConfig) sinkConfig).callbackHandler = this;
        DbTemplateConfigParser dbParser = new DbTemplateConfigParser();
        dbParser.fileName = templateConfigFile;
        dbParser.parse();
        templateConfig = dbParser.getTemplateConfig(templateName);
        Debugger.debug(Debugger.TRACE, "Template Details");
        Debugger.debug(Debugger.TRACE, "------------------------");
        Debugger.debug(Debugger.TRACE, "Template Name: " + templateConfig.templateName);
        Vector<String> reqPathNameVec = templateConfig.getReqPathNames();
        for (int i = 0; i < reqPathNameVec.size(); i++) {
            String name = reqPathNameVec.elementAt(i);
            Debugger.debug(Debugger.TRACE, "\tReqPath Name: " + name);
            ReqPathConfig config = templateConfig.getReqPathConfig(name);
            Vector<String> dbTables = config.getDbTableNames();
            for (int j = 0; j < dbTables.size(); j++) {
                Debugger.debug(Debugger.TRACE, "\t\tDbTable Name: " + dbTables.elementAt(j));
            }
            Vector<String> mapKeys = config.getMapKeys();
            for (int j = 0; j < mapKeys.size(); j++) {
                Debugger.debug(Debugger.TRACE, "\t\tMap:" + mapKeys.elementAt(j) + ":" + config.getMapValue(mapKeys.elementAt(j)));
            }
        }
    }

    public void connect() {
        System.out.println("Trying to connect...");
        sink = new DswSink(sinkConfig);
        connected = true;
        sink.connectAndWait();
    }

    public void callBack(DataPacket dataPkt) {
        System.out.println("DataIntSink:Got some Data");
        for (int i = 0; i < dataPkt.getSize(); i++) {
            double[] data = (double[]) dataPkt.getDataAt(i);
            String chanName = dataPkt.getChannelNameAt(i);
            for (int j = 0; j < data.length; j++) {
                Date timestamp = new Date((long) (dataPkt.getTimestampAt(i, j)));
                String time = DateFormat.getDateTimeInstance().format(timestamp);
                System.out.println("Received|" + sink.getName() + "|" + chanName + "|" + time + "|" + "|" + j + "|" + data[j]);
                updateIntoDb(chanName, (Object) new Double(data[j]), timestamp.getTime());
            }
        }
        System.out.println("-----------------------------------");
    }

    public void updateIntoDb(String chanName, Object data, long timeSinceEpoch) {
        ReqPathConfig config = templateConfig.getReqPathConfig(chanName);
        if (config == null) {
            throw new IllegalStateException("Got the data from reqPath \"" + chanName + "\" for which the sink is not subscribed");
        }
        Vector<String> dbNamesVec = config.getDbTableNames();
        if (dbNamesVec.size() <= 0) {
            return;
        }
        int index = chanName.indexOf("/");
        String source = chanName.substring(0, index);
        String channel = chanName.substring(index + 1, chanName.length());
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
        String dataStr = null;
        switch(config.datatype) {
            case Constants.DATATYPE_DOUBLE:
                dataStr = data.toString();
                break;
            default:
                throw new IllegalStateException("Only double datatype is supported. " + "More options will be supported soon");
        }
        for (int i = 0; i < dbNamesVec.size(); i++) {
            String dbName = dbNamesVec.elementAt(i);
            String sql = "INSERT INTO " + dbName + " (" + config.getMapValue(Constants.DB_TEMPLATE_SRC_TAG) + "," + config.getMapValue(Constants.DB_TEMPLATE_CHANNEL_TAG) + "," + config.getMapValue(Constants.DB_TEMPLATE_GEN_TIME_TAG) + "," + config.getMapValue(Constants.DB_TEMPLATE_VALUE_TAG) + ") " + "VALUES ('" + source + "','" + channel + "','" + timeStr + "'," + dataStr + ")";
            Debugger.debug(Debugger.TRACE, "Updating at db: " + dbName);
            Debugger.debug(Debugger.TRACE, "Sql = " + sql);
            try {
                stmt.executeUpdate(sql);
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new IllegalStateException("SQL Error");
            }
        }
    }

    public static void main(String args[]) {
        if (args.length != 4) {
            System.err.println("Usage: java stubs.DataIntSink " + "<sinkConfig xml file> <sink name> <dbTemplate xml file> <template name>");
            return;
        }
        String sinkConfigFile = args[0];
        String sinkName = args[1];
        String dbConfigFile = args[2];
        String templateName = args[3];
        DataIntSink dswSink = new DataIntSink(sinkConfigFile, sinkName, dbConfigFile, templateName);
        dswSink.connect();
    }
}
